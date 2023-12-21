package org.cyberborean.rdfbeans;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.ReadWriteLock;

import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.annotations.RDFSubject;
import org.cyberborean.rdfbeans.datatype.DatatypeMapper;
import org.cyberborean.rdfbeans.exceptions.RDFBeanException;
import org.cyberborean.rdfbeans.exceptions.RDFBeanValidationException;
import org.cyberborean.rdfbeans.impl.Marshaller;
import org.cyberborean.rdfbeans.impl.RepositoryConnectionPool;
import org.cyberborean.rdfbeans.impl.Unmarshaller;
import org.cyberborean.rdfbeans.proxy.ProxyInstancesPool;
import org.cyberborean.rdfbeans.proxy.ProxyListener;
import org.cyberborean.rdfbeans.reflect.RDFBeanInfo;
import org.cyberborean.rdfbeans.reflect.SubjectProperty;
import org.cyberborean.rdfbeans.util.LockKeeper;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;

public class RDFBeanManagerContext {
	
	private List<ProxyListener> proxyListeners = new Vector<ProxyListener>();

	private final IRI context;
	protected RepositoryConnectionPool connectionPool;
	protected LockKeeper lockKeeper;
	private ProxyInstancesPool proxies;

	protected Marshaller marshaller;
	protected Unmarshaller unmarshaller;

	RDFBeanManagerContext(IRI context) {
		this.context = context;
		this.proxies = new ProxyInstancesPool(this);
	}

	public RDFBeanManagerContext(IRI iri, RDFBeanManagerContext parent) {
		this(iri);
		this.connectionPool = parent.connectionPool;
		this.lockKeeper = parent.lockKeeper;		
		this.marshaller = parent.marshaller;
		this.unmarshaller = parent.unmarshaller;
	}

	/**
	 * Exposes a connection to RDF4J Repository for the current thread. If there
	 * is no opened connection for this thread,
	 * it will be created.
	 * 
	 * @return RDF4J RepositoryConnection object
	 */
	public RepositoryConnection getRepositoryConnection() {
		return connectionPool.getConnection();
	}
	
	
	/**
	 * Stores the state of a Java object as a set of
	 * triple statements in the underlying RDF model.
	 * 
	 * The class of the object must conform to the RDFBean specification.
	 * 
	 * If the object has a not-null property, annotated with {@link RDFSubject},
	 * the method returns IRI of newly created RDF resource. Otherwise (the
	 * RDFBean is anonymous), a BNode object is returned.
	 * 
	 * If an RDF representation of the given unanonymous object already exists
	 * in the current context, the method does not perform any modifications.
	 * 
	 * Upon storing a first instance of every Java class, the method adds
	 * special statement to the model containing information about binding of
	 * specific RDF type to that class. This information is needed to
	 * determine which class to use for instatiation of restored objects
	 * later.
	 * 
	 * If there is an active transaction started on RepositoryConnection
	 * for the current thread, updates of all individual triples are added to
	 * that transaction.
	 * This means that the updates are not effective until the active
	 * transaction is committed.
	 * If no active transaction exists, this method will commit new transaction
	 * for all
	 * individual triple updates.
	 * 
	 * @param o
	 *            RDFBean object to add
	 * @return Resource IRI (or BNode for anonymous RDFBean)
	 * @throws RDFBeanException
	 *             If class of the object is not a valid RDFBean class
	 * @throws RepositoryException
	 * 
	 */
	public Resource add(Object o) throws RDFBeanException, RepositoryException {
		return addOrUpdate(o, false);
	}

	/**
	 * Updates RDF representation of a Java object previously stored in the
	 * underlying RDF model.
	 * 
	 * The class of the object must conform the RDFBean specification.
	 * 
	 * If no RDF representation for the given object is found in the current
	 * context,
	 * or if the object is an anonymous RDFBean, the method behaves like
	 * {@link #add(Object) add()}.
	 *
	 * If there is an active transaction started on RepositoryConnection
	 * for the current thread, updates of all individual triples are added to
	 * that transaction.
	 * This means that the updates are not effective until the active
	 * transaction is committed.
	 * If no active transaction exists, this method will commit new transaction
	 * for all
	 * individual triple updates.
	 * 
	 * @param o
	 *            RDFBean to update
	 * @return Resource IRI (or BNode for anonymous RDFBean)
	 * @throws RDFBeanException
	 *             If class of the object is not a valid RDFBean class
	 * @throws RepositoryException
	 */
	public synchronized Resource update(Object o) throws RDFBeanException, RepositoryException {
		return addOrUpdate(o, true);
	}

	/**
	 * Restores the state of a Java object from an RDF representation in the
	 * underlying RDF model.
	 * 
	 * The class must conform to the RDFBean specification.
	 * 
	 * @param r
	 *            Resource IRI (or BNode for anonymous RDFBean).
	 * @param rdfBeanClass
	 *            Java class of RDFBean
	 * @return Unmarshalled Java object, or null if the resource does not
	 *         exist in the current context
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean class or an instance of
	 *             this
	 *             class cannot be created
	 * @throws RDF4JException
	 */
	public <T> T get(Resource r, Class<T> rdfBeanClass) throws RDFBeanException, RDF4JException {
		if (!isResourceExist(r)) {
			return null;
		}
		return _get(r, rdfBeanClass);
	}

	/**
	 * Restores the state of a Java object from an RDF representation in the
	 * underlying RDF model.
	 * 
	 * The method tries to determine a Java class of the reconstructed object
	 * using binding class
	 * information previously added to the model. If the binding class
	 * information is
	 * not found, RDFBeanException is thrown.
	 * 
	 * @param r
	 *            Resource IRI (or BNode for anonymous RDFBean).
	 * @return Unmarshalled Java object, or null if the resource does not exist
	 * @throws RDFBeanException
	 *             If the binding class cannot be detected, or it is not a valid
	 *             RDFBean class or an instance of this class cannot be created
	 * @throws RDF4JException
	 */
	public Object get(Resource r) throws RDFBeanException, RDF4JException {
		return get(r, null);
	}

	/**
	 * Restores the state of a Java object from an RDF representation in the
	 * underlying RDF model.
	 * 
	 * The class must conform to the RDFBean specification.
	 * 
	 * If a namespace prefix is defined in {@link RDFSubject} declaration for
	 * this RDFBean class, the provided identifier value is interpreted as a
	 * local part of fully qualified RDFBean name (RDF resource IRI). Otherwise,
	 * a fully qualified name is expected.
	 * 
	 * @param stringId
	 *            RDFBean ID value
	 * @param rdfBeanClass
	 *            Java class of RDFBean
	 * @return The unmarshalled Java object, or null if the resource matching
	 *         the given ID does not exist
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean class or an instance of
	 *             this class cannot be created
	 * @throws RDF4JException
	 * 
	 */
	public <T> T get(String stringId, Class<T> rdfBeanClass) throws RDFBeanException, RDF4JException {
		Resource r = getResource(stringId, rdfBeanClass);
		if (r != null) {
			return get(r, rdfBeanClass);
		}
		return null;
	}

	/**
	 * Returns an iterator over all objects of the specified Java class stored
	 * in the underlying RDF model.
	 * 
	 * The class must conform to the RDFBean specification.
	 * 
	 * The returned Iterator performs "lazy" restoring of Java objects (on
	 * every `next()` call) with no specific order. When iterator is exhausted,
	 * the caller must invoke
	 * `close()` method to release the resources of underlying RDF model.
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
				public void close() throws Exception {
				}

			};
		}

		final CloseableIteration<Statement, RepositoryException> sts = connectionPool.getConnection()
				.getStatements(null, RDF.TYPE, type, false, (IRI)context);

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
	 * Checks if an RDF resource exists in the underlying
	 * RDF model.
	 * 
	 * @param r
	 *            Resource IRI or BNode
	 * @return true, if the model contains at least one statement with the given
	 *         resource subject.
	 * @throws RepositoryException
	 */
	public boolean isResourceExist(Resource r) throws RepositoryException {
		return hasStatement(r, null, null);
	}

	/**
	 * Checks if an RDF resource exists in the underlying
	 * RDF model and represents an object of the specified Java class.
	 * 
	 * The class must conform to the RDFBean specification.
	 * 
	 * @param r
	 *            Resource IRI or BNode
	 * @param context
	 *            RDF4J context
	 * @return true, if the model contains the statements with the given
	 *         resource subject and RDF type of that resource matches one
	 *         specified in {@link RDFBean} annotation of the given class.
	 * @throws RDFBeanValidationException
	 *             If the class is not a valid RDFBean class
	 * @throws RepositoryException
	 */
	public boolean isResourceExist(Resource r, Class rdfBeanClass)
			throws RDFBeanValidationException, RepositoryException {
		RDFBeanInfo rbi = RDFBeanInfo.get(rdfBeanClass);
		return hasStatement(r, RDF.TYPE, rbi.getRDFType());
	}

	private boolean hasStatement(Resource s, IRI p, Value o) throws RepositoryException {
		ReadWriteLock lock = lockKeeper.getLock(s);
		lock.readLock().lock();
		try {
			return connectionPool.getConnection().hasStatement(s, p, o, false, context);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Returns an RDF resource representing an object that matches the specified
	 * RDFBean identifier and Java class.
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
	public Resource getResource(String stringId, Class rdfBeanClass) throws RDFBeanException, RepositoryException {
		SubjectProperty subject = RDFBeanInfo.get(rdfBeanClass).getSubjectProperty();
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
	 * It results in deletion of all statements where the given resource is
	 * either a subject or an object.
	 * 
	 * If there is an active transaction started on RepositoryConnection
	 * for the current thread, all individual triple removals are added to
	 * that transaction.
	 * This means that the updates are not effective until the active
	 * transaction is committed.
	 * If no active transaction exists, this method will commit new transaction
	 * for all
	 * individual triple removals.
	 * 
	 * @param uri
	 *            Resource IRI
	 * @return true if the resource existed in the model before deletion, false
	 *         otherwise
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
		RepositoryConnection conn = connectionPool.getConnection();
		boolean newTxn = maybeStartTransaction(conn);
		ReadWriteLock lock = lockKeeper.getLock(uri);
		lock.writeLock().lock();
		try {
			// delete where is a subject
			conn.remove(uri, null, null, (IRI)context);
			// delete where is an object
			conn.remove((Resource) null, null, uri, (IRI)context);
			proxies.purge(uri);
			if (newTxn) {
				conn.commit();
			}
		} catch (RepositoryException e) {
			if (newTxn) {
				conn.rollback();
			}
			throw e;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Deletes an RDF resource representing an object that matches the specified
	 * RDFBean identifier and the Java class from the underlying model.
	 * 
	 * The class must conform to the RDFBean specification.
	 * 
	 * If the RDF resource is found, it results in deletion of all statements
	 * where it is either a subject or an object.
	 * 
	 * If a namespace prefix is defined in {@link RDFSubject} declaration for
	 * this RDFBean class, the provided identifier value is interpreted as a
	 * local part of fully qualified RDFBean name (RDF resource IRI). Otherwise,
	 * the fully qualified name is expected.
	 * 
	 * If there is an active transaction started on RepositoryConnection
	 * for the current thread, all individual triple removals are added to
	 * that transaction.
	 * This means that the updates are not effective until the active
	 * transaction is committed.
	 * If no active transaction exists, this method will commit new transaction
	 * for all individual triple removals.
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
	public void delete(String stringId, Class rdfBeanClass) throws RDFBeanException, RepositoryException {
		Resource r = this.getResource(stringId, rdfBeanClass);
		if (r != null) {
			this.delete(r);
		}
	}

	private Resource addOrUpdate(Object o, boolean update)
			throws RDFBeanException, RepositoryException {
		RepositoryConnection conn = connectionPool.getConnection();
		boolean newTxn = maybeStartTransaction(conn);
		Resource node;
		try {
			node = marshaller.marshal(conn, o, update, context);
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

	private <T> T _get(Resource r, Class<T> cls) throws RDFBeanException, RDF4JException {
		if (isResourceExist(r)) {
			// Unmarshal the resource
			return unmarshaller.unmarshal(connectionPool.getConnection(), r, cls, context);
		}
		return null;
	}

	// ================== RDFBean dynamic proxy functionality ==================

	/**
	 * Creates new dynamic proxy object implementing the specified Java
	 * interface. The specified RDF resource will represent the object in the
	 * underlying
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
		return createInternal(connectionPool.getConnection(), r, RDFBeanInfo.get(iface), iface);
	}

	/**
	 * Creates new dynamic proxy object implementing the specified Java
	 * interface. An RDF resource matching the specified RDFBean identifier will
	 * represent the object in the underlying
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
	 *             if iface is not valid RDFBean interface or the RDFBean
	 *             identifier cannot be resolved to a resource
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
		return createInternal(connectionPool.getConnection(), uri, rbi, iface);
	}

	private <T> T createInternal(RepositoryConnection conn, Resource r, RDFBeanInfo rbi, Class<T> iface) throws RDFBeanException, RepositoryException {
		boolean newObject = false;
		if (!isResourceExist(r)) {
			boolean newTxn = maybeStartTransaction(conn);
			try {
				conn.add(r, RDF.TYPE, rbi.getRDFType(), (IRI)context);
				addSuperInterfaceTypes(conn, rbi);
				if (newTxn) {
					conn.commit();
				}
			} catch (RepositoryException e) {
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

	private void addSuperInterfaceTypes(RepositoryConnection conn, RDFBeanInfo rbi)
			throws RDFBeanValidationException {
		for (Class<?> superIface : rbi.getRDFBeanClass().getInterfaces()) {
			if (RDFBeanInfo.isRdfBeanClass(superIface)) {
				RDFBeanInfo superRbi = RDFBeanInfo.get(superIface);
				if (superRbi != null) {
					conn.add(rbi.getRDFType(), RDFS.SUBCLASSOF, superRbi.getRDFType(), (IRI)context);
					addSuperInterfaceTypes(conn, superRbi);
				}
			}
		}

	}

	/**
	 * Constructs all dynamic proxy objects implementing the specified Java
	 * interface from their representations in the underlying RDF model.
	 * 
	 * The interface must conform to the RDFBean specification.
	 * 
	 * @param iface
	 *            RDFBean-compliant Java interface
	 * @return Collection of dynamic proxy objects with the specified interface
	 * @throws RDFBeanException
	 *             If iface is not a valid RDFBean interface
	 * @throws RepositoryException
	 */
	public <T> Collection<T> createAll(Class<T> iface)
			throws RDFBeanException, RepositoryException {
		RDFBeanInfo rbi = RDFBeanInfo.get(iface);
		IRI type = rbi.getRDFType();
		Collection<T> result = new HashSet<T>();
		if (type == null) {
			return result;
		}
		RepositoryConnection conn = connectionPool.getConnection();
		RepositoryResult<Statement> sts = null;
		try {
			sts = conn.getStatements(null, RDF.TYPE, type, false, (IRI)context);
			while (sts.hasNext()) {
				T proxy = createInternal(conn, sts.next().getSubject(), rbi, iface);
				result.add(proxy);
			}
		} finally {
			if (sts != null) {
				sts.close();
			}
		}
		return result;
	}

	private IRI resolveUri(String id, RDFBeanInfo rbi) throws RDFBeanException {
		try {
			if (new java.net.URI(id).isAbsolute()) {
				return SimpleValueFactory.getInstance().createIRI(id);
			} else {
				SubjectProperty sp = rbi.getSubjectProperty();
				if (sp != null) {
					return sp.getUri(id);
				}
			}
		} catch (URISyntaxException e) {
			throw new RDFBeanException("Invalid URI syntax: " + id, e);
		}
		return null;
	}

	private boolean maybeStartTransaction(RepositoryConnection conn) {
		boolean newTxn = !conn.isActive();
		if (newTxn) {
			conn.begin();
		}
		return newTxn;
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
	

	/**
	 * Returns the current ClassLoader for loading RDFBean classes.
	 * 
	 * By default, the classes are loaded by the ClassLoader of this
	 * RDFBeanManager.
	 * 
	 * @return the current ClassLoader instance
	 * 
	 * @see setClassLoader(ClassLoader)
	 */
	public ClassLoader getClassLoader() {
		return unmarshaller.getClassLoader();
	}

	/**
	 * Returns a current DatatypeMapper implementation.
	 * 
	 * @return the datatypeMapper
	 * 
	 * @see setDatatypeMapper(DatatypeMapper)
	 */
	public DatatypeMapper getDatatypeMapper() {
		return marshaller.getDatatypeMapper();
	}

	public IRI getContext() {
		return context;
	}

}
