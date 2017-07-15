package org.cyberborean.rdfbeans.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

public class RepositoryConnectionPool {
	
	private Repository repo;
	private ConcurrentMap<Thread, RepositoryConnection> pool = new ConcurrentHashMap<>();	
	private ThreadLocal<RepositoryConnection> connHolder = new ThreadLocal<RepositoryConnection>() {

		@Override
		protected RepositoryConnection initialValue() {
			return createNewConnection();
		}
		
	}; 
		
	public RepositoryConnectionPool(Repository repo) {
		this.repo = repo;
	}
	
	private RepositoryConnection createNewConnection() {
		RepositoryConnection conn = repo.getConnection();
		pool.put(Thread.currentThread(), conn);
		return conn;
	}

	public RepositoryConnection getConnection() throws RepositoryException {
		RepositoryConnection conn = connHolder.get();
		if (!conn.isOpen()) {
			conn = createNewConnection();
		}
		return conn;
	}

	public synchronized void closeAll() throws RepositoryException {
		for (RepositoryConnection conn: pool.values()) {
			conn.close();
		}
		pool.clear();
	}

	
}
