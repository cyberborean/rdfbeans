/**
 * RDFBeanDelegator.java
 * 
 * RDFBeans Feb 7, 2011 4:36:03 PM alex
 *
 * $Id: RDFBeanDelegator.java 39 2013-12-25 13:17:06Z alexeya $
 *  
 */
package com.viceversatech.rdfbeans.proxy;

import java.beans.IndexedPropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.Literal;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.Variable;

import com.viceversatech.rdfbeans.RDFBeanManager;
import com.viceversatech.rdfbeans.annotations.RDFContainer.ContainerType;
import com.viceversatech.rdfbeans.exceptions.RDFBeanException;
import com.viceversatech.rdfbeans.reflect.RDFBeanInfo;
import com.viceversatech.rdfbeans.reflect.RDFProperty;

/**
 * An InvocationHandler to handle invocations of getter and setter methods
 * on dynamic RDFBean proxies. 
 * 
 * @author alex
 * 
 */
public class RDFBeanDelegator implements InvocationHandler {
	
	// preloaded Method objects for the methods in java.lang.Object
    private static Method hashCodeMethod;
    private static Method equalsMethod;
    private static Method toStringMethod;
    
    static {
		try {
		    hashCodeMethod = Object.class.getMethod("hashCode", null);
		    equalsMethod = Object.class.getMethod("equals", new Class[] { Object.class });
		    toStringMethod = Object.class.getMethod("toString", null);
	        } 
		catch (NoSuchMethodException e) {
		    throw new NoSuchMethodError(e.getMessage());
		}
    }


	private Resource subject;
	private RDFBeanInfo rdfBeanInfo;
	private RDFBeanManager rdfBeanManager;
	private Model model;
	private boolean created = false;

	public RDFBeanDelegator(Resource subject, RDFBeanInfo rdfBeanInfo,
			RDFBeanManager rdfBeanManager) {
		this.subject = subject;
		this.rdfBeanInfo = rdfBeanInfo;
		this.rdfBeanManager = rdfBeanManager;
		this.model = rdfBeanManager.getModel();
	}

	public Object invoke(Object proxy, Method method, Object[] args)
			throws RDFBeanException, NoSuchMethodException {
		if (method.getDeclaringClass() == Object.class) {
			// invoke object method
			if (method.equals(hashCodeMethod)) {
				return proxyHashCode();
			}
			else if (method.equals(equalsMethod)) {
				return proxyEquals(args[0]);
			}
			else if (method.equals(toStringMethod)) {
				return proxyToString();
			}
			else {
				throw new RDFBeanException(
						"Unexpected object method dispatched: "
								+ method.getName() + " in "
								+ rdfBeanInfo.getRDFBeanClass().getName());
			}
		}
		if (method.equals(rdfBeanInfo.getSubjectProperty().getPropertyDescriptor().getReadMethod())) {
			// Return RDFBean ID
			return rdfBeanInfo.getSubjectProperty().getUriPart(subject);
		}
		if (method.equals(rdfBeanInfo.getSubjectProperty().getPropertyDescriptor().getWriteMethod())) {
			// no-op
			return null;
		}
		// make sure that model is opened
		if (!model.isOpen()) {
			throw new IllegalStateException("Cannot invoke " + method.getName() + " method in " + 
					rdfBeanInfo.getRDFBeanClass().getName() + ": underlying RDF model is not opened");
							
		}
		// invoke RDFBean method
		RDFProperty p = rdfBeanInfo.getPropertyForMethod(method);
		if (p != null) {
			if (method.equals(p.getPropertyDescriptor().getReadMethod())) {
				return getValue(p);
			} 
			else if (method.equals(p.getPropertyDescriptor().getWriteMethod())) {
				if (args.length < 1) {
					throw new NoSuchMethodException("Method " + method.getName()
							+ " in " + rdfBeanInfo.getRDFBeanClass().getName()
							+ " requires a " + p.getPropertyType()
							+ " argument.");
				}
				setValue(p, args[0]);
				fireObjectPropertyChanged(proxy, p.getUri(), args[0]);
				return null;
			}
			if (p.getPropertyDescriptor() instanceof IndexedPropertyDescriptor) {
				if (method.equals(((IndexedPropertyDescriptor)p.getPropertyDescriptor()).getIndexedReadMethod())) {
					if ((args.length == 1) && (args[0] instanceof Integer)) {
						Integer index = (Integer) args[0];
						Object array = getValue(p);
						return Array.get(array, index);
					}					
				}
				else if (method.equals(((IndexedPropertyDescriptor)p.getPropertyDescriptor()).getIndexedWriteMethod())) {
					if ((args.length == 2) && (args[0] instanceof Integer)) {
						Integer index = (Integer) args[0];
						Object array = getValue(p);
						Array.set(array, index, args[1]);
						setValue(p, array);
						fireObjectPropertyChanged(proxy, p.getUri(), array);
						return null;
					}
				}
			}
		}		
		throw new NoSuchMethodException(
					"Unexpected RDFBean proxy method dispatched: "
							+ method.toString() + " in "
							+ rdfBeanInfo.getRDFBeanClass().getName());	
	}

	
	private String proxyToString() {
		return subject.toString();
	}

	private Object proxyEquals(Object object) {
		return (object != null) && (proxyHashCode() == object.hashCode());
	}

	private int proxyHashCode() {	
		return subject.toString().hashCode();
	}

	/**
	 * @param uri
	 * @return
	 * @throws RDFBeanException
	 */
	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	private Object getValue(RDFProperty p) throws RDFBeanException {
		Object result = null;
		ClosableIterator<Statement> sts;
		if (p.isInversionOfProperty()) {
			sts = model.findStatements(Variable.ANY, p.getUri(), subject);
			if (!sts.hasNext()) {
				// try a container
				sts = model.sparqlConstruct("CONSTRUCT { ?subject <" + p.getUri() + "> <" + subject + "> } " + 
									  "WHERE { ?subject <" + p.getUri() + "> ?container. " +
									  		  "?container ?li <" + subject + ">" +
									  		" }").iterator();
			}			
		}
		else {
			sts = model.findStatements(subject,	p.getUri(), Variable.ANY);
		}
		// Determine field type
		Class fClass = p.getPropertyType();
		if (Collection.class.isAssignableFrom(fClass) || fClass.isArray()) {			
			// Collection property - collect all values
			// Check if an array or interface or abstract class			
			if (fClass.isArray() 
					|| List.class.equals(fClass)
					|| AbstractList.class.equals(fClass)) {
				fClass = ArrayList.class;
			}
			if (SortedSet.class.equals(fClass)) {
				fClass = TreeSet.class;
			} else if (Set.class.equals(fClass)
					|| AbstractSet.class.equals(fClass)
					|| Collection.class.equals(fClass)) {
				fClass = HashSet.class;
			}
			// Instantiate collection
			Collection items;
			try {
				items = (Collection) fClass.newInstance();
			} catch (Exception ex) {
				throw new RDFBeanException(ex);
			}
			// Determine component type
			Class cClass = p.getPropertyComponentType();
			// Collect values
			while (sts.hasNext()) {
				Node object;
				if (p.isInversionOfProperty()) {
					object = sts.next().getSubject();
				}
				else {
					object = sts.next().getObject();
				}
				Object item = unmarshalObject(object, cClass);
				if (item != null) {
					if (item instanceof Collection) {
						items.addAll((Collection) item);
					} else {
						items.add(item);
					}
				}
			}
			if (p.getPropertyType().isArray() && (items instanceof List)) {
				if (!items.isEmpty()) {
					List list = (List) items;
					Object array = Array.newInstance(list.get(0).getClass(), list.size());
					for (int i = 0; i < list.size(); i++) {
						Array.set(array, i, list.get(i));
					}
					result = array;
				}
				else {
					result = new Object[0];
				}
			}
			else {
				result = items;
			}
		} else {
			// Not a collection - get the first value only
			if (sts.hasNext()) {
				Node object;
				if (p.isInversionOfProperty()) {
					object = sts.next().getSubject();
				}
				else {
					object = sts.next().getObject();
				}
				Object value = unmarshalObject(object, fClass);
				if (value != null) {
					if ((value instanceof Collection)
							&& ((Collection) value).iterator().hasNext()) {
						value = ((Collection) value).iterator().next();
					}
					result = value;
				}
			}
		}
		sts.close();
		return result;
	}

	@SuppressWarnings({
		"rawtypes", "unchecked"
	})
	private Object unmarshalObject(Node object, Class<?> iface) throws RDFBeanException {
		if (object instanceof Literal) {
			// literal
			return rdfBeanManager.getDatatypeMapper().getJavaObject(
					object.asLiteral());
		} else if (object instanceof BlankNode) {
			// Blank node - check if an RDF collection
			Resource r = (Resource) object;
			if (model.contains(r, org.ontoware.rdf2go.vocabulary.RDF.type,
					org.ontoware.rdf2go.vocabulary.RDF.Alt)
					|| model.contains(r,
							org.ontoware.rdf2go.vocabulary.RDF.type,
							org.ontoware.rdf2go.vocabulary.RDF.Bag)
					|| model.contains(r,
							org.ontoware.rdf2go.vocabulary.RDF.type,
							org.ontoware.rdf2go.vocabulary.RDF.Seq)) {
				// Collect all items (ordered)
				ArrayList items = new ArrayList();
				int i = 0;
				Object item = null;
				do {
					item = null;
					ClosableIterator<Statement> itemst = model.findStatements(
							(Resource) object,
							org.ontoware.rdf2go.vocabulary.RDF.li(i),
							Variable.ANY);
					if (itemst.hasNext()) {
						item = unmarshalObject(itemst.next().getObject(), iface);
						if (item != null) {
							items.add(item);
						}
						i++;
					}
					itemst.close();
				} while (item != null);
				// Return collection
				return items; 
			}
		}
		
		// Resource

		// first check if we can construct a bean proxy using binding info
		Object o = rdfBeanManager.create((Resource) object);
		if ((o == null) && (iface != null) && iface.isInterface()) {
			// construct a bean proxy using provided interface
			o =	rdfBeanManager.create((Resource) object, iface);
		}
		if (o != null) {
			return o;
		}
		
		// URI
		return object.asURI().asJavaURI();
	}

	/**
	 * 
	 * TODO inverted properties
	 * 
	 * @param p
	 * @param args
	 * @throws RDFBeanException 
	 */
	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	private void setValue(RDFProperty p, Object value) throws RDFBeanException {
		
		/*
		if (p.isInversionOfProperty()) {
			throw new RDFBeanException("Value of a virtual property " +
					"(inversion of '" + p.getUri() +"') cannot be set on " + 
					rdfBeanInfo.getRDFBeanClass().getName());
		}*/
		
		if (value == null) {
			removeValue(p);
			return;
		}
				
		// Prepare value		
		if (value.getClass().isArray()) {
			// wrap array into the List
			ArrayList list = new ArrayList(Array.getLength(value));
			for (int i = 0; i < Array.getLength(value); i++) {
				list.add(Array.get(value, i));
			}
			value = list;
		}
		// XXX indexed properties		
		
		model.setAutocommit(false);
		
		
		// RDFBeanManager should take care of it:
		/*
		if (!created) {
			if (!rdfBeanManager.isResourceExist(subject)) {
				// Create resource
				model.addStatement(subject, org.ontoware.rdf2go.vocabulary.RDF.type, rdfBeanInfo.getRDFType());
				model.addStatement(rdfBeanInfo.getRDFType(), RDFBeanManager.BINDINGIFACE_PROPERTY, 
						model.createPlainLiteral(rdfBeanInfo.getRDFBeanClass().getName()));				
			}
			created = true;
		}*/
		
		// Clear old values
		if (p.isInversionOfProperty()) {
			model.removeStatements(Variable.ANY, p.getUri(), subject);
		}
		else {
			model.removeStatements(subject, p.getUri(), Variable.ANY);
		}
				
		if (p.getContainerType() == ContainerType.NONE) {
			if (value instanceof Collection) {
				// Collection
				Collection values = (Collection) value;
				// Create multiple triples
				for (Object v : values) {
					Node object = createNode(v);
					if (object != null) {
						if (p.isInversionOfProperty()) {
							if (object instanceof Resource) {
								model.addStatement((Resource)object, p.getUri(), subject);
							}
							else {
								throw new RDFBeanException("Value of the \"inverseOf\" property " + 
										p.getPropertyDescriptor().getName() + " of class " + 
										rdfBeanInfo.getRDFBeanClass().getName() + " must be of " +
										"an RDFBean type (was: " + object.getClass().getName() + ")");
							}
						}
						else {
							model.addStatement(subject, p.getUri(), object);
						}						
					}
				}
			}
			else {
				// Single value
				Node object = createNode(value);
				if (object != null) {	
					if (p.isInversionOfProperty()) {
						if (object instanceof Resource) {
							model.addStatement((Resource)object, p.getUri(), subject);
						}
						else {
							throw new RDFBeanException("Value of the \"inverseOf\" property " + 
									p.getPropertyDescriptor().getName() + " of class " + 
									rdfBeanInfo.getRDFBeanClass().getName() + " must be of " +
									"an RDFBean type (was: " + object.getClass().getName() + ")");
						}
					}
					else {
						model.addStatement(subject, p.getUri(), object);
					}					
				}
			}
		}
		else {
			if (!p.isInversionOfProperty()) {
				Collection values;
				if (value instanceof Collection) {
					values = (Collection) value;
				}
				else {
					values = Collections.singleton(value);
				}
				// Create RDF Container bNode
				URI ctype = org.ontoware.rdf2go.vocabulary.RDF.Bag;
				if (p.getContainerType() == ContainerType.SEQ) {
					ctype = org.ontoware.rdf2go.vocabulary.RDF.Seq;
				} else if (p.getContainerType() == ContainerType.ALT) {
					ctype = org.ontoware.rdf2go.vocabulary.RDF.Alt;
				}
				BlankNode collection = model.createBlankNode();
				model.addStatement(collection,
						org.ontoware.rdf2go.vocabulary.RDF.type, ctype);
				int i = 0;
				for (Object v : values) {
					Node object = createNode(v);
					if (object != null) {
						model.addStatement(collection,
								org.ontoware.rdf2go.vocabulary.RDF.li(i),
								object);
						i++;						
					}
				}
				model.addStatement(subject, p.getUri(), collection);
			}
			else {
				throw new RDFBeanException("RDF container type is not allowed for a \"inverseOf\" property " +
						p.getPropertyDescriptor().getName() + " of class " + 
						rdfBeanInfo.getRDFBeanClass().getName());
			}
		}
		
		if (rdfBeanManager.isAutocommit()) {
			model.commit();			
		}		
		
	}

	/**
	 * @param p
	 */
	private void removeValue(RDFProperty p) {
		model.setAutocommit(false);
		if (p.isInversionOfProperty()) {
			model.removeStatements(Variable.ANY, p.getUri(), subject);
		}
		else {
			model.removeStatements(subject, p.getUri(), Variable.ANY);
		}
		if (rdfBeanManager.isAutocommit()) {
			model.commit();
		}
	}

	private synchronized Node createNode(Object value)
			throws RDFBeanException {
		// Check if a Literal
		Literal l = rdfBeanManager.getDatatypeMapper().getRDFValue(value, model);
		if (l != null) {
			return l;
		}
		// Check if another RDFBean
		if (RDFBeanInfo.isRdfBean(value)) {
			RDFBeanInfo rbi = RDFBeanInfo.get(value.getClass());
			if (rbi.getSubjectProperty() == null) {
				throw new RDFBeanException("RDFSubject property is not declared in " + value.getClass().getName() + " class or its interfaces");
			}
			return (URI) rbi.getSubjectProperty().getValue(value);
		}
		// Check if a Resource
		if (value instanceof Resource) {
			return (Resource)value;
		}
		// Check if Java URI
		if (java.net.URI.class.isAssignableFrom(value.getClass())) {
			return model.createURI(((java.net.URI) value).toString());
		}
		
		throw new RDFBeanException("Unexpected value to set: " + value);
	}
	
	
	private void fireObjectPropertyChanged(Object object, URI property, Object newValue) {
		for (ProxyListener l : rdfBeanManager.getProxyListeners()) {
			l.objectPropertyChanged(object, property, newValue);
		}
	}

}
