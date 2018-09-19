package org.cyberborean.rdfbeans.test.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.cyberborean.rdfbeans.test.RDFBeansTestBase;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Test;

public class RepositoryConnectionPoolTest extends RDFBeansTestBase {

final int numOfThreads = 100;
	
	@Test
	public void testRepositoryConnectionPool() throws InterruptedException, ExecutionException {		
		ExecutorService executor = Executors.newFixedThreadPool(numOfThreads);
		List<Future<String>> futures = new ArrayList<>(numOfThreads);
		final CountDownLatch latch = new CountDownLatch(1);
		Set<RepositoryConnection> connections = Collections.synchronizedSet(new HashSet<>());
		for (int i = 0; i < numOfThreads; i++) {
			futures.add(
				executor.submit(new Callable<String>() {

					@Override
					public String call() throws Exception {
						latch.await();				
						RepositoryConnection conn1 = manager.getRepositoryConnection();						
						RepositoryConnection conn2 = manager.getRepositoryConnection();
						if (conn1 != conn2) {
							// multiple getRepositoryConnection() calls must return the same RepositoryConnection object
							// as long as the thread is the same
							return "getRepositoryConnection() returned different RepositoryConnection instances for the same thread!";
						}
						if (connections.contains(conn1)) {
							// getRepositoryConnection() calls must return different RepositoryConnection objects for
							// different threads
							return "getRepositoryConnection() returned the same RepositoryConnection instance for different threads!";
						}
						connections.add(conn1);						
						return null;
					}
				} )
			);
		}
		
		latch.countDown();
		
		int i = 0;
		for (Future<String> f: futures) {
			String result = f.get();
			assertNull(i++ + ": " + result, result);
		}
		
		// extra check that all connections were unique for their threads 
		assertEquals(numOfThreads, connections.size());
	}
	
}
