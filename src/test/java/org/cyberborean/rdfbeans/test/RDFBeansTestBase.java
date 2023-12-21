package org.cyberborean.rdfbeans.test;

import org.cyberborean.rdfbeans.RDFBeanManager;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;

public abstract class RDFBeansTestBase {
		 
	protected RDFBeanManager manager;
	protected Repository repo;

	@Before
	public void setupManager() throws Exception {
		repo = new SailRepository(new MemoryStore());
        //repo.initialize();   
        manager = new RDFBeanManager(repo);
	}
	
	@After
	public void teardownManager() throws Exception {
		manager.close();
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
