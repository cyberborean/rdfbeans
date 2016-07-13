
package org.cyberborean.rdfbeans;

import java.net.URISyntaxException;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.WeakHashMap;

import org.cyberborean.rdfbeans.annotations.RDFContainer.ContainerType;
import org.cyberborean.rdfbeans.annotations.RDFSubject;
import org.cyberborean.rdfbeans.datatype.DatatypeMapper;
import org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper;
import org.cyberborean.rdfbeans.exceptions.RDFBeanException;
import org.cyberborean.rdfbeans.exceptions.RDFBeanValidationException;
import org.cyberborean.rdfbeans.proxy.ProxyInstancesPool;
import org.cyberborean.rdfbeans.proxy.ProxyListener;
import org.cyberborean.rdfbeans.reflect.RDFBeanInfo;
import org.cyberborean.rdfbeans.reflect.RDFProperty;
import org.cyberborean.rdfbeans.reflect.SubjectProperty;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;

/**
 * 
 * RDFBeans databinding functions are accessible as methods of a single
 * RDFBeanManager class. An RDFBeanManager instance is created with a RDF2Go
 * Model which provides an abstraction layer to access an underlying physical
 * RDF storage. Currently, RDF2Go project provides implementations of Model
 * interface (adapters) for Sesame 2.x and Jena frameworks.
 * 
 * A Model instance is passed as an argument to the RDFBeanManager constructor.
 * The Model implementations may require the model to be opened (initialized)
 * before and closed after use. The following example illustrates how to setup
 * RDFBeans databinding with a model adapter determined automatically via RDF2Go
 * ModelFactory mechanism:
 * 
 * <pre>
 * import org.cyberborean.rdfbeans.RDFBeanManager; 
 * import org.ontoware.rdf2go.ModelFactory; 
 * import org.ontoware.rdf2go.RDF2Go; 
 * import org.ontoware.rdf2go.model.Model; 
 * ...
 * 
 * ModelFactory modelFactory = RDF2Go.getModelFactory(); 
 * Model model = modelFactory.createModel(); 
 * model.open(); 
 * RDFBeanManager manager = new RDFBeanManager(model); 
 * ... 
 * model.close();
 * </pre>
 * 
 * An example with hardcoded Sesame 2.x NativeStore model implementation:
 * 
 * <pre>
 * import org.cyberborean.rdfbeans.RDFBeanManager; 
 * import org.ontoware.rdf2go.model.Model; 
 * import org.openrdf.rdf2go.RepositoryModel;
 * import org.openrdf.repository.Repository; 
 * import org.openrdf.repository.sail.SailRepository; 
 * import org.openrdf.sail.nativerdf.NativeStore; 
 * ...
 * 
 * Repository repository = new SailRepository(new NativeStore(new File("~/.sesame/test"))); 
 * repository.initialize(); 
 * Model model = new RepositoryModel(repository); 
 * model.open(); 
 * RDFBeanManager manager = new RDFBeanManager(model); 
 * ... 
 * model.close();
 * </pre>
 * 
 * For detailed information on RDF2Go configuration for specific triple store
 * adapters, please refer to RDF2Go documentation.
 * 
 * 
 * @author alex
 * @version $Id:$
 * 
 */
public class RDFBeanManager {

	public static final ValueFactory valueFactory = SimpleValueFactory.getInstance();
	public static final IRI BINDINGCLASS_PROPERTY = valueFactory.createIRI(
			"http://viceversatech.com/rdfbeans/2.0/bindingClass");
	public static final IRI BINDINGIFACE_PROPERTY = valueFactory.createIRI(
			"http://viceversatech.com/rdfbeans/2.0/bindingIface");

	private RepositoryConnection conn;
	private boolean autocommit = true;
	private ClassLoader classLoader;
	private DatatypeMapper datatypeMapper = new DefaultDatatypeMapper();

	private WeakHashMap<Object, Resource> resourceCache;
	private WeakHashMap<Resource, Object> objectCache;
	private Map<IRI, Class> classCache;
	private ProxyInstancesPool proxies;
	private List<ProxyListener> proxyListeners = new Vector<ProxyListener>();

	/**
	 * Creates new RDFBeanManager instance upon the given RDF2Go model.
	 * 
	 * @param conn
	 */
	public RDFBeanManager(RepositoryConnection conn) {
		this.conn = conn;
		this.classLoader = this.getClass().getClassLoader();
		this.classCache = new HashMap<>();
		this.proxies = new ProxyInstancesPool(this);
	}
	
	public RepositoryConnection getRepositoryConnection() {		
		return conn;
	}

	// ====================== RDFBean classes functionality ====================

	/**
	 * Marshall the state of an RDFBean object to an RDF resource (a set of
	 * triple statements) in the underlying RDF model.
	 * 
	 * <p>
	 * If the RDFBean object has not-null property, annotated with
	 * {@link RDFSubject}, the method returns absolute URI of RDF resource.
	 * Otherwise, RDF BlankNode is returned.
	 * 
	 * <p>
	 * If an RDF representation of the given unanonymous object already exists
	 * in the model, the method immediately returns the RDF resource without
	 * changing the model.
	 * 
	 * <p>
	 * If autocommit mode is on (see {@link setAutocommit(boolean)}), the
	 * statements are commited into the RDF model in a single transaction.
	 * Otherwise, the transaction is delayed until the <code>commit()</code>
	 * method of the underlying Model implementation is invoked.
	 * 
	 * @param o
	 *            RDFBean to add
	 * @return Resource URI or BlankNode for anonymous RDFBean
	 * @throws RDFBeanException
	 *             If the object is not a valid RDFBean
	 * @throws RepositoryException 
	 * 
	 * @see update(Object)
	 * @see setAutocommit(boolean)
	 */
	public synchronized Resource add(Object o) throws RDFBeanException, RepositoryException {
		return addOrUpdate(o, false);
	}

	/**
	 * Marshall the state of an RDFBean object to update an existing RDF
	 * resource in the underlying RDF model.
	 * 
	 * <p>
	 * If no resource for the given object exists, or the object is anonymous
	 * RDFBean represented with a BlankNode, the method works like
	 * {@link #add(Object) add()}.
	 * 
	 * <p>
	 * If autocommit mode is on (see {@link setAutocommit(boolean)}), the
	 * statements are commited into the RDF model in a single transaction.
	 * Otherwise, the transaction is delayed until the <code>commit()</code>
	 * method of the underlying Model implementation is invoked.
	 * 
	 * @param o
	 *            RDFBean to update
	 * @return Resource URI or BlankNode for anonymous RDFBean
	 * @throws RDFBeanException
	 *             If the object is not a valid RDFBean
	 * @throws RepositoryException 
	 * 
	 * @see add(Object)
	 * @see setAutocommit(boolean)
	 */
	public synchronized Resource update(Object o) throws RDFBeanException, RepositoryException {
		return addOrUpdate(o, true);
	}

	/**
	 * Unmarshall an RDF resource to an instance of the specified RDFBean class.
	 * 
	 * @param r
	 *            Resource URI (or BlankNode for anonymous RDFBeans).
	 * @param rdfBeanClass
	 *            Java class of RDFBean
	 * @return Unmarshalled RDFBean object, or null if the resource does not
	 *         exists
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean or an instance of this
	 *             class cannot be created
	 * @throws RDF4JException
	 * @see get(Resource)
	 * @see get(String,Class)
	 * @see getAll(Class)
	 */

	public <T> T get(Resource r, Class<T> rdfBeanClass) throws RDFBeanException, RDF4JException {
		if (!this.isResourceExist(r)) {
			return null;
		}
		return this._get(r, rdfBeanClass);
	}

	/**
	 * 
	 * Unmarshall an RDF resource to an RDFBean instance.
	 * 
	 * <p>
	 * The method tries to autodetect an RDFBean Java class using information
	 * added to the model at marshalling. If a binding class information is not
	 * found, RDFBeanException is thrown.
	 * 
	 * @param r
	 *            Resource URI or BlankNode for anonymous RDFBeans.
	 * @return Unmarshalled RDFBean object, or null if the resource does not
	 *         exists in the model
	 * @throws RDFBeanException
	 *             If the binding class cannot be detected, is not a valid
	 *             RDFBean or an instance of this class cannot be created
	 * @throws RDF4JException
	 * @see get(Resource,Class)
	 * @see get(String,Class)
	 * @see getAll(Class)
	 */
	public Object get(Resource r) throws RDFBeanException, RDF4JException {
		if (!this.isResourceExist(r)) {
			return null;
		}
		Class<?> cls = getBindingClass(r);
		if (cls == null) {
			throw new RDFBeanException("Cannot detect a binding class for "
					+ r.stringValue());
		}
		return this._get(r, cls);
	}

	/**
	 * Unmarshall an RDF resource matching specified RDFBean identifier to an
	 * instance of the specified RDFBean class.
	 * 
	 * <p>
	 * If a namespace prefix is defined in {@link RDFSubject} declaration for
	 * this RDFBean class, the provided identifier value is interpreted as a
	 * local part of fully qualified RDFBean name (RDF resource URI). Otherwise,
	 * the fully qualified name must be provided.
	 * 
	 * @param stringId
	 *            RDFBean ID value
	 * @param rdfBeanClass
	 *            Java class of RDFBean
	 * @return The unmarshalled Java object, or null no resource matching the
	 *         given ID is found exists
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean or an instance of this
	 *             class cannot be created
	 * @throws RDF4JException
	 * @see get(Resource)
	 * @see get(Resource,Class)
	 * @see getAll(Class)
	 */
	public <T> T get(String stringId, Class<T> rdfBeanClass)
			throws RDFBeanException, RDF4JException {
		Resource r = this.getResource(stringId, rdfBeanClass);
		if (r != null) {
			return this.get(r, rdfBeanClass);
		}
		return null;
	}

	/**
	 * Obtain an iterator over all instances of specified RDFBean class stored
	 * in the RDF model
	 * 
	 * <p>
	 * The returned Iterator performs lazy unmarshalling of RDFBean objects (on
	 * every <code>next()</code> call) without any specific order. When
	 * iterating is done, the caller must invoke the <code>close()</code> method
	 * of CloseableIterator to release the resources of the underlying RDF model
	 * implementation.
	 * 
	 * @param rdfBeanClass
	 *            Java class of RDFBeans
	 * @return Iterator over RDFBean instances
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean
	 * @throws RepositoryException 
	 */
	public <T> CloseableIteration<T, Exception> getAll(final Class<T> rdfBeanClass)
			throws RDFBeanException, RepositoryException {
		RDFBeanInfo rbi = RDFBeanInfo.get(rdfBeanClass);
		IRI type = rbi.getRDFType();
		if (type == null) {
			return new CloseableIteration<T, Exception>() {

				@Override
				public boolean hasNext() throws Exception {
					return false;
				}

				@Override
				public T next() throws Exception {
					return null;
				}

				@Override
				public void remove() throws Exception {
					throw new UnsupportedOperationException();
				}

				@Override
				public void close() throws Exception {}
				
			};
		}		
		
		final CloseableIteration<Statement, RepositoryException> sts = conn.getStatements(null, RDF.TYPE, type, false);
		
		return new CloseableIteration<T, Exception>() {

			@Override
			public boolean hasNext() throws Exception {
				return sts.hasNext();
			}

			@Override
			public T next() throws Exception {
				return _get(sts.next().getSubject(), rdfBeanClass);
			}

			@Override
			public void remove() throws Exception {
				throw new UnsupportedOperationException();
			}

			@Override
			public void close() throws Exception {
				sts.close();
			}
		};
	}

	/**
	 * Check if a RDF resource exists in the underlying model.
	 * 
	 * @param r
	 *            Resource URI or BlankNode
	 * @return true, if the model contains the statements with the given
	 *         subject.
	 * @throws RepositoryException 
	 */
	public boolean isResourceExist(Resource r) throws RepositoryException {
		return hasStatement(r, null, null);
	}
	
	public boolean isResourceExist(Resource r, Class rdfBeanClass) throws RDFBeanValidationException, RepositoryException {
		RDFBeanInfo rbi = RDFBeanInfo.get(rdfBeanClass);
		return hasStatement(r, RDF.TYPE, rbi.getRDFType());
	}
	

	private boolean hasStatement(Resource s, IRI p, Value o) throws RepositoryException {
		return conn.hasStatement(s, p, o, false);
	}

	/**
	 * Resolve the RDFBean identifier to an RDF resource URI.
	 * 
	 * <p>
	 * If a namespace prefix is defined in {@link RDFSubject} declaration for
	 * this RDFBean class, the provided identifier value is interpreted as a
	 * local part of fully qualified RDFBean name (RDF resource URI). Otherwise,
	 * the fully qualified name must be provided.
	 * 
	 * @param stringId
	 *            RDFBean ID value
	 * @param rdfBeanClass
	 *            Java class of RDFBean
	 * @return Resource URI, or null if no resource matching the given RDFBean
	 *         ID found.
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean
	 * @throws RepositoryException 
	 */
	public Resource getResource(String stringId, Class rdfBeanClass)
			throws RDFBeanException, RepositoryException {
		SubjectProperty subject = RDFBeanInfo.get(rdfBeanClass)
				.getSubjectProperty();
		if (subject != null) {
			IRI r = subject.getUri(stringId);
			if (isResourceExist(r)) {
				return r;
			}
		}
		return null;
	}

	/**
	 * Delete the RDF resource from underlying model.
	 * 
	 * <p>
	 * If autocommit mode is on (see {@link setAutocommit(boolean)}), the
	 * statements are removed from the RDF model as a single transaction.
	 * Otherwise, the transaction is delayed until the <code>commit()</code>
	 * method of the underlying Model implementation is invoked.
	 * 
	 * @param uri Resource URI
	 * @throws RepositoryException 
	 * @see delete(String,Class)
	 * @see setAutocommit(boolean)
	 */
	public boolean delete(Resource uri) throws RepositoryException {
		if (isResourceExist(uri)) {
			deleteInternal(uri);
			return true;
		}
		return false;
	}
	
	private void deleteInternal(Resource uri) throws RepositoryException {
		if (isAutocommit()) {
			conn.begin();
		}
		try {
			// delete where is a subject
			conn.remove(uri, null, null);
			// delete where is an object
			conn.remove((Resource)null, null, uri);
			proxies.purge(uri);
			if (isAutocommit()) {
				conn.commit();
			}
		}
		catch (RepositoryException e) {
			if (isAutocommit()) {
				conn.rollback();					
			}
			throw e;
		}
	}

	/**
	 * Delete an RDF resource matching the specified RDFBean identifier from
	 * underlying model.
	 * 
	 * <p>
	 * If autocommit mode is on (see {@link setAutocommit(boolean)}), the
	 * statements are removed from the RDF model as a single transaction.
	 * Otherwise, the transaction is delayed until the <code>commit()</code>
	 * method of the underlying Model implementation is invoked.
	 * 
	 * @param stringId
	 *            RDFBean ID value
	 * @param rdfBeanClass
	 *            Java class of RDFBean
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean
	 * @throws RepositoryException 
	 * 
	 * @see delete(Resource)
	 * @see setAutocommit(boolean)
	 */
	public void delete(String stringId, Class rdfBeanClass)
			throws RDFBeanException, RepositoryException {
		Resource r = this.getResource(stringId, rdfBeanClass);
		if (r != null) {
			this.delete(r);
		}
	}

	private synchronized Resource addOrUpdate(Object o, boolean update) throws RDFBeanException, RepositoryException {
		this.resourceCache = new WeakHashMap<Object, Resource>();
		if (isAutocommit()) {
			conn.begin();
		}
		Resource node;
		try {
			node = marshal(o, update);
			if (isAutocommit()) {
				conn.commit();
			}
		} catch (RDFBeanException | RepositoryException e) {
			if (isAutocommit()) {
				conn.rollback();
			}
			throw e;
		}
		return node;
	}
	
	private Resource marshal(Object o, boolean update)
			throws RDFBeanException, RepositoryException {
		// Check if object is already marshalled
		Resource subject = this.resourceCache.get(o);
		if (subject != null) {
			// return cached node
			return subject;
		}
		// Indentify RDF type
		Class cls = o.getClass();
		RDFBeanInfo rbi = RDFBeanInfo.get(cls);
		IRI type = rbi.getRDFType();
		conn.add(type, BINDINGCLASS_PROPERTY, conn.getValueFactory().createLiteral(cls.getName()));
		
		// introspect RDFBEan
		SubjectProperty sp = rbi.getSubjectProperty();
		if (sp != null) {
			Object value = sp.getValue(o);
			if (value != null) {
				subject = (IRI) value;
			} 
			else {
				// NOP no pb, will create blank node
			}
		}
		if (subject == null) {
			// Blank node
			subject = conn.getValueFactory().createBNode();
		} 
		else if (hasStatement(subject, null, null)) {
			// Resource is already in the model
			if (update) {
				// Remove existing triples
				conn.remove(subject, null, null);
			} 
			else {
				// Will not be added
				return subject;
			}
		}
		// Add subject to cache
		this.resourceCache.put(o, subject);
		// Add rdf:type
		conn.add(subject, RDF.TYPE, type);
		// Add properties
		for (RDFProperty p : rbi.getProperties()) {
			IRI predicate = p.getUri();
			Object value = p.getValue(o);
			if (p.isInversionOfProperty()) {
				conn.remove((Resource)null, predicate, subject);
			}
			if (value != null) {				
				if (isCollection(value)) {
					// Collection
					Collection values = (Collection) value;
					if (p.getContainerType() == ContainerType.NONE) {
						// Create multiple triples
						for (Object v : values) {
							Value object = toRdf(v);
							if (object != null) {								
								if (p.isInversionOfProperty()) {
									if (object instanceof Resource) {	
										conn.add((Resource)object, predicate, subject);
									}
									else {
										throw new RDFBeanException("Value of the \"inverseOf\" property " + 
												p.getPropertyDescriptor().getName() + " of class " + 
												rbi.getRDFBeanClass().getName() + " must be of " +
												"an RDFBean type (was: " + object.getClass().getName() + ")");
									}
								}
								else {
									conn.add(subject, predicate, object);
								}
							}
						}
					} 
					else if (p.getContainerType() == ContainerType.LIST) {
						if (p.isInversionOfProperty()) {
							throw new RDFBeanException("RDF container type is not allowed for a \"inverseOf\" property " +
									p.getPropertyDescriptor().getName() + " of class " +
									rbi.getRDFBeanClass().getName());
						}
						marshalLinkedList(values, subject, p);
					}
					else {
						if (!p.isInversionOfProperty()) {
							// Create RDF Container bNode							
							IRI ctype = RDF.BAG;
							if (p.getContainerType() == ContainerType.SEQ) {
								ctype = RDF.SEQ;
							} else if (p.getContainerType() == ContainerType.ALT) {
								ctype = RDF.ALT;
							}
							BNode collection = conn.getValueFactory().createBNode();
							conn.add(collection, RDF.TYPE, ctype);
							int i = 1;
							for (Object v : values) {
								Value object = toRdf(v);
								if (object != null) {
									conn.add(collection,
											conn.getValueFactory().createIRI(RDF.NAMESPACE, "_" + i++),
											object);
								}
							}
							conn.add(subject, predicate, collection);
						}
						else {
							throw new RDFBeanException("RDF container type is not allowed for a \"inverseOf\" property " +
									p.getPropertyDescriptor().getName() + " of class " + 
									rbi.getRDFBeanClass().getName());
						}
					}
				} 
				else {
					// Single value
					Value object = toRdf(value);
					if (object != null) {
						if (p.isInversionOfProperty()) {
							if (object instanceof Resource) {
								conn.add((Resource)object, predicate, subject);
							}
							else {
								throw new RDFBeanException("Value of the \"inverseOf\" property " + 
										p.getPropertyDescriptor().getName() + " of class " + 
										rbi.getRDFBeanClass().getName() + " must be of " +
										"an RDFBean type (was: " + object.getClass().getName() + ")");
							}
						}
						else {
							conn.add(subject, predicate, object);
						}
					}
				}
			}
		}
		return subject;
	}

	private void marshalLinkedList(Collection values, Resource subject, RDFProperty property) throws RDFBeanException, RepositoryException {
		BNode listHead = conn.getValueFactory().createBNode();
		conn.add(subject, property.getUri(), listHead);
		Iterator<Object> value = values.iterator();
		do {
			if (value.hasNext()) {
				Value valueNode = toRdf(value.next());
				conn.add(listHead, RDF.FIRST, valueNode);
			}
			if (value.hasNext()) {
				BNode newHead = conn.getValueFactory().createBNode();
				conn.add(listHead, RDF.REST, newHead);
				listHead = newHead;
			} else {
				conn.add(listHead, RDF.REST, RDF.NIL);
			}
		} while (value.hasNext());
	}

	private Value toRdf(Object value)
			throws RDFBeanException, RepositoryException {
		// Check if another RDFBean
		if (RDFBeanInfo.isRdfBean(value)) {
			return marshal(value, false);
		}
		// Check if URI
		if (java.net.URI.class.isAssignableFrom(value.getClass())) {
			return conn.getValueFactory().createIRI(value.toString());
		}
		// Check if a Literal
		Literal l = getDatatypeMapper().getRDFValue(value, conn.getValueFactory());
		if (l != null) {
			return l;
		}
		throw new RDFBeanException("Unsupported class [" + value.getClass().getName() + "] of value " + value.toString());
	}

	private static boolean isCollection(Object value) {
		return value instanceof Collection;
	}

	private Class<?> getBindingClass(Resource r) throws RDFBeanException, RepositoryException {		
		Class<?> cls = null;
		try (CloseableIteration<Statement, RepositoryException> ts =
				conn.getStatements(r, RDF.TYPE, null, false)) {
			while (cls == null && ts.hasNext()) {
				Value type = ts.next().getObject();
				if (type instanceof IRI) {
					cls = getBindingClassForType((IRI)type);
				}
				else {
					throw new RDFBeanException("Resource " + r.stringValue() + " has invalid RDF type " + type.stringValue() + ": not a URI");
				}
			}
		}
		return cls;
	}

	protected Class<?> getBindingClassForType(IRI rdfType) throws RDFBeanException, RepositoryException{
		Class cls = classCache.get(rdfType);
		if (cls != null) {
			return cls;
		}
		String className = null;		
		RepositoryResult<Statement> ts = null;
		try {
			ts = conn.getStatements(rdfType, BINDINGCLASS_PROPERTY, null, false);
			if (ts.hasNext()) {
				Value type = ts.next().getObject();
				if (type instanceof Literal) {
					className = type.stringValue();
				}
				else {
					throw new RDFBeanException("Value of " + BINDINGCLASS_PROPERTY.stringValue() + " property must be a literal");
				}
			}
		}
		finally {
			if (ts != null) {
				ts.close();
			}
		}
		
		if (className != null) {
			try {
				cls = Class.forName(className, true, classLoader);
				classCache.put(rdfType, cls);
				return cls;
			} catch (ClassNotFoundException ex) {
				throw new RDFBeanException("Class " + className
						+ " bound to RDF type <" + rdfType + "> is not found",
						ex);
			}
		}
		return null;
	}

	private <T> T _get(Resource r, Class<T> cls) throws RDFBeanException, RDF4JException {
		this.objectCache = new WeakHashMap<Resource, Object>();
		// Unmarshal the resource
		return unmarshal(r, cls);
	}

	private <T> T unmarshal(Resource resource, Class<T> cls)
			throws RDFBeanException, RDF4JException {
		// Check if the object is already retrieved
		T o = (T) objectCache.get(resource);
		if (o != null) {
			return o;
		}
		// Instantiate RDFBean
		try {
			o = cls.newInstance();
		} catch (Exception ex) {
			throw new RDFBeanException(ex);
		}
		this.objectCache.put(resource, o);
		// introspect RDFBean
		RDFBeanInfo rbi = RDFBeanInfo.get(cls);
		SubjectProperty subjectProperty = rbi.getSubjectProperty();
		if ((subjectProperty != null) && !(resource instanceof BNode)) {
			String id = resource.stringValue();
			subjectProperty.setValue(o, id);
		}
		for (RDFProperty p : rbi.getProperties()) {
			// Get values
			IRI predicate = p.getUri();
			CloseableIteration<Statement, ? extends RDF4JException> statements;
			if (p.isInversionOfProperty()) {				
				statements = conn.getStatements(null, predicate, resource, false);
				if (!statements.hasNext()) {
					// try a container
					GraphQuery q = conn.prepareGraphQuery(QueryLanguage.SPARQL, "CONSTRUCT { ?subject <" + p.getUri() + "> <" + resource + "> } " + 
							  "WHERE { ?subject <" + p.getUri() + "> ?container. " +
					  		  "?container ?li <" + resource + ">" +
					  		" }");
					statements = q.evaluate();
				}
			}
			else {
				statements = conn.getStatements(resource, predicate, null, false);				
			}

			//Collect all values
			List<Value> values = new ArrayList<>();
			try {
				while (statements.hasNext()) {
					values.add(p.isInversionOfProperty()? statements.next().getSubject() : statements.next().getObject());
				}
			}
			finally {
				statements.close();
			}
			
			if (values.isEmpty()) {				
				continue;
			}
			
			// Determine field type
			Class fClass = p.getPropertyType();
			if (Collection.class.isAssignableFrom(fClass) || fClass.isArray()) {
				// Collection property - collect all values
				// Check if an array or interface or abstract class
				if (fClass.isArray() || List.class.equals(fClass)
						|| AbstractList.class.equals(fClass)) {
					fClass = ArrayList.class;
				} else if (SortedSet.class.equals(fClass)) {
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
				// Collect values
				for (Value value: values) {
					Object object = unmarshalObject(value);
					if (object != null) {
						if (object instanceof Collection) {
							items.addAll((Collection) object);
						} else {
							items.add(object);
						}
					}
				}
				// Assign collection property
				p.setValue(o, items);
			} 
			else {
				// Not a collection - get the first value only
				Value value = values.iterator().next();
				Object object = unmarshalObject(value);
				if (object != null) {
					if ((object instanceof Collection)
							&& ((Collection) object).iterator().hasNext()) {
						object = ((Collection) object).iterator().next();
					}
					p.setValue(o, object);
				}				
			}
		}
		return o;
	}

	private Object unmarshalObject(Value object) throws RDFBeanException, RDF4JException {
		if (object instanceof Literal) {
			// literal
			return getDatatypeMapper().getJavaObject((Literal)object);
		} 
		else if (object instanceof BNode) {
			// Blank node - check if an RDF collection
			Resource r = (Resource) object;
			
			if (conn.hasStatement(r, RDF.TYPE, RDF.BAG, false) 
					|| conn.hasStatement(r, RDF.TYPE, RDF.SEQ, false)
					|| conn.hasStatement(r, RDF.TYPE, RDF.ALT, false)) {	
				// Collect all items (ordered)
				ArrayList items = new ArrayList();
				int i = 1;
				Object item;
				do {
					item = null;
					RepositoryResult<Statement> itemst = conn.getStatements(
							(Resource) object,
							conn.getValueFactory().createIRI(RDF.NAMESPACE, "_" + i),
							null, false);
					try {
						if (itemst.hasNext()) {
							item = unmarshalObject(itemst.next().getObject());
							if (item != null) {
								items.add(item);
							}
							i++;
						}
					} 
					finally {
						itemst.close();
					}
				} while (item != null);
				// Return collection
				return items;
			} else if (conn.hasStatement(r, RDF.FIRST, null, false)) {
				// Head-Tail list, also collect all items
				ArrayList<Object> items = new ArrayList<Object>();
				addList(items, r);
				return items;
			}
		}
		
		// Resource						
		Class<?> cls = null;
		try {
			cls = getBindingClass((Resource) object);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (cls != null) {
			return unmarshal((Resource) object, cls);
		}
		
		//URI ?
		return java.net.URI.create(object.stringValue());
	}

	private void addList(List<Object> list, final Resource currentHead) throws RDF4JException, RDFBeanException {
		// add the "first" items.
		RepositoryResult<Statement> firstStatements = conn.getStatements(
				currentHead,
				RDF.FIRST,
				null, false);
		while (firstStatements.hasNext()) {
			// multi-headed lists are possible, but flattened here.
			Object item = unmarshalObject(firstStatements.next().getObject());
			if (item != null) {
				list.add(item);
			}
		}
		firstStatements.close();

		// follow non-rdf:nil rest(s), if any.
		RepositoryResult<Statement> restStatements = conn.getStatements(
				currentHead,
				RDF.REST,
				null, false);
		while (restStatements.hasNext()) {
			Value nextHead = restStatements.next().getObject();
			if (!RDF.NIL.equals(nextHead)) {
				if (nextHead instanceof BNode) {
					addList(list, (BNode) nextHead);
				}
			}
		}
		restStatements.close();
	}

	// ================== RDFBean dynamic proxy functionality ==================

	/**
	 * Create new dynamic proxy instance that implements the specified RDFBean
	 * interface and backed by the specified RDF resource in the underlying
	 * RDF model.
	 * 
	 * @param r
	 *            Resource URI
	 * @param iface
	 *            RDFBean interface
	 * @return New RDFBean dynamic proxy object with the specified interface
	 * @throws RDFBeanException
	 *             If iface is not valid RDFBean interface
	 * @throws RepositoryException 
	 *             
	 * @see create(String,Class)
	 * @param <T>
	 */

	public <T> T create(Resource r, Class<T> iface) throws RDFBeanException, RepositoryException {
		return createInternal(r, RDFBeanInfo.get(iface), iface) ;
	}

	/**
	 * Create new dynamic proxy instance that implements the specified RDFBean
	 * interface and backed by an RDF resource matching to the given RDFBean ID.
	 * 
	 * <p>
	 * If a namespace prefix is defined in {@link RDFSubject} declaration for
	 * this RDFBean interface, the provided identifier value is interpreted as a
	 * local part of fully qualified RDFBean name (RDF resource URI). Otherwise,
	 * the fully qualified name must be provided.
	 * 
	 * @param id
	 *            RDFBean ID value
	 * @param iface
	 *            RDFBean interface
	 * @return New RDFBean dynamic proxy object with the specified interface
	 * @throws RDFBeanException
	 *             if iface is not valid RDFBean interface or there is an error
	 *             resolving RDFBean identifier
	 * @throws RepositoryException 
	 *             
	 * @see create(Resource,Class)
	 */

	public <T> T create(String id, Class<T> iface) throws RDFBeanException, RepositoryException {
		RDFBeanInfo rbi = RDFBeanInfo.get(iface);
		IRI uri = resolveUri(id, rbi);
		if (uri == null) {
			throw new RDFBeanException("Cannot resolve RDFBean ID: " + id);
		}
		return createInternal(uri, rbi, iface);
	}
	
	private <T> T createInternal(Resource r, RDFBeanInfo rbi, Class<T> iface) throws RDFBeanException, RepositoryException {
		boolean newObject = false;
		if (!isResourceExist(r)) {
			if (isAutocommit()) {
				conn.begin();
			}
			try {
				conn.add(r, RDF.TYPE, rbi.getRDFType());
				//conn.add(rbi.getRDFType(), RDFBeanManager.BINDINGIFACE_PROPERTY, conn.getValueFactory().createLiteral(rbi.getRDFBeanClass().getName()));
				if (isAutocommit()) {
					conn.commit();
				}
			}
			catch (RepositoryException e) {
				if (isAutocommit()) {
					conn.rollback();					
				}
				throw e;
			}
			newObject = true;
		}
		T obj = proxies.getInstance(r, rbi, iface);
		if (newObject) {
			fireObjectCreated(obj, iface, r);
		}
		return obj;
	}
	
	/**
	 * Returns a collection of dynamic proxy instances for existing RDF resources
	 * 
	 * @param iface
	 * 			RDFBean interface
	 * @return Collection of dynamic proxy objects with specified interface
	 * @throws RDFBeanException
	 * @throws RepositoryException 
	 */
	public <T> Collection<T> createAll(Class<T> iface) throws RDFBeanException, RepositoryException {
		RDFBeanInfo rbi = RDFBeanInfo.get(iface);
		IRI type = rbi.getRDFType();
		Collection<T> result = new HashSet<T>(); 
		if (type == null) {
			return result;
		}
		RepositoryResult<Statement> sts = null;
		try {
			sts = conn.getStatements(null, RDF.TYPE, type, false);
			while (sts.hasNext()) {
				T proxy = createInternal(sts.next().getSubject(), rbi, iface);
				result.add(proxy);
			}
		}
		finally {
			if (sts != null) {
				sts.close();
			}
		}		
		return result;
	}

	private IRI resolveUri(String id, RDFBeanInfo rbi) throws RDFBeanException {
		try {
			if (new java.net.URI(id).isAbsolute()) {
				return conn.getValueFactory().createIRI(id);
			}
			else {
				SubjectProperty sp = rbi.getSubjectProperty();
				if (sp != null) {
					return sp.getUri(id);
				}
			}
		}
		catch (URISyntaxException e) {
			throw new RDFBeanException("Invalid URI syntax: " + id, e);
		}
		return null;
	}

	// ============================ Common methods =============================
	

	/**
	 * Check if autocommit mode is on
	 * 
	 * <p>
	 * If autocommit mode is on, the transactions with the RDF model will be 
	 * immediately commited on invocation of {@link add(Object)}, {@link update(Object)} and 
	 * {@link delete(Resource)} methods, as well as of the setter methods of the
	 * dynamic proxy objects. Otherwise, the transactions must be commited by explicit
	 * invocation of the <code>commit()</code> method of the Model implementation.
	 * 
	 * <p>
	 * By default, the autocommit mode is on.
	 * 
	 * @return True if autocommit mode is on.
	 * 
	 * @see setAutocommit(boolean)
	 */
	public boolean isAutocommit() {
		return autocommit;
	}

	/**
	 * Set autocommit mode.
	 * 
	 * <p>
	 * If autocommit mode is on, the transactions with the RDF model will be 
	 * immediately commited on invocation of {@link add(Object)}, {@link update(Object)} and 
	 * {@link delete(Resource)} methods, as well as of the setter methods of the
	 * dynamic proxy objects. Otherwise, the transactions must be commited by explicit
	 * invocation of the <code>commit()</code> method of the Model implementation.
	 * 
	 * <p>
	 * By default, the autocommit mode is on.
	 *  
	 * @param autocommit
	 *            false to set the autocommit mode off or true to on
	 *            
	 * @see isAutocommit()
	 */
	public void setAutocommit(boolean autocommit) {
		this.autocommit = autocommit;
	}

	/**
	 * Return the current ClassLoader for loading RDFBean classes.
	 * 
	 * <p>
	 * By default, the classes are loaded by the ClassLoader of this RDFBeanManager.  
	 * 
	 * @return the current ClassLoader instance
	 * 
	 * @see setClassLoader(ClassLoader)
	 */
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	/**
	 * Set a custom ClassLoader instance for loading RDFBean classes.
	 * 
	 * <p>
	 * By default, the classes are loaded by the ClassLoader of this RDFBeanManager.
	 *   
	 * @param classLoader
	 *            the ClassLoader instance to set
	 *            
	 * @see getClassLoader()           
	 */
	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Return a current DatatypeMapper implementation.
	 * 
	 * @return the datatypeMapper
	 * 
	 * @see setDatatypeMapper(DatatypeMapper)
	 */
	public DatatypeMapper getDatatypeMapper() {
		return datatypeMapper;
	}

	/**
	 * Set a DatatypeMapper implementation.
	 * 
	 * @param datatypeMapper
	 *            the datatypeMapper to set
	 * 
	 * @see getDatatypeMapper()
	 */
	public void setDatatypeMapper(DatatypeMapper datatypeMapper) {
		this.datatypeMapper = datatypeMapper;
	}
	
	public void addProxyListener(ProxyListener l) {
		this.proxyListeners.add(l);
	}
	
	public void removeProxyListener(ProxyListener l) {
		this.proxyListeners.remove(l);
	}
	
	public List<ProxyListener> getProxyListeners() {
		return Collections.unmodifiableList(proxyListeners);
	}
	
	private void fireObjectCreated(Object object, Class<?> cls, Resource resource) {
		for (ProxyListener l : getProxyListeners()) {
			l.objectCreated(object, cls, resource);
		}
	}

}
