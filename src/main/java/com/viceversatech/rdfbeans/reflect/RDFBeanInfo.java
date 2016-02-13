/**
 * RDFBeanInfo.java
 * 
 * RDFBeans Feb 4, 2011 8:28:46 PM alex
 *
 * $Id: RDFBeanInfo.java 36 2012-12-09 05:58:20Z alexeya $
 *  
 */
package com.viceversatech.rdfbeans.reflect;

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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;

import com.viceversatech.rdfbeans.annotations.RDF;
import com.viceversatech.rdfbeans.annotations.RDFBean;
import com.viceversatech.rdfbeans.annotations.RDFContainer;
import com.viceversatech.rdfbeans.annotations.RDFNamespaces;
import com.viceversatech.rdfbeans.annotations.RDFSubject;
import com.viceversatech.rdfbeans.exceptions.RDFBeanValidationException;

/**
 * RDFBeanInfo.
 * 
 * @author alex
 * 
 */
public class RDFBeanInfo {
	
	private static WeakHashMap<Class, RDFBeanInfo> rdfBeanInfoCache = new WeakHashMap<Class, RDFBeanInfo>();
	
	public static RDFBeanInfo get(Class rdfBeanClass)
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
	private Map<URI, RDFProperty> properties = new HashMap<URI, RDFProperty>();
	private Map<Method, RDFProperty> propertiesByGetter = new HashMap<Method, RDFProperty>();	
	private Map<Method, RDFProperty> propertiesBySetter = new HashMap<Method, RDFProperty>();
	private Map<String, String> namespaces = new HashMap<String, String>();
	private URI rdfType;

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
		RDFBean ann = ReflectionUtil.getClassAnnotation(rdfBeanClass, RDFBean.class);
		if (ann != null) {			
			String type = ann.value();
			if (type != null) {
				rdfType = createUri(type);
				if (!rdfType.asJavaURI().isAbsolute()) {
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
		} else {
			throw new RDFBeanValidationException("Not an RDFBean: "
					+ RDFBean.class.getName() + " annotation is missing on "
					+ rdfBeanClass.getName() + " class or its interfaces", rdfBeanClass);
		}
		for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
			Method getter = pd.getReadMethod();
			if (getter != null) {
				Annotation mAnn = ReflectionUtil.getMethodAnnotation(getter, RDF.class, RDFSubject.class);
				if (mAnn != null) {
					if (mAnn.annotationType().equals(RDF.class)) {
						Annotation cAnn = ReflectionUtil.getMethodAnnotation(getter, RDFContainer.class);
						RDFProperty p = new RDFProperty(pd, this, (RDF) mAnn, (RDFContainer) cAnn);
						propertiesByGetter.put(getter, p);
						Method setter = pd.getWriteMethod();
						if (setter != null) {
							/*if (p.isInversionOfProperty()) {
								throw new RDFBeanValidationException("The property '" + pd.getDisplayName() + "' is virtual " +
										"and may not have a setter method '" + setter.getName() + "' in " + rdfBeanClass.getName(), rdfBeanClass);
							}*/
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
					} else if (mAnn.annotationType().equals(RDFSubject.class)
							&& (subjectProperty == null)) {
						subjectProperty = new SubjectProperty(pd, this, (RDFSubject) mAnn);
					}
				}
			}			
		}
	}

	private void initNamespaces() throws RDFBeanValidationException {
		List<RDFNamespaces> nsAnns = ReflectionUtil.getAllClassAnnotations(rdfBeanClass, RDFNamespaces.class);
		for (RDFNamespaces nsAnn: nsAnns) {
			for (String s: nsAnn.value()) {		
				String[] ss = s.split("=", 2);
				if (ss.length == 2) {
					String prefix = ss[0].trim();
					String value = ss[1].trim();
					if (!namespaces.containsKey(prefix)) {
						namespaces.put(prefix, value);
					}
				}
				else {
					throw new RDFBeanValidationException("Wrong namespace declaration syntax: '" + s + "'", rdfBeanClass);
				}
			}
		}
	}
	
	protected URI createUri(String s) {		
		return new URIImpl(createUriString(s));
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
	
	public RDFProperty getProperty(URI uri) {
		return properties.get(uri);
	}

	public URI getRDFType() {
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
