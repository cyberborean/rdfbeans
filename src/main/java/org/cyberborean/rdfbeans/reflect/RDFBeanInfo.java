
package org.cyberborean.rdfbeans.reflect;

import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.cyberborean.rdfbeans.annotations.RDF;
import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.annotations.RDFContainer;
import org.cyberborean.rdfbeans.annotations.RDFNamespaces;
import org.cyberborean.rdfbeans.annotations.RDFSubject;
import org.cyberborean.rdfbeans.exceptions.RDFBeanValidationException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class RDFBeanInfo {
	
	private static WeakHashMap<Class, RDFBeanInfo> rdfBeanInfoCache = new WeakHashMap<Class, RDFBeanInfo>();
	
	public static synchronized RDFBeanInfo get(Class rdfBeanClass)
			throws RDFBeanValidationException {
		RDFBeanInfo rbi = rdfBeanInfoCache.get(rdfBeanClass);
		if (rbi == null) {
			try {
				rbi = new RDFBeanInfo(rdfBeanClass);
				rdfBeanInfoCache.put(rdfBeanClass, rbi);
			} catch (IntrospectionException e) {
				throw new RDFBeanValidationException(rdfBeanClass, e);
			}
		}
		return rbi;
	}

	public static boolean isRdfBean(Object object) {
		return isRdfBeanClass(object.getClass());
	}

	public static boolean isRdfBeanClass(Class cls) {
		return ReflectionUtil.getClassAnnotation(cls, RDFBean.class) != null;
	}

	private Class rdfBeanClass;
	private BeanInfo beanInfo;
	private SubjectProperty subjectProperty = null;
	private Map<IRI, RDFProperty> properties = new HashMap<>();
	private Map<Method, RDFProperty> propertiesByGetter = new HashMap<Method, RDFProperty>();	
	private Map<Method, RDFProperty> propertiesBySetter = new HashMap<Method, RDFProperty>();
	private Map<String, String> namespaces = new HashMap<String, String>();
	private IRI rdfType;

	private RDFBeanInfo(Class rdfBeanClass) throws RDFBeanValidationException,
			IntrospectionException {
		this.rdfBeanClass = rdfBeanClass;
		beanInfo = Introspector.getBeanInfo(rdfBeanClass);
		if (rdfBeanClass.isInterface()) {
			beanInfo = new InterfaceBeanInfo(rdfBeanClass, beanInfo);
		}
		introspect();
	}

	private void introspect() throws RDFBeanValidationException {
		initNamespaces();
		initBeanType();
		for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
			checkSubjectProperty(pd);
			RDF annotation = checkAnnotation(pd, RDF.class);
			if (annotation != null) {
				RDFContainer container = checkAnnotation(pd, RDFContainer.class);
				RDFProperty p = new RDFProperty(pd, this, annotation, container);
				Method getter = pd.getReadMethod();
				if (getter != null) {
					propertiesByGetter.put(getter, p);
				}
				Method setter = pd.getWriteMethod();
				if (setter != null) {
					propertiesBySetter.put(setter, p);
				}
				if (pd instanceof IndexedPropertyDescriptor) {
					IndexedPropertyDescriptor ipd = ((IndexedPropertyDescriptor)pd);
					Method igetter = ipd.getIndexedReadMethod();
					if (igetter != null) {
						propertiesByGetter.put(igetter, p);
					}
					Method isetter = ipd.getIndexedWriteMethod();
					if (isetter != null) {
						propertiesBySetter.put(isetter, p);
					}
				}
				properties.put(p.getUri(), p);
			}
		}
	}

	private void checkSubjectProperty(PropertyDescriptor pd) throws RDFBeanValidationException {
		RDFSubject annotation = checkAnnotation(pd, RDFSubject.class);
		if (annotation != null) {
			if (subjectProperty == null) {
				subjectProperty = new SubjectProperty(pd, this, annotation);
			}
		}
	}

	private <T extends Annotation> T checkAnnotation(PropertyDescriptor pd, Class<T> theClass) {
		T annotation = null;
		Method getter = pd.getReadMethod();
		if (getter != null) {
			annotation = ReflectionUtil.getMethodAnnotation(getter, theClass);
		} else {
			Method setter = pd.getWriteMethod();
			if (setter != null) {
				annotation = ReflectionUtil.getMethodAnnotation(setter, theClass);
			}
		}
		if (annotation == null) {
			// last resort: try an appropriately named field (as with e.g. Lombok)
			try {
				Field field = rdfBeanClass.getDeclaredField(pd.getName());
				if (pd.getPropertyType().isAssignableFrom(field.getType())) {
					// field might be more specific, but needs to be compatible
					annotation = field.getDeclaredAnnotation(theClass);
				}
			} catch (NoSuchFieldException e) {
				// no luck then
				return null;
			}
		}
		return annotation;
	}

	private void initBeanType() throws RDFBeanValidationException {
		RDFBean ann = ReflectionUtil.getClassAnnotation(rdfBeanClass, RDFBean.class);
		if (ann == null) {
			throw new RDFBeanValidationException("Not an RDFBean: "
					+ RDFBean.class.getName() + " annotation is missing on "
					+ rdfBeanClass.getName() + " class or its interfaces", rdfBeanClass);
		} else {
			String type = ann.value();
			if (type != null) {
				try {
					rdfType = SimpleValueFactory.getInstance().createIRI(createUriString(type));
				}
				catch (IllegalArgumentException iae) {
					throw new RDFBeanValidationException(
							"RDF type parameter of " + RDFBean.class.getName() + " annotation on "
									+ rdfBeanClass.getName() + " class must be an absolute valid URI: " + type, rdfBeanClass);
				}
			} else {
				throw new RDFBeanValidationException(
						"Required RDF type parameter is missing in "
								+ RDFBean.class.getName() + " annotation on "
								+ rdfBeanClass.getName() + " class", rdfBeanClass);
			}
		}
	}

	private void initNamespaces() throws RDFBeanValidationException {
		RDFNamespaces packageNamespaces = ReflectionUtil.getPackageAnnotation(rdfBeanClass, RDFNamespaces.class);
		if (packageNamespaces != null) {
			registerNamespaces(packageNamespaces);
		}
		List<RDFNamespaces> nsAnns = ReflectionUtil.getAllClassAnnotations(rdfBeanClass, RDFNamespaces.class);
		for (RDFNamespaces nsAnn: nsAnns) {
			registerNamespaces(nsAnn);
		}
	}

	private void registerNamespaces(RDFNamespaces annotation) throws RDFBeanValidationException {
		for (String declaration: annotation.value()) {
			String[] split = declaration.split("=", 2);
			if (split.length == 2) {
				String prefix = split[0].trim();
				String value = split[1].trim();
				if (namespaces.containsKey(prefix)) {
					String currentValue = namespaces.get(prefix);
					throw new RDFBeanValidationException("Tried to re-declare namespace: '" + prefix + "'," + 
							"already defined as '" + currentValue + "'", rdfBeanClass);
				} else {
					namespaces.put(prefix, value);
				}
			}
			else {
				throw new RDFBeanValidationException("Wrong namespace declaration syntax: '" + declaration + "'", rdfBeanClass);
			}
		}
	}

	protected String createUriString(String s) {
		for (Map.Entry<String, String> me: namespaces.entrySet()) {
			String prefix = me.getKey() + ":";
			if (s.startsWith(prefix)) {
				s = me.getValue() + s.substring(prefix.length());
			}
		}
		return s;
	}

	public Class getRDFBeanClass() {
		return rdfBeanClass;
	}
	
	/**
	 * @return the beanInfo
	 */
	public BeanInfo getBeanInfo() {
		return beanInfo;
	}

	/**
	 * @return the subjectProperty
	 */
	public SubjectProperty getSubjectProperty() {
		return subjectProperty;
	}

	/**
	 * @return the properties
	 */
	public Collection<RDFProperty> getProperties() {
		return properties.values();
	}
	
	public RDFProperty getProperty(IRI uri) {
		return properties.get(uri);
	}

	public IRI getRDFType() {
		return rdfType;
	}
	
	public RDFProperty getPropertyForMethod(Method m) {
		RDFProperty p = propertiesByGetter.get(m);
		if (p == null) {
			p = propertiesBySetter.get(m);
		}
		return p;
	}
	
	public Map<String, String> getRDFNamespaces() {
		return namespaces;
	}

	
	/**
	 * InterfaceBeanInfo.
	 *
	 * @author alex
	 *
	 */
	class InterfaceBeanInfo extends SimpleBeanInfo {

		private Class iface;
		private BeanInfo parentBeanInfo;
		private PropertyDescriptor[] properties;
		private EventSetDescriptor[] events;
		private MethodDescriptor[] methods;
		
		InterfaceBeanInfo(Class iface, BeanInfo beanInfo) throws IntrospectionException {
			this.iface = iface;
			this.parentBeanInfo = beanInfo;
			this.properties = beanInfo.getPropertyDescriptors();
			this.events = beanInfo.getEventSetDescriptors();
			this.methods = beanInfo.getMethodDescriptors();
			introspect();
		}
		
		private void introspect() throws IntrospectionException {
			for (Class superIface: ReflectionUtil.getAllInterfaces(iface.getInterfaces())) {
				BeanInfo superBeanInfo = Introspector.getBeanInfo(superIface);
				properties = joinArrays(properties, superBeanInfo.getPropertyDescriptors());
				events = joinArrays(events, superBeanInfo.getEventSetDescriptors());
				methods = joinArrays(methods, superBeanInfo.getMethodDescriptors());
			}
		}

		private <T> T[] joinArrays(T[] a1,	T[] a2) {
			List<T> list = new ArrayList<T>(a1.length + a2.length); 
			list.addAll(Arrays.asList(a1));
			list.addAll(Arrays.asList(a2));
			return (T[]) list.toArray(a1);
		}

		@Override
		public BeanDescriptor getBeanDescriptor() {
			return parentBeanInfo.getBeanDescriptor();
		}

		@Override
		public PropertyDescriptor[] getPropertyDescriptors() {
			return properties;
		}

		@Override
		public int getDefaultPropertyIndex() {
			return parentBeanInfo.getDefaultPropertyIndex();
		}

		@Override
		public EventSetDescriptor[] getEventSetDescriptors() {
			return events;
		}

		@Override
		public int getDefaultEventIndex() {
			return parentBeanInfo.getDefaultEventIndex();
		}

		@Override
		public MethodDescriptor[] getMethodDescriptors() {
			return methods;
		}

		@Override
		public BeanInfo[] getAdditionalBeanInfo() {
			return parentBeanInfo.getAdditionalBeanInfo();
		}

		@Override
		public Image getIcon(int iconKind) {
			return parentBeanInfo.getIcon(iconKind);
		}
		
	}
	
}
