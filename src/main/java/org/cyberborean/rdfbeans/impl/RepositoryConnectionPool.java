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
			RepositoryConnection conn = repo.getConnection();
			pool.put(Thread.currentThread(), conn);
			return conn;
		}

		@Override
		public RepositoryConnection get() {
			RepositoryConnection conn = super.get();
			if (!conn.isOpen()) {
				// create new connection if the current one is closed
				conn = initialValue();
				set(conn);				
			}
			return conn;
		}
		
	}; 
		
	public RepositoryConnectionPool(Repository repo) {
		this.repo = repo;
	}
	
	public RepositoryConnection getConnection() throws RepositoryException {
		return connHolder.get();
	}

	public synchronized void closeAll() throws RepositoryException {
		for (RepositoryConnection conn: pool.values()) {
			conn.close();
		}
		pool.clear();
	}

	
}
