package org.cyberborean.rdfbeans.test.rdfbeanmanager;

import static org.junit.Assert.*;

import java.net.URI;

import org.cyberborean.rdfbeans.RDFBeanManager;
import org.cyberborean.rdfbeans.exceptions.RDFBeanException;
import org.cyberborean.rdfbeans.test.RDFBeansTestBase;
import org.cyberborean.rdfbeans.test.examples.entities.IPerson;
import org.cyberborean.rdfbeans.test.examples.entities.Person;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.Test;

public class TransactionTest extends RDFBeansTestBase {

	@Test
	public void testAdd() throws RepositoryException, RDFBeanException {
		Person john = new Person();
		john.setId("johndoe");
		john.setName("John Doe");
		john.setEmail("johndoe@example.com");

		manager.getRepositoryConnection().begin();
		Resource r = manager.add(john);

		// we create new RDFBeanManager on the same repository to ensure that
		// the different connection will be used for check
		try (RDFBeanManager manager2 = new RDFBeanManager(repo)) {

			// the resource should not appear until commit
			assertFalse(manager2.isResourceExist(r));

			manager.getRepositoryConnection().commit();

			assertTrue(manager2.isResourceExist(r));
		}
	}

	@Test
	public void testUpdate() throws RepositoryException, RDFBeanException {
		Person john = new Person();
		john.setId("johndoe");
		john.setName("John Doe");
		john.setEmail("johndoe@example.com");
		Resource r = manager.add(john);

		manager.getRepositoryConnection().begin();

		john.setEmail("johndoe2@example.com");
		john.setHomepage(URI.create("http://john.example.com"));
		manager.update(john);

		// we create new RDFBeanManager on the same repository to ensure that
		// the different connection will be used for check
		try (RDFBeanManager manager2 = new RDFBeanManager(repo)) {

			// the updates should not appear until commit
			john = manager2.get(r, Person.class);
			assertEquals("johndoe@example.com", john.getEmail());
			assertNull(john.getHomepage());

			manager.getRepositoryConnection().commit();

			john = manager2.get(r, Person.class);
			assertEquals("johndoe2@example.com", john.getEmail());
			assertNotNull(john.getHomepage());
		}
	}

	@Test
	public void testDelete() throws RepositoryException, RDFBeanException {
		Person john = new Person();
		john.setId("johndoe");
		john.setName("John Doe");
		john.setEmail("johndoe@example.com");
		Resource r = manager.add(john);

		manager.getRepositoryConnection().begin();

		manager.delete(r);

		// we create new RDFBeanManager on the same repository to ensure that
		// the different connection will be used for check
		try (RDFBeanManager manager2 = new RDFBeanManager(repo)) {

			// the resource should appear until commit
			assertTrue(manager2.isResourceExist(r));

			manager.getRepositoryConnection().commit();

			assertFalse(manager2.isResourceExist(r));
		}
	}

	@Test
	public void testProxyCreate() throws RepositoryException, RDFBeanException {
		manager.getRepositoryConnection().begin();

		IRI iri = manager.getRepositoryConnection().getValueFactory().createIRI("urn:test:johndoe");
		IPerson john = manager.create(iri, IPerson.class);
		john.setName("John Doe");
		john.setEmail("johndoe@example.com");

		// we create new RDFBeanManager on the same repository to ensure that
		// the different connection will be used for check
		try (RDFBeanManager manager2 = new RDFBeanManager(repo)) {

			// the resource should not appear until commit
			assertFalse(manager2.isResourceExist(iri));

			manager.getRepositoryConnection().commit();

			assertTrue(manager2.isResourceExist(iri));
		}
	}

	@Test
	public void testProxyUpdate() throws RepositoryException, RDFBeanException {
		IRI iri = manager.getRepositoryConnection().getValueFactory().createIRI("urn:test:johndoe");
		IPerson john = manager.create(iri, IPerson.class);
		john.setName("John Doe");
		john.setEmail("johndoe@example.com");

		manager.getRepositoryConnection().begin();

		john.setEmail("johndoe2@example.com");
		john.setHomepage(URI.create("http://john.example.com"));

		// we create new RDFBeanManager on the same repository to ensure that
		// the different connection will be used for check
		try (RDFBeanManager manager2 = new RDFBeanManager(repo)) {

			// the updates should not appear until commit
			john = manager2.create(iri, IPerson.class);
			assertEquals("johndoe@example.com", john.getEmail());
			assertNull(john.getHomepage());

			manager.getRepositoryConnection().commit();

			john = manager2.create(iri, IPerson.class);
			assertEquals("johndoe2@example.com", john.getEmail());
			assertNotNull(john.getHomepage());
		}
	}

}
