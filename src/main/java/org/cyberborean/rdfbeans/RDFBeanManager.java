
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

import org.cyberborean.rdfbeans.annotations.RDFBean;
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
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
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

/**
 * Provides basic CRUD and dynamic proxy management functions for persisting RDFBean data objects using a RDF model stored in RDF4J repository.
 * 
 * RDFBeanManager is instantiated with opened RDF4J 
 * [RepositoryConnection](http://docs.rdf4j.org/javadoc/latest?org/eclipse/rdf4j/repository/RepositoryConnection.html) to an existing 
 * [Repository](http://docs.rdf4j.org/javadoc/latest?org/eclipse/rdf4j/repository/Repository.html) object. The client code is responsible
 * for opening and closing the repository connection, as well as initializing and shutting down the repository.
 * 
 * Example of usage:
 * 
 * ```java
 * Repository repo;
 * try {
 *     File dataDir = new File("C:\\temp\\myRepository\\");      
 *     repo = new SailRepository(new NativeStore(dataDir));
 *     repo.initialize();
 *     try (RepositoryConnection con = repo.getConnection()) {
 *         RDFBeanManager rdfBeanManager = new RDFBeanManager(con);          
 *         //... do something ...
 *     }
 * finally {
 *     repo.shutDown();   
 * }
 * ```
 * 
 * ### Multi-threading
 * 
 * This class, as well as RDF4J RepositoryConnection is **not thread-safe**. It is recommended that each thread obtain it's own 
 * RepositoryConnection from a shared Repository object and create a separate RDFBeanManager instance on it.
 * 
 * ### Transactions
 * 
 * By default, RDFBeanManager methods add or delete individual RDF statements in a transaction-safe manner. 
 * A method starts new transaction on RepositoryConnection before any update and commit it automatically after updates are completed. 
 * If the method throws an exception, the entire transaction is rolled back that guarantees that all updates the method made to this point 
 * will not take effect.
 * 
 * The behaviour is different if the method is invoked when RepositoryConnection already has an active transaction. 
 * In this case, the method does not start new transaction, but re-uses existing one by adding new operations to it. The updates will not take
 * effect until the transaction is committed. If an exception is thrown by the method, the transaction status is not changed (the client code is free
 * to roll it back on it's own).   
 * 
 * With this explicit transaction management, one can group multiple RDFBeanManager operations 
 * and treat them as a single update, as shown in the below example:
 *  
 * ```java
 * RepositoryConnection con = rdfBeanManager.getRepositoryConnection();
 * // start a transaction
 * con.begin();
 * try {
 *     // Add few RDFBean objects
 *     rdfBeanManager.add(object1);
 *     rdfBeanManager.add(object2);
 *     rdfBeanManager.add(object3);
 *     // Commit the above adds at once
 *     con.commit();
 * }
 * catch (Throwable t) {
 *     // Something went wrong, we roll the transaction back
 *     con.rollback();
 *     throw t;
 * }
 * ``` 
 * 
 */
public class RDFBeanManager {

	public static final ValueFactory valueFactory = SimpleValueFactory.getInstance();
	public static final IRI BINDINGCLASS_PROPERTY = valueFactory.createIRI(
			"http://viceversatech.com/rdfbeans/2.0/bindingClass");
	public static final IRI BINDINGIFACE_PROPERTY = valueFactory.createIRI(
			"http://viceversatech.com/rdfbeans/2.0/bindingIface");

	private RepositoryConnection conn;
	private ClassLoader classLoader;
	private DatatypeMapper datatypeMapper = new DefaultDatatypeMapper();

	private WeakHashMap<Object, Resource> resourceCache;
	private WeakHashMap<Resource, Object> objectCache;
	private Map<IRI, Class> classCache;
	private ProxyInstancesPool proxies;
	private List<ProxyListener> proxyListeners = new Vector<ProxyListener>();

	/**
	 * Creates new RDFBeanManager instance upon the given RDF4J RepositoryConnection.
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

	// ====================== RDFBeans CRUD functionality ====================

	/**
	 * Marshalls the state of a Java object to an RDF resource (a set of
	 * triple statements in the underlying RDF model).
	 * 
	 * The class of the object must conform to the RDFBean specification.
	 * 
	 * If the object has a not-null property, annotated with {@link RDFSubject},
	 * the method returns IRI of newly created RDF resource. Otherwise (the
	 * RDFBean is anonymous), a BNode object is returned.
	 * 
	 * If an RDF representation of the given unanonymous object already exists
	 * in the model, the method immediately returns IRI without any model
	 * modifications.
	 * 
	 * Upon marshalling a first instance of every Java class, the method adds
	 * special statement to the model containing information about binding of 
	 * specific RDF type to that class. This information is needed to
	 * determine which class to use for instatiation of the unmarshalled objects later. 
	 * @see get(Resource) 
	 *
	 * If there is an active transaction started on RepositoryConnection, all
	 * individual updates performed by this method are added to that
	 * transaction. This means that the updates are not effective until the
	 * transaction is committed. Otherwise, this method will start new
	 * transaction under the hood to commit individual triple updates at once.
	 * 
	 * @param o
	 *            RDFBean to add
	 * @return Resource IRI (or BNode for anonymous RDFBean)
	 * @throws RDFBeanException
	 *             If class of the object is not a valid RDFBean class
	 * @throws RepositoryException
	 * 
	 * @see update(Object)
	 */
	public Resource add(Object o) throws RDFBeanException, RepositoryException {
		return addOrUpdate(o, false);
	}

	/**
	 * Marshalls the state of a Java RDFBean object to an RDF resource (a set of
	 * triple statements in the underlying RDF model). If existing RDF representation of this
	 * object is found in the model, it will be overwritten.
	 * 
	 * The class of the object must conform the RDFBean specification.
	 * 
	 * If no RDF representation for the given object is found, or if the object is an anonymous
	 * RDFBean, the method works exactly like {@link #add(Object) add()}.
	 *
	 * If there is an active transaction started on RepositoryConnection, all individual 
	 * updates performed by this method are added to that transaction. That means that the updates
	 * are not effective until the transaction is committed. Otherwise, this method will start new
	 * transaction under the hood to commit individual triple updates at once.     
	 * 
	 * @param o
	 *            RDFBean to update
	 * @return Resource IRI (or BNode for anonymous RDFBean)
	 * @throws RDFBeanException
	 *             If class of the object is not a valid RDFBean class
	 * @throws RepositoryException 
	 * 
	 * @see add(Object)
	 */
	public synchronized Resource update(Object o) throws RDFBeanException, RepositoryException {
		return addOrUpdate(o, true);
	}

	/**
	 * Unmarshalls an RDF resource by creating an object of the specified Java class.
	 * 
	 * The class must conform to the RDFBean specification.
	 * 
	 * @param r
	 *            Resource IRI (or BNode for anonymous RDFBean).
	 * @param rdfBeanClass
	 *            Java class of RDFBean
	 * @return Unmarshalled Java object, or null if the resource does not
	 *         exist
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean class or an instance of this
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
	 * Unmarshalls an RDF resource by creating an object of auto-detected Java class.
	 * 
	 * The method tries to determine a Java class using binding class information
	 * added to the model at marshalling. If the binding class information is not
	 * found, RDFBeanException is thrown.
	 * 
	 * @param r
	 *            Resource IRI (or BNode for anonymous RDFBean).
	 * @return Unmarshalled Java object, or null if the resource does not
	 *         exist 
	 * @throws RDFBeanException
	 *             If the binding class cannot be detected, or it is not a valid
	 *             RDFBean class or an instance of this class cannot be created
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
	 * Unmarshalls an RDF resource matching the specified RDFBean identifier 
	 * by creating an object of the specified Java class.
	 * 
	 * The class must conform to the RDFBean specification.
	 * 
	 * If a namespace prefix is defined in {@link RDFSubject} declaration for
	 * this RDFBean class, the provided identifier value is interpreted as a
	 * local part of fully qualified RDFBean name (RDF resource IRI). Otherwise,
	 * the fully qualified name is expected.
	 * 
	 * @param stringId
	 *            RDFBean ID value
	 * @param rdfBeanClass
	 *            Java class of RDFBean
	 * @return The unmarshalled Java object, or null if the resource matching the
	 *         given ID does not exist
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean class or an instance of this
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
	 * Returns an iterator over all objects of the specified Java class stored
	 * in the RDF model.
	 * 
	 * The class must conform to the RDFBean specification.
	 * 
	 * The returned Iterator performs "lazy" unmarshalling of objects (on
	 * every `next()` call) with no specific order. When
	 * iterating is done, the caller must invoke the `close()` method
	 * to release the resources of underlying RDF model.
	 * 
	 * @param rdfBeanClass
	 *            Java class of objects to iterate
	 * @return Iterator over instances of the specified Java class
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean class
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
	 * Checks whether an RDF resource exists in the underlying model.
	 * 
	 * @param r
	 *            Resource IRI or BNode
	 * @return true, if the model contains the statements with the given
	 *         resource subject.
	 * @throws RepositoryException 
	 */
	public boolean isResourceExist(Resource r) throws RepositoryException {
		return hasStatement(r, null, null);
	}
	
	/**
	 * Checks whether an RDF resource exists in the underlying model and
	 * represents an object of the specified Java class.
	 * 
	 * The class must conform to the RDFBean specification.
	 * 
	 * @param r
	 *            Resource IRI or BNode
	 * @return true, if the model contains the statements with the given
	 *         resource subject and RDF type of that resource matches one
	 *         specified in {@link RDFBean} annotation of the given class. 
	 * @throws RDFBeanValidationException
	 * 			If the class is not a valid RDFBean class
	 * @throws RepositoryException 
	 */
	public boolean isResourceExist(Resource r, Class rdfBeanClass) throws RDFBeanValidationException, RepositoryException {
		RDFBeanInfo rbi = RDFBeanInfo.get(rdfBeanClass);
		return hasStatement(r, RDF.TYPE, rbi.getRDFType());
	}
	

	private boolean hasStatement(Resource s, IRI p, Value o) throws RepositoryException {
		return conn.hasStatement(s, p, o, false);
	}

	/**
	 * Returns an RDF resource representing an object that matches the specified RDFBean identifier and Java class.
	 * 
	 * The class must conform to the RDFBean specification.
	 * 
	 * If a namespace prefix is defined in {@link RDFSubject} declaration for
	 * this RDFBean class, the provided identifier value is interpreted as a
	 * local part of fully qualified RDFBean name (RDF resource IRI). Otherwise,
	 * the fully qualified name is expected.
	 * 
	 * @param stringId
	 *            RDFBean ID value
	 * @param rdfBeanClass
	 *            Java class of RDFBean
	 * @return Resource IRI, or null if no resource matching the given RDFBean
	 *         ID found.
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean class
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
	 * Deletes the RDF resource from the underlying model.
	 * 
	 * It results in deletion of all statements where the given resource is either a subject or an object.
	 * 
	 * If there is an active transaction started on RepositoryConnection, all
	 * individual updates performed by this method are added to that
	 * transaction. That means that the updates are not effective until the
	 * transaction is committed. Otherwise, this method will start new
	 * transaction under the hood to commit individual triple updates at once.
	 * 
	 * @param uri Resource IRI
	 * @return true if the resource existed in the model before deletion, false otherwise
	 * @throws RepositoryException 
	 * @see delete(String,Class)
	 */
	public boolean delete(Resource uri) throws RepositoryException {
		if (isResourceExist(uri)) {
			deleteInternal(uri);
			return true;
		}
		return false;
	}
	
	private synchronized void deleteInternal(Resource uri) throws RepositoryException {
		boolean newTxn = maybeStartTransaction();
		try {
			// delete where is a subject
			conn.remove(uri, null, null);
			// delete where is an object
			conn.remove((Resource)null, null, uri);
			proxies.purge(uri);
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

	/**
	 * Deletes an RDF resource representing an object that matches the specified RDFBean identifier and Java class
	 * from the underlying model.
	 * 
	 * The class must conform to the RDFBean specification.
	 * 
	 * If the RDF resource is found, it results in deletion of all statements where it is either a subject or an object.
	 * 
	 * If a namespace prefix is defined in {@link RDFSubject} declaration for
	 * this RDFBean class, the provided identifier value is interpreted as a
	 * local part of fully qualified RDFBean name (RDF resource IRI). Otherwise,
	 * the fully qualified name is expected.
	 * 
	 * If there is an active transaction started on RepositoryConnection, all
	 * individual updates performed by this method are added to that
	 * transaction. That means that the updates are not effective until the
	 * transaction is committed. Otherwise, this method will start new
	 * transaction under the hood to commit individual triple updates at once.
	 * 
	 * @param stringId
	 *            RDFBean ID value
	 * @param rdfBeanClass
	 *            Java class of RDFBean
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean class
	 * @throws RepositoryException 
	 * 
	 * @see delete(Resource)
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
		boolean newTxn = maybeStartTransaction();
		Resource node;
		try {
			node = marshal(o, update);
			if (newTxn) {
				conn.commit();
			}
		} catch (RDFBeanException | RepositoryException e) {
			if (newTxn) {
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
	 * Creates new dynamic proxy object implementing the specified Java
	 * interface. The specified RDF resource will represent the object in the underlying
	 * RDF model.
	 * 
	 * The interface must conform to the RDFBean specification.
	 * 
	 * @param r
	 *            Resource IRI
	 * @param iface
	 *            RDFBean-compliant Java interface
	 * @return New dynamic proxy object with the specified interface
	 * @throws RDFBeanException
	 *             If iface is not a valid RDFBean interface
	 * @throws RepositoryException 
	 *             
	 * @see create(String,Class)
	 * @param <T>
	 */
	public <T> T create(Resource r, Class<T> iface) throws RDFBeanException, RepositoryException {
		return createInternal(r, RDFBeanInfo.get(iface), iface) ;
	}

	/**
	 * Creates new dynamic proxy object implementing the specified Java
	 * interface. An RDF resource matching the specified RDFBean identifier will represent the object in the underlying
	 * RDF model.
	 * 
	 * The interface must conform to the RDFBean specification.
	 * 
	 * If a namespace prefix is defined in {@link RDFSubject} declaration for
	 * this RDFBean interface, the provided identifier value is interpreted as a
	 * local part of fully qualified RDFBean name (RDF resource IRI). Otherwise,
	 * the fully qualified name is expected.
	 * 
	 * @param id
	 *            RDFBean ID value
	 * @param iface
	 *            RDFBean-compliant Java interface
	 * @return New dynamic proxy object with the specified interface
	 * @throws RDFBeanException
	 *             if iface is not valid RDFBean interface or the RDFBean identifier cannot be resolved to a resource
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
			boolean newTxn = maybeStartTransaction();
			try {
				conn.add(r, RDF.TYPE, rbi.getRDFType());
				//conn.add(rbi.getRDFType(), RDFBeanManager.BINDINGIFACE_PROPERTY, conn.getValueFactory().createLiteral(rbi.getRDFBeanClass().getName()));
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
			newObject = true;
		}
		T obj = proxies.getInstance(r, rbi, iface);
		if (newObject) {
			fireObjectCreated(obj, iface, r);
		}
		return obj;
	}
	
	/**
	 * Constructs all dynamic proxy objects implementing the specified Java
	 * interface from their representations in the underlying RDF model. 
	 * 
	 * The interface must conform to the RDFBean specification.
	 * 
	 * @param iface
	 * 			RDFBean-compliant Java interface
	 * @return Collection of dynamic proxy objects with the specified interface
	 * @throws RDFBeanException
	 * 			If iface is not a valid RDFBean interface
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
	
	private boolean maybeStartTransaction() {
		boolean newTxn = !conn.isActive();
		if (newTxn) {
			conn.begin();
		}
		return newTxn;
	}

	/**
	 * Returns the current ClassLoader for loading RDFBean classes.
	 * 
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
	 * Sets a custom ClassLoader instance for loading RDFBean classes.
	 * 
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
	 * Returns a current DatatypeMapper implementation.
	 * 
	 * @return the datatypeMapper
	 * 
	 * @see setDatatypeMapper(DatatypeMapper)
	 */
	public DatatypeMapper getDatatypeMapper() {
		return datatypeMapper;
	}

	/**
	 * Sets a DatatypeMapper implementation.
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
