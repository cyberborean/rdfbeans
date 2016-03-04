package org.cyberborean.rdfbeans.test;

import org.cyberborean.rdfbeans.RDFBeanManager;
import org.junit.After;
import org.junit.Before;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.openrdf.sail.memory.MemoryStore;

public abstract class RDFBeansTestBase {
		 
	protected RDFBeanManager manager;
	private Repository repo;

	@Before
	public void setupManager() throws Exception {
		repo = new SailRepository(new MemoryStore());
        repo.initialize();        
        manager = new RDFBeanManager(repo.getConnection());
	}
	
	@After
	public void teardownManager() throws Exception {
		manager.getRepositoryConnection().close();
        repo.shutDown();
	}
	
	protected void dumpRepository() {
		try {
			manager.getRepositoryConnection().export(
				Rio.createWriter(RDFFormat.TURTLE, System.out));
		} catch (RepositoryException | RDFHandlerException | UnsupportedRDFormatException e) {
			e.printStackTrace();
		}
	}
}
