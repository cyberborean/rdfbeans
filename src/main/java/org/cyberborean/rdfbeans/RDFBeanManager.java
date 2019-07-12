package org.cyberborean.rdfbeans;

import java.util.HashMap;
import java.util.Map;

import org.cyberborean.rdfbeans.datatype.DatatypeMapper;
import org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper;
import org.cyberborean.rdfbeans.impl.Marshaller;
import org.cyberborean.rdfbeans.impl.RepositoryConnectionPool;
import org.cyberborean.rdfbeans.impl.Unmarshaller;
import org.cyberborean.rdfbeans.util.LockKeeper;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Provides basic CRUD and dynamic proxy management functions for persisting
 * RDFBean data objects using a RDF model stored in RDF4J repository.
 * 
 */
public class RDFBeanManager extends RDFBeanManagerContext implements AutoCloseable {		
		
	private Map<IRI, RDFBeanManagerContext> contexts = new HashMap<>(); 
	
	/**
	 * Creates new RDFBeanManager instance backed by the given RDF4J Repository.
	 * 
	 * @param repo
	 *            RDF4J Repository.
	 */
	public RDFBeanManager(Repository repo) {
		super(null);
		connectionPool = new RepositoryConnectionPool(repo);
		lockKeeper = new LockKeeper();
		marshaller = new Marshaller(lockKeeper, new DefaultDatatypeMapper());
		unmarshaller = new Unmarshaller(lockKeeper, new DefaultDatatypeMapper(),
				this.getClass().getClassLoader());
	}

	public RDFBeanManagerContext getContext(IRI iri) {
		if (iri == null) {
			return this;
		}
		RDFBeanManagerContext context = contexts.get(iri);
		if (context == null) {
			context = new RDFBeanManagerContext(iri, this);
			contexts.put(iri, context);
		}
		return context;
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

	
	// ============================ Common methods =============================

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

	
}
