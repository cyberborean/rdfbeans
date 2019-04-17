package org.cyberborean.rdfbeans.test.namedgraphs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URI;
import java.util.Calendar;

import org.cyberborean.rdfbeans.exceptions.RDFBeanException;
import org.cyberborean.rdfbeans.test.RDFBeansTestBase;
import org.cyberborean.rdfbeans.test.examples.entities.Person;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.Before;
import org.junit.Test;

public class MultipleContextTest extends RDFBeansTestBase {

	private static final String PERSON_ID = "johndoe"; 
	Person john;
	
	Resource graph0;
	Resource graph1;
	Resource graph2;
	
	@Before
	public void setUp() throws Exception {
		john = new Person();
		john.setId(PERSON_ID);
		john.setName("John Doe");
		john.setEmail("johndoe@example.com");
		Calendar c = Calendar.getInstance();
		c.set(Calendar.YEAR, 1980);
		c.set(Calendar.MONTH, 0);
		c.set(Calendar.DAY_OF_MONTH, 1);
		john.setBirthday(c.getTime());
		String[] nicks = new String[] { "johndoe", "johnnydoe", "JohnnyTheTerrible" };
		john.setNick(nicks);
		john.setHomepage(new URI("http://johndoe.example.com"));
		
		graph0 = manager.getRepositoryConnection().getValueFactory().createIRI("urn:contexts:0");
		graph1 = manager.getRepositoryConnection().getValueFactory().createIRI("urn:contexts:1");
		graph2 = manager.getRepositoryConnection().getValueFactory().createIRI("urn:contexts:2");
	}

	@Test
	public void testAddToNullContext() throws RepositoryException, RDFBeanException {
		manager.add(john);
		
		assertNotNull(manager.get(PERSON_ID, Person.class));
		
		assertNull(manager.get(PERSON_ID, Person.class, graph0));
		assertNull(manager.get(PERSON_ID, Person.class, graph1));
		assertNull(manager.get(PERSON_ID, Person.class, graph2));
	}
	
	@Test
	public void testAddToOneContext() throws RepositoryException, RDFBeanException {
		manager.add(john, graph0);
		
		assertNotNull(manager.get(PERSON_ID, Person.class, graph0));
		
		assertNull(manager.get(PERSON_ID, Person.class));
		assertNull(manager.get(PERSON_ID, Person.class, graph1));
		assertNull(manager.get(PERSON_ID, Person.class, graph2));
	}

	@Test
	public void testAddToManyContexts() throws RepositoryException, RDFBeanException {
		manager.add(john, graph0, graph1);
		
		assertNotNull(manager.get(PERSON_ID, Person.class, graph0));
		assertNotNull(manager.get(PERSON_ID, Person.class, graph1));
				
		assertNull(manager.get(PERSON_ID, Person.class));
		assertNull(manager.get(PERSON_ID, Person.class, graph2));
	}
	
	@Test
	public void testAddToManyAndNullContext() throws RepositoryException, RDFBeanException {
		manager.add(john, graph0, graph1, null);
		
		assertNotNull(manager.get(PERSON_ID, Person.class, graph0));
		assertNotNull(manager.get(PERSON_ID, Person.class, graph1));
		assertNotNull(manager.get(PERSON_ID, Person.class));
		
		assertNull(manager.get(PERSON_ID, Person.class, graph2));		
	}
	
	@Test
	public void testUpdateInNullContext() throws RepositoryException, RDFBeanException {
		manager.add(john);
		
		Person copy = manager.get(PERSON_ID, Person.class);
		copy.setEmail("foo@example.com");		
		manager.update(copy);
		
		assertEquals("foo@example.com", manager.get(PERSON_ID, Person.class).getEmail());
	}
	
	@Test
	public void testUpdateInOneContext() throws RepositoryException, RDFBeanException {
		manager.add(john, graph0);
		
		Person copy = manager.get(PERSON_ID, Person.class, graph0);
		copy.setEmail("foo@example.com");		
		manager.update(copy, graph0);
		
		assertEquals("foo@example.com", manager.get(PERSON_ID, Person.class, graph0).getEmail());
	}
	
	@Test
	public void testUpdateInOneFromManyContexts() throws RepositoryException, RDFBeanException {
		manager.add(john, graph0, graph1);
		
		Person copy = manager.get(PERSON_ID, Person.class, graph0);
		copy.setEmail("foo@example.com");		
		manager.update(copy, graph0);
		
		assertEquals("foo@example.com", manager.get(PERSON_ID, Person.class, graph0).getEmail());
		assertEquals("johndoe@example.com", manager.get(PERSON_ID, Person.class, graph1).getEmail());
	}
	
	@Test
	public void testUpdateInNullFromManyContext() throws RepositoryException, RDFBeanException {
		manager.add(john, graph0, graph1);
		
		Person copy = manager.get(PERSON_ID, Person.class, graph0);
		copy.setEmail("foo@example.com"); 		
		manager.update(copy); 
		
		// we expect that update() acts like add() if the resource is missing, 
		// so a copy of the object without a context should be created
		
		assertEquals("foo@example.com", manager.get(PERSON_ID, Person.class).getEmail());		
		assertEquals("johndoe@example.com", manager.get(PERSON_ID, Person.class, graph0).getEmail());
		assertEquals("johndoe@example.com", manager.get(PERSON_ID, Person.class, graph1).getEmail());
	}
	
	@Test
	public void testUpdateInDifferentContext() throws RepositoryException, RDFBeanException {
		manager.add(john, graph0);
		
		Person copy = manager.get(PERSON_ID, Person.class, graph0);
		copy.setEmail("foo@example.com");		
		manager.update(copy, graph1); 
		
		// we expect that update() acts like add() if the resource is missing,
		// so a copy of the object in the given context should be created
				
		assertEquals("johndoe@example.com", manager.get(PERSON_ID, Person.class, graph0).getEmail());
		assertEquals("foo@example.com", manager.get(PERSON_ID, Person.class, graph1).getEmail());		
	}
	
	@Test
	public void testDeleteFromNullContext() throws RepositoryException, RDFBeanException {
		manager.add(john);		
		assertNotNull(manager.get(PERSON_ID, Person.class));
		
		manager.delete(PERSON_ID, Person.class);
		assertNull(manager.get(PERSON_ID, Person.class));		
	}
	
	@Test
	public void testDeleteFromOneContext() throws RepositoryException, RDFBeanException {
		manager.add(john, graph0);		
		assertNotNull(manager.get(PERSON_ID, Person.class, graph0));
		
		manager.delete(PERSON_ID, Person.class, graph0);
		assertNull(manager.get(PERSON_ID, Person.class, graph0));		
	}
	
	@Test
	public void testDeleteFromDifferentContext() throws RepositoryException, RDFBeanException {
		manager.add(john, graph0);		
		assertNotNull(manager.get(PERSON_ID, Person.class, graph0));
		
		manager.delete(PERSON_ID, Person.class, graph1);
		
		// the model should not be affected
		assertNotNull(manager.get(PERSON_ID, Person.class, graph0));		
	}
	
	@Test
	public void testDeleteFromOneFromManyContexts() throws RepositoryException, RDFBeanException {
		manager.add(john, graph0, graph1);		
		assertNotNull(manager.get(PERSON_ID, Person.class, graph0));
		assertNotNull(manager.get(PERSON_ID, Person.class, graph1));
		
		manager.delete(PERSON_ID, Person.class, graph0);
		assertNull(manager.get(PERSON_ID, Person.class, graph0));
		assertNotNull(manager.get(PERSON_ID, Person.class, graph1));
	}
	
	@Test
	public void testDeleteFromMultipleContexts() throws RepositoryException, RDFBeanException {
		manager.add(john, graph0, graph1);		
		assertNotNull(manager.get(PERSON_ID, Person.class, graph0));
		assertNotNull(manager.get(PERSON_ID, Person.class, graph1));
		
		manager.delete(PERSON_ID, Person.class, graph0, graph1);
		assertNull(manager.get(PERSON_ID, Person.class, graph0));
		assertNull(manager.get(PERSON_ID, Person.class, graph1));
	}
	
	@Test
	public void testDeleteFromNullFromManyContext() throws RepositoryException, RDFBeanException {
		manager.add(john, graph0, null);		
		assertNotNull(manager.get(PERSON_ID, Person.class, graph0));
		assertNotNull(manager.get(PERSON_ID, Person.class));
		
		manager.delete(PERSON_ID, Person.class);
		assertNull(manager.get(PERSON_ID, Person.class));
		assertNotNull(manager.get(PERSON_ID, Person.class, graph0));
		
	}
	
}
