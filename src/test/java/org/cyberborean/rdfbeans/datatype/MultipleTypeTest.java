package org.cyberborean.rdfbeans.datatype;

import org.cyberborean.rdfbeans.RDFBeanManager;
import org.cyberborean.rdfbeans.exceptions.RDFBeanException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class MultipleTypeTest {
	private SailRepository repo;
	protected List<IRI> triedTypes = new ArrayList<>();
	private RDFBeanManager manager;

	private class CheckBeanManager extends RDFBeanManager {
		public CheckBeanManager(RepositoryConnection conn) {
			super(conn);
		}

		@Override
		protected Class<?> getBindingClassForType(IRI rdfType) throws RDFBeanException, RepositoryException {
			triedTypes.add(rdfType);
			return super.getBindingClassForType(rdfType);
		}
	}

	@Before
	public void setupManager() throws Exception {
		repo = new SailRepository(new MemoryStore());
		repo.initialize();
		RepositoryConnection initialFillConn = repo.getConnection();
		initialFillConn.add(getClass().getResourceAsStream("listUnmarshal.ttl"), "", RDFFormat.TURTLE);
		initialFillConn.close();
		manager = new CheckBeanManager(repo.getConnection());
	}

	@Test
	public void shouldTryAllTypes() {
		triedTypes.clear();
		try {
			manager.get(repo.getValueFactory().createIRI("http://example.com/list/dataClass"));
			throw new AssertionError("Should have thrown by now");
		} catch (RDFBeanException e) {
			assert(e.getMessage().startsWith("Cannot detect a binding class"));
		}
		assertThat(triedTypes.size(), is(2));
	}
}
