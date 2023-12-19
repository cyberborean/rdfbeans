package org.cyberborean.rdfbeans.proxy;

import java.beans.IndexedPropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

import org.cyberborean.rdfbeans.RDFBeanManagerContext;
import org.cyberborean.rdfbeans.annotations.RDFContainer.ContainerType;
import org.cyberborean.rdfbeans.exceptions.RDFBeanException;
import org.cyberborean.rdfbeans.exceptions.RDFBeanValidationException;
import org.cyberborean.rdfbeans.reflect.RDFBeanInfo;
import org.cyberborean.rdfbeans.reflect.RDFProperty;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;

/**
 * An InvocationHandler to handle invocations of getter and setter methods
 * on dynamic RDFBean proxies. 
 * 
 */
public class RDFBeanDelegator implements InvocationHandler {
	
	// preloaded Method objects for the methods in java.lang.Object
    private static Method hashCodeMethod;
    private static Method equalsMethod;
    private static Method toStringMethod;
    
    static {
		try {
		    hashCodeMethod = Object.class.getMethod("hashCode");
		    equalsMethod = Object.class.getMethod("equals", Object.class);
		    toStringMethod = Object.class.getMethod("toString");
	        } 
		catch (NoSuchMethodException e) {
		    throw new NoSuchMethodError(e.getMessage());
		}
    }


	private Resource subject;
	private RDFBeanInfo rdfBeanInfo;
	private RDFBeanManagerContext rdfBeanManagerContext;
	private IRI context;

	public RDFBeanDelegator(Resource subject, RDFBeanInfo rdfBeanInfo,
			RDFBeanManagerContext rdfBeanManagerContext) {
		this.subject = subject;
		this.rdfBeanInfo = rdfBeanInfo;
		this.rdfBeanManagerContext = rdfBeanManagerContext;
		this.context = rdfBeanManagerContext.getContext();
	}		
	
	private RepositoryConnection getRepositoryConnection() {
		RepositoryConnection conn = rdfBeanManagerContext.getRepositoryConnection();
		
		// make sure that model is opened
		if (!conn.isOpen()) {
			throw new IllegalStateException(rdfBeanInfo.getRDFBeanClass().getName() + ": RepositoryConnection is not opened");
							
		}
		return conn;		
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws RDFBeanException, NoSuchMethodException, RDF4JException {
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
			return rdfBeanInfo.getSubjectProperty().getUriPart((IRI)subject);
		}
		if (method.equals(rdfBeanInfo.getSubjectProperty().getPropertyDescriptor().getWriteMethod())) {
			// no-op
			return null;
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
	 * @throws RDF4JException
	 * @throws RepositoryException 
	 */
	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	private Object getValue(RDFProperty p) throws RDFBeanException, RepositoryException, RDF4JException {
		RepositoryConnection conn = getRepositoryConnection();
		Object result = null;
		CloseableIteration<Statement, ? extends RDF4JException> sts;
		if (p.isInversionOfProperty()) {
			sts = conn.getStatements(null, p.getUri(), subject, false, (IRI)context);
			if (!sts.hasNext()) {
				// try a container
				GraphQuery q = conn.prepareGraphQuery(QueryLanguage.SPARQL, "CONSTRUCT { ?subject <" + p.getUri() + "> <" + subject + "> } " + 
						  "WHERE { ?subject <" + p.getUri() + "> ?container. " +
				  		  "?container ?li <" + subject + ">" +
				  		" }");
				sts = q.evaluate();
			}			
		}
		else {
			sts = conn.getStatements(subject, p.getUri(), null, false, (IRI)context);
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
				Value object;
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
				Value object;
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
			else {
				result = checkPrimitiveTypeDefault(p.getPropertyType());
			}
		}
		sts.close();
				
		return result;
	}

	private Object checkPrimitiveTypeDefault(Class<?> type) {
		if (type.equals(int.class)) {
			return Integer.valueOf(0);
		}
		if (type.equals(float.class)) {
			return Float.valueOf(0.0f);
		}
		if (type.equals(double.class)) {
			return Double.valueOf(0.0d);
		}
		if (type.equals(long.class)) {
			return Long.valueOf(0L);
		}
		if (type.equals(short.class)) {
			return Short.valueOf((short) 0);
		}
		if (type.equals(byte.class)) {
			return Byte.valueOf((byte) 0);
		}
		if (type.equals(char.class)) {
			return Character.valueOf('\u0000');
		}
		if (type.equals(boolean.class)) {
			return Boolean.FALSE;
		}		
		return null;
	}

	@SuppressWarnings({
		"rawtypes", "unchecked"
	})
	private Object unmarshalObject(Value object, Class<?> iface) throws RDFBeanException, RepositoryException {
		if (object instanceof Literal) {
			// literal
			return rdfBeanManagerContext.getDatatypeMapper().getJavaObject((Literal)object);
		}
		else if (object instanceof BNode) {
			RepositoryConnection conn = getRepositoryConnection();
			// Blank node - check if an RDF collection
			Resource r = (Resource) object;
			if (conn.hasStatement(r, RDF.TYPE, RDF.BAG, false, (IRI)context) 
					|| conn.hasStatement(r, RDF.TYPE, RDF.SEQ, false, (IRI)context)
					|| conn.hasStatement(r, RDF.TYPE, RDF.ALT, false, (IRI)context)) {	
				// Collect all items (ordered)
				ArrayList items = new ArrayList();
				int i = 1;
				Object item = null;
				do {
					item = null;
					RepositoryResult<Statement> itemst = conn.getStatements(
							(Resource) object,
							conn.getValueFactory().createIRI(RDF.NAMESPACE, "_" + i),
							null, false, (IRI)context);
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
		else if (object instanceof IRI) {
			// Possibly, another RDFBean
			// try to construct a bean proxy using provided interface
			Object proxy = rdfBeanManagerContext.create((IRI) object, iface);
			if (proxy != null) {
				return proxy;
			}
			else {
				// check if a non-existent RDFBean
				try {
					RDFBeanInfo.get(iface);
					return null;
				}
				catch (RDFBeanValidationException e) {
					// continue
				}
			}
		}
		
		// return original RDF value
		return object;
	}
	
	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	private void setValue(RDFProperty p, Object value) throws RDFBeanException, RepositoryException {		
		RepositoryConnection conn = getRepositoryConnection();
		
		if (value == null) {			
			if (p.isInversionOfProperty()) {
				conn.remove((Resource)null, p.getUri(), subject, (IRI)context);
			}
			else {
				conn.remove(subject, p.getUri(), null, (IRI)context);
			}	
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
		
		boolean newTxn = !conn.isActive();
		if (newTxn) {
			conn.begin();
		}
		try {		
			// Clear old values
			if (p.isInversionOfProperty()) {
				conn.remove((Resource)null, p.getUri(), subject, (IRI)context);
			}
			else {
				if (value instanceof Collection && p.getContainerType() != ContainerType.NONE) {
					// remove RDF container
					RepositoryResult<Statement> stmts = conn.getStatements(subject, p.getUri(), null, (IRI)context);
					while (stmts.hasNext()) {
						Resource bNode = (Resource) stmts.next().getObject();
						conn.remove(bNode, null, null, (IRI)context);
					}
				}
				conn.remove(subject, p.getUri(), null, (IRI)context);
			}
					
			if (p.getContainerType() == ContainerType.NONE) {
				if (value instanceof Collection) {
					// Collection
					Collection values = (Collection) value;
					// Create multiple triples
					for (Object v : values) {
						Value object = toRdf(v, conn.getValueFactory());
						if (object != null) {
							if (p.isInversionOfProperty()) {
								if (object instanceof Resource) {								
									conn.add((Resource)object, p.getUri(), subject, (IRI)context);
								}
								else {
									throw new RDFBeanException("Value of the \"inverseOf\" property " + 
											p.getPropertyDescriptor().getName() + " of class " + 
											rdfBeanInfo.getRDFBeanClass().getName() + " must be of " +
											"an RDFBean type (was: " + object.getClass().getName() + ")");
								}
							}
							else {
								conn.add(subject, p.getUri(), object, (IRI)context);
							}						
						}
					}
				}
				else {
					// Single value
					Value object = toRdf(value, conn.getValueFactory());
					if (object != null) {	
						if (p.isInversionOfProperty()) {
							if (object instanceof Resource) {
								conn.add((Resource)object, p.getUri(), subject, (IRI)context);
							}
							else {
								throw new RDFBeanException("Value of the \"inverseOf\" property " + 
										p.getPropertyDescriptor().getName() + " of class " + 
										rdfBeanInfo.getRDFBeanClass().getName() + " must be of " +
										"an RDFBean type (was: " + object.getClass().getName() + ")");
							}
						}
						else {
							conn.add(subject, p.getUri(), object, (IRI)context);
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
					IRI ctype = RDF.BAG;
					if (p.getContainerType() == ContainerType.SEQ) {
						ctype = RDF.SEQ;
					} else if (p.getContainerType() == ContainerType.ALT) {
						ctype = RDF.ALT;
					}
					BNode collection = conn.getValueFactory().createBNode();
					conn.add(collection, RDF.TYPE, ctype, (IRI)context);
					int i = 1;
					for (Object v : values) {
						Value object = toRdf(v, conn.getValueFactory());
						if (object != null) {
							conn.add(collection,
									conn.getValueFactory().createIRI(RDF.NAMESPACE, "_" + i),
									object, (IRI)context);
							i++;
						}
					}
					conn.add(subject, p.getUri(), collection, (IRI)context);
				}
				else {
					throw new RDFBeanException("RDF container type is not allowed for a \"inverseOf\" property " +
							p.getPropertyDescriptor().getName() + " of class " + 
							rdfBeanInfo.getRDFBeanClass().getName());
				}
			}
			if (newTxn) {
				conn.commit();			
			}
		}
		catch (RepositoryException e) {
			if (newTxn) {
				conn.rollback();			
			}
			throw e;
		}			
		
	}

	private synchronized Value toRdf(Object value, ValueFactory valueFactory)
			throws RDFBeanException {
		// Check if a Literal
		Literal l = rdfBeanManagerContext.getDatatypeMapper().getRDFValue(value, valueFactory);
		if (l != null) {
			return l;
		}
		// Check if another RDFBean
		if (RDFBeanInfo.isRdfBean(value)) {
			checkIfTheSameContext(value);
			RDFBeanInfo rbi = RDFBeanInfo.get(value.getClass());
			if (rbi.getSubjectProperty() == null) {
				throw new RDFBeanException("RDFSubject property is not declared in " + value.getClass().getName() + " class or its interfaces");
			}
			return (IRI) rbi.getSubjectProperty().getValue(value);
		}
		// Check if a Resource
		if (value instanceof Resource) {
			return (Resource)value;
		}
		// Check if Java URI
		if (java.net.URI.class.isAssignableFrom(value.getClass())) {
			return valueFactory.createIRI(value.toString());
		}
		
		throw new RDFBeanException("Unexpected value to set: " + value);
	}
	
	private void checkIfTheSameContext(Object value) {
		if (Proxy.isProxyClass(value.getClass())) {
			InvocationHandler h = Proxy.getInvocationHandler(value);
			if (h instanceof RDFBeanDelegator && (context != ((RDFBeanDelegator)h).context)) {
				throw new RDFBeanException("The value must be in the same RDF context (was " + ((RDFBeanDelegator)h).context.stringValue() + ")");
			}
		}
	}

	private void fireObjectPropertyChanged(Object object, IRI property, Object newValue) {
		for (ProxyListener l : rdfBeanManagerContext.getProxyListeners()) {
			l.objectPropertyChanged(object, property, newValue);
		}
	}

}
