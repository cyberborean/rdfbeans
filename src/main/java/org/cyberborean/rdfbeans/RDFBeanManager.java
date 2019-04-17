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
import org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper;
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
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;

/**
 * Provides basic CRUD and dynamic proxy management functions for persisting
 * RDFBean data objects using a RDF model stored in RDF4J repository.
 * 
 */
public class RDFBeanManager implements AutoCloseable {

	private RepositoryConnectionPool connectionPool;

	private LockKeeper lockKeeper = new LockKeeper();
	private Marshaller marshaller = new Marshaller(lockKeeper, new DefaultDatatypeMapper());
	private Unmarshaller unmarshaller = new Unmarshaller(lockKeeper, new DefaultDatatypeMapper(),
			this.getClass().getClassLoader());

	private ProxyInstancesPool proxies;
	private List<ProxyListener> proxyListeners = new Vector<ProxyListener>();

	/**
	 * Creates new RDFBeanManager instance backed by the given RDF4J Repository.
	 * 
	 * @param repo
	 *            RDF4J Repository.
	 */
	public RDFBeanManager(Repository repo) {
		this.connectionPool = new RepositoryConnectionPool(repo);
		this.proxies = new ProxyInstancesPool(this);
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
	 * Closes this RDFBeanManager instance and RepositoryConnection objects for
	 * all threads.
	 * 
	 */
	@Override
	public void close() throws RepositoryException {
		connectionPool.closeAll();
	}

	// ====================== Constants CRUD functionality ====================

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
	 * The method has an optional vararg parameter to specify RDF4J contexts
	 * (named graph IRI's) to store the object triples in. If no context is specified, the 
	 * object is added into the default context, which can also be identified with the
	 * `null` reference. If multiple contexts are specified, the method creates separate copies 
	 * of the object in all those contexts.    
	 * 
	 * If an RDF representation of the given unanonymous object already exists
	 * in a specific context, the method does not perform any modifications in this context.
	 * 
	 * Upon marshalling a first instance of every Java class, the method adds
	 * special statement to the model containing information about binding of
	 * specific RDF type to that class. This information is needed to
	 * determine which class to use for instatiation of the unmarshalled objects
	 * later.
	 * 
	 * @see get(Resource)
	 *
	 *      If there is an active transaction started on RepositoryConnection
	 *      for the current thread, all
	 *      individual updates performed by this method are added to that
	 *      transaction. This means that the updates are not effective until the
	 *      transaction is committed. Otherwise, this method will start new
	 *      transaction to commit individual triple updates at once.
	 * 
	 * @param o
	 *            RDFBean object to add
	 * @param contexts
	 *            The contexts to add the object to.
	 * @return Resource IRI (or BNode for anonymous RDFBean)
	 * @throws RDFBeanException
	 *             If class of the object is not a valid RDFBean class
	 * @throws RepositoryException
	 * 
	 * @see update(Object)
	 */
	public Resource add(Object o, Resource... contexts) throws RDFBeanException, RepositoryException {
		return addOrUpdate(o, false, contexts);
	}

	/**
	 * Marshalls the state of a Java RDFBean object to an RDF resource (a set of
	 * triple statements in the underlying RDF model). If existing RDF
	 * representation of this
	 * object is found in the model, it will be overwritten.
	 * 
	 * The class of the object must conform the RDFBean specification.
	 * 
	 * The method has an optional vararg parameter to specify RDF4J contexts
	 * (named graph IRI's) to update the object triples in. If no context is specified, the 
	 * object is updated in the default context, which can also be identified with the
	 * `null` reference. If multiple contexts are specified, the method updates object copies
	 * in all those contexts.   
	 * 
	 * If no RDF representation for the given object is found in a specific context, or 
	 * if the object is an anonymous RDFBean, the method behaves like {@link #add(Object,Resource...) add()}.
	 *
	 * If there is an active transaction started on RepositoryConnection for the
	 * current thread, all individual
	 * updates performed by this method are added to that transaction. That
	 * means that the updates
	 * are not effective until the transaction is committed. Otherwise, this
	 * method will start new
	 * transaction to commit individual triple updates at once.
	 * 
	 * @param o
	 *            RDFBean to update
	 * @param contexts
	 *            The contexts to update the object in. If no contexts are
	 *            specified, the object added without context will be updated.
	 * @return Resource IRI (or BNode for anonymous RDFBean)
	 * @throws RDFBeanException
	 *             If class of the object is not a valid RDFBean class
	 * @throws RepositoryException
	 * 
	 * @see add(Object)
	 */
	public synchronized Resource update(Object o, Resource... contexts) throws RDFBeanException, RepositoryException {
		return addOrUpdate(o, true, contexts);
	}

	/**
	 * Unmarshalls the state of a Java RDFBean object from an RDF resource in the default (null) context. 
	 * 
	 * This is equivalent to `get(r, rdfBeanClass, null)`
	 * 
	 * @param r
	 *            Resource IRI (or BNode for anonymous RDFBean).
	 * @param rdfBeanClass
	 *            Java class of RDFBean
	 * @return Unmarshalled Java object, or null if the resource does not
	 *         exist
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean class or an instance of
	 *             this
	 *             class cannot be created
	 * @throws RDF4JException
	 * @see get(Resource,Class,Resource)
	 * @see get(Resource)
	 * @see get(String,Class)
	 * @see getAll(Class)
	 */
	public <T> T get(Resource r, Class<T> rdfBeanClass) throws RDFBeanException, RDF4JException {
		return this.get(r, rdfBeanClass, null);
	}

	/**
	 * Unmarshalls an RDF resource from a context (named graph) by creating an
	 * object of the specified Java class.
	 * 
	 * The class must conform to the RDFBean specification.
	 * 
	 * @param r
	 *            Resource IRI (or BNode for anonymous RDFBean).
	 * @param rdfBeanClass
	 *            Java class of RDFBean
	 * @param context
	 *            The context to retrieve the object from
	 * @return Unmarshalled Java object, or null if the resource does not
	 *         exist in the given context
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean class or an instance of
	 *             this
	 *             class cannot be created
	 * @throws RDF4JException
	 * @see get(Resource)
	 * @see get(String,Class)
	 * @see getAll(Class)
	 */
	public <T> T get(Resource r, Class<T> rdfBeanClass, Resource context) throws RDFBeanException, RDF4JException {
		if (!this.isResourceExist(r, context)) {
			return null;
		}
		return this._get(r, rdfBeanClass, context);
	}

	/**
	 * Unmarshalls an RDF resource by creating an object of auto-detected Java
	 * class.
	 * 
	 * The method tries to determine a Java class using binding class
	 * information
	 * added to the model at marshalling. If the binding class information is
	 * not
	 * found, RDFBeanException is thrown.
	 * 
	 * This method assumes that the object has been added without context. To
	 * retrieve objects from specific context,
	 * use {@link #get(Resource,Resource)} method.
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
		return this.get(r, null, null);
	}

	/**
	 * Unmarshalls an RDF resource from a context (named graph) by creating an
	 * object of auto-detected Java class.
	 * 
	 * The method tries to determine a Java class using binding class
	 * information
	 * added to the model at marshalling. If the binding class information is
	 * not
	 * found, RDFBeanException is thrown.
	 * 
	 * @param r
	 *            Resource IRI (or BNode for anonymous RDFBean).
	 * @param context
	 *            The context to retrieve the object from
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
	public Object get(Resource r, Resource context) throws RDFBeanException, RDF4JException {
		return this.get(r, null, context);
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
	 * This method assumes that the object has been added without context. To
	 * retrieve objects from specific context,
	 * use {@link #get(String,Class,Resource)} method.
	 * 
	 * @param stringId
	 *            RDFBean ID value
	 * @param rdfBeanClass
	 *            Java class of RDFBean
	 * @return The unmarshalled Java object, or null if the resource matching
	 *         the
	 *         given ID does not exist
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean class or an instance of
	 *             this
	 *             class cannot be created
	 * @throws RDF4JException
	 * @see get(Resource)
	 * @see get(Resource,Class)
	 * @see getAll(Class)
	 */
	public <T> T get(String stringId, Class<T> rdfBeanClass) throws RDFBeanException, RDF4JException {
		return this.get(stringId, rdfBeanClass, null);
	}

	/**
	 * Unmarshalls an RDF resource from a context (named graph) matching the
	 * specified RDFBean identifier
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
	 * @param context
	 *            The context to retrieve the object from
	 * @return The unmarshalled Java object, or null if the resource matching
	 *         the
	 *         given ID does not exist
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean class or an instance of
	 *             this
	 *             class cannot be created
	 * @throws RDF4JException
	 * @see get(Resource)
	 * @see get(Resource,Class)
	 * @see getAll(Class)
	 */
	public <T> T get(String stringId, Class<T> rdfBeanClass, Resource context) throws RDFBeanException, RDF4JException {
		Resource r = this.getResource(stringId, rdfBeanClass, context);
		if (r != null) {
			return this.get(r, rdfBeanClass, context);
		}
		return null;
	}

	public <T> CloseableIteration<T, Exception> getAll(final Class<T> rdfBeanClass, Resource... contexts)
			throws RDFBeanException, RepositoryException {
		return getAll(rdfBeanClass, false, contexts);
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
	 * @param contexts
	 *            The context(s) to get the data from. If no contexts are given,
	 *            all data is returned.
	 * @return Iterator over instances of the specified Java class
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean class
	 * @throws RepositoryException
	 */
	public <T> CloseableIteration<T, Exception> getAll(final Class<T> rdfBeanClass, boolean includeInferred,
			Resource... contexts) throws RDFBeanException, RepositoryException {
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

		final CloseableIteration<Statement, RepositoryException> sts = getRepositoryConnection().getStatements(null,
				RDF.TYPE, type, includeInferred, contexts);

		return new CloseableIteration<T, Exception>() {

			@Override
			public boolean hasNext() throws Exception {
				return sts.hasNext();
			}

			@Override
			public T next() throws Exception {
				return _get(sts.next().getSubject(), rdfBeanClass, contexts);
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
	public boolean isResourceExist(Resource r, Resource... contexts) throws RepositoryException {
		return hasStatement(r, null, null, contexts);
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
	 *             If the class is not a valid RDFBean class
	 * @throws RepositoryException
	 */
	public boolean isResourceExist(Resource r, Class rdfBeanClass, Resource... contexts)
			throws RDFBeanValidationException, RepositoryException {
		RDFBeanInfo rbi = RDFBeanInfo.get(rdfBeanClass);
		return hasStatement(r, RDF.TYPE, rbi.getRDFType(), contexts);
	}

	private boolean hasStatement(Resource s, IRI p, Value o, Resource... contexts) throws RepositoryException {
		ReadWriteLock lock = lockKeeper.getLock(s);
		lock.readLock().lock();
		try {
			return getRepositoryConnection().hasStatement(s, p, o, false, contexts);
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
	public Resource getResource(String stringId, Class rdfBeanClass, Resource... contexts)
			throws RDFBeanException, RepositoryException {
		SubjectProperty subject = RDFBeanInfo.get(rdfBeanClass).getSubjectProperty();
		if (subject != null) {
			IRI r = subject.getUri(stringId);
			if (isResourceExist(r, contexts)) {
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
	 * If there is an active transaction started on RepositoryConnection for the
	 * current thread, all
	 * individual updates performed by this method are added to that
	 * transaction. That means that the updates are not effective until the
	 * transaction is committed. Otherwise, this method will start new
	 * transaction to commit individual triple updates at once.
	 * 
	 * @param uri
	 *            Resource IRI
	 * @return true if the resource existed in the model before deletion, false
	 *         otherwise
	 * @throws RepositoryException
	 * @see delete(String,Class)
	 */
	public boolean delete(Resource uri, Resource... contexts) throws RepositoryException {
		if (isResourceExist(uri, contexts)) {
			deleteInternal(uri, contexts);
			return true;
		}
		return false;
	}

	private synchronized void deleteInternal(Resource uri, Resource... contexts) throws RepositoryException {
		RepositoryConnection conn = getRepositoryConnection();
		boolean newTxn = maybeStartTransaction(conn);
		ReadWriteLock lock = lockKeeper.getLock(uri);
		lock.writeLock().lock();
		try {
			if (contexts.length == 0) {
				// if no contexts are provided, delete only triples without context
				contexts = new Resource[] { null };
			}
			// delete where is a subject
			conn.remove(uri, null, null, contexts);
			// delete where is an object
			conn.remove((Resource) null, null, uri, contexts);
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
	 * RDFBean identifier and Java class
	 * from the underlying model.
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
	 * If there is an active transaction started on RepositoryConnection for the
	 * current thread, all
	 * individual updates performed by this method are added to that
	 * transaction. That means that the updates are not effective until the
	 * transaction is committed. Otherwise, this method will start new
	 * transaction to commit individual triple updates at once.
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
	public void delete(String stringId, Class rdfBeanClass, Resource... contexts)
			throws RDFBeanException, RepositoryException {
		Resource r = this.getResource(stringId, rdfBeanClass, contexts);
		if (r != null) {
			this.delete(r, contexts);
		}
	}

	private Resource addOrUpdate(Object o, boolean update, Resource... contexts)
			throws RDFBeanException, RepositoryException {
		RepositoryConnection conn = getRepositoryConnection();
		boolean newTxn = maybeStartTransaction(conn);
		Resource node;
		try {
			node = marshaller.marshal(conn, o, update, contexts);
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

	private <T> T _get(Resource r, Class<T> cls, Resource... contexts) throws RDFBeanException, RDF4JException {
		if (isResourceExist(r, contexts)) {
			// Unmarshal the resource
			return unmarshaller.unmarshal(getRepositoryConnection(), r, cls, contexts);
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
	public <T> T create(Resource r, Class<T> iface, Resource... contexts) throws RDFBeanException, RepositoryException {
		return createInternal(getRepositoryConnection(), r, RDFBeanInfo.get(iface), iface, contexts);
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
	public <T> T create(String id, Class<T> iface, Resource... contexts) throws RDFBeanException, RepositoryException {
		RDFBeanInfo rbi = RDFBeanInfo.get(iface);
		IRI uri = resolveUri(id, rbi);
		if (uri == null) {
			throw new RDFBeanException("Cannot resolve RDFBean ID: " + id);
		}
		return createInternal(getRepositoryConnection(), uri, rbi, iface, contexts);
	}

	private <T> T createInternal(RepositoryConnection conn, Resource r, RDFBeanInfo rbi, Class<T> iface,
			Resource... contexts) throws RDFBeanException, RepositoryException {
		boolean newObject = false;
		if (!isResourceExist(r)) {
			boolean newTxn = maybeStartTransaction(conn);
			try {
				conn.add(r, RDF.TYPE, rbi.getRDFType(), contexts);
				// conn.add(rbi.getRDFType(),
				// RDFBeanManager.BINDINGIFACE_PROPERTY,
				// conn.getValueFactory().createLiteral(rbi.getRDFBeanClass().getName()));
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
		T obj = proxies.getInstance(r, rbi, iface, contexts);
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
	 *            RDFBean-compliant Java interface
	 * @return Collection of dynamic proxy objects with the specified interface
	 * @throws RDFBeanException
	 *             If iface is not a valid RDFBean interface
	 * @throws RepositoryException
	 */
	public <T> Collection<T> createAll(Class<T> iface, Resource... contexts)
			throws RDFBeanException, RepositoryException {
		RDFBeanInfo rbi = RDFBeanInfo.get(iface);
		IRI type = rbi.getRDFType();
		Collection<T> result = new HashSet<T>();
		if (type == null) {
			return result;
		}
		RepositoryConnection conn = getRepositoryConnection();
		RepositoryResult<Statement> sts = null;
		try {
			sts = conn.getStatements(null, RDF.TYPE, type, false, contexts);
			while (sts.hasNext()) {
				T proxy = createInternal(conn, sts.next().getSubject(), rbi, iface, contexts);
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

	// ============================ Common methods =============================

	private boolean maybeStartTransaction(RepositoryConnection conn) {
		boolean newTxn = !conn.isActive();
		if (newTxn) {
			conn.begin();
		}
		return newTxn;
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
	 * Sets a custom ClassLoader instance for loading RDFBean classes.
	 * 
	 * By default, the classes are loaded by the ClassLoader of this
	 * RDFBeanManager.
	 * 
	 * @param classLoader
	 *            the ClassLoader instance to set
	 * 
	 * @see getClassLoader()
	 */
	public void setClassLoader(ClassLoader classLoader) {
		unmarshaller.setClassLoader(classLoader);
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

	/**
	 * Sets a DatatypeMapper implementation.
	 * 
	 * @param datatypeMapper
	 *            the datatypeMapper to set
	 * 
	 * @see getDatatypeMapper()
	 */
	public void setDatatypeMapper(DatatypeMapper datatypeMapper) {
		marshaller.setDatatypeMapper(datatypeMapper);
		unmarshaller.setDatatypeMapper(datatypeMapper);
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
