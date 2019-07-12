package org.cyberborean.rdfbeans.test.namedgraphs;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import org.cyberborean.rdfbeans.test.RDFBeansTestBase;
import org.cyberborean.rdfbeans.test.examples.entities.Person;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.Before;
import org.junit.Test;

/**
 * Test RDFBeans stored in different RDF4J contexts
 * 
 */
public class ExampleClassTest extends RDFBeansTestBase {

	Person john;
	Person mary;
	Person jim;

	Resource subject;

	IRI graph0;
	IRI graph1;
	IRI graph2;

	@Before
	public void setUp() throws Exception {
		john = new Person();
		john.setId("johndoe");
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

		mary = new Person();
		mary.setId("marysmith");
		mary.setName("Mary Smith");
		mary.setEmail("marysmith@example.com");
		Set<Person> maryKnows = new HashSet<Person>();
		maryKnows.add(john); // recursive link
		mary.setKnows(maryKnows);

		jim = new Person();
		// jim.setId("jimsmith"); // will be anonymous node
		jim.setName("Jim Smith");
		jim.setEmail("jimsmith@example.com");
		Set<Person> jimKnows = new HashSet<Person>();
		jimKnows.add(mary);
		jimKnows.add(john); // recursive link
		jim.setKnows(jimKnows);

		Set<Person> johnKnows = new HashSet<Person>();
		johnKnows.add(mary);
		johnKnows.add(jim);
		john.setKnows(johnKnows);

		// ---

		graph0 = manager.getRepositoryConnection().getValueFactory().createIRI("urn:contexts:0");
		graph1 = manager.getRepositoryConnection().getValueFactory().createIRI("urn:contexts:1");
		graph2 = manager.getRepositoryConnection().getValueFactory().createIRI("urn:contexts:2");

		// add object to two contexts and without one
		subject = manager.add(john);
		manager.getContext(graph1).add(john);
		manager.getContext(graph2).add(john);
	}

	private void checkIsJohn(Person p2) {
		assertNotNull(p2);
		assertEquals(john.getId(), p2.getId());
		assertEquals(john.getName(), p2.getName());
		assertEquals(john.getEmail(), p2.getEmail());
		assertEquals(john.getHomepage(), p2.getHomepage());
		assertEquals(john.getBirthday(), p2.getBirthday());
		assertNotNull(p2.getNick());
		assertEquals(john.getNick().length, p2.getNick().length);
		for (int i = 0; i < john.getNick().length; i++) {
			assertTrue(john.getNick(i).equals(p2.getNick(i)));
		}
		assertEquals(john.getKnows().size(), p2.getKnows().size());
		for (Person p : john.getKnows()) {
			boolean found = false;
			for (Person pknows : p2.getKnows()) {
				if (p.getName().equals(pknows.getName())) {
					found = true;
					break;
				}
			}
			assertTrue(found);
		}
	}
	
	@Test
	public void testCheckResourceExists() throws RepositoryException {
		assertTrue(manager.isResourceExist(subject));
		assertTrue(manager.getContext(graph1).isResourceExist(subject));
		assertTrue(manager.getContext(graph2).isResourceExist(subject));
		assertFalse(manager.getContext(graph0).isResourceExist(subject));
	}

	@Test
	public void testGet() throws Exception {
		_testGet(graph1);
		_testGet(graph2);

		Person p2 = (Person) manager.getContext(graph0).get(subject);
		assertNull(p2);
		p2 = manager.getContext(graph0).get(john.getId(), Person.class);
		assertNull(p2);
		p2 = manager.getContext(graph0).get(subject, Person.class);
		assertNull(p2);
	}

	private void _testGet(IRI graph) throws Exception {
		Person p2 = (Person) manager.getContext(graph).get(subject);
		checkIsJohn(p2);
		p2 = manager.getContext(graph).get(john.getId(), Person.class);
		checkIsJohn(p2);
		p2 = manager.getContext(graph).get(subject, Person.class);
		checkIsJohn(p2);
	}

	@Test
	public void testGetAll() throws Exception {
		_testGetAll(graph1);
		_testGetAll(graph2);
		
		CloseableIteration<Person, Exception> iter = manager.getContext(graph0).getAll(Person.class);
		assertFalse(iter.hasNext());
		iter.close();
		
	}
	
	private void _testGetAll(IRI graph) throws Exception {
		CloseableIteration<Person, Exception> iter = manager.getContext(graph).getAll(Person.class);
		Set<Person> s = new HashSet<>();
		while (iter.hasNext()) {
			Object o = iter.next();
			assertTrue(o instanceof Person);
			s.add((Person) o);
		}
		iter.close();
		assertEquals(s.size(), 3);
	}

	@Test
	public void testGetResource() throws Exception {
		Resource r = manager.getContext(graph1).getResource(john.getId(), Person.class);
		assertEquals(r, subject);
		
		r = manager.getContext(graph2).getResource(john.getId(), Person.class);
		assertEquals(r, subject);
		
		r = manager.getContext(graph0).getResource(john.getId(), Person.class);
		assertNull(r);
	}

	@Test
	public void testUpdate() throws Exception {
//		john.setName("John C. Doe");
//		String[] nicks = Arrays.copyOf(john.getNick(), john.getNick().length + 1);
//		nicks[nicks.length - 1] = "John C. Doe";
//		john.setNick(nicks);
//		
//		// update object without context
//		Resource r = manager.update(john);
//		checkIsJohn((Person) manager.get(r, (Resource)null));
//		
//		// update object in one context
//		r = manager.update(john, graph1);
//		checkIsJohn((Person) manager.get(r, graph1));		
//		Person p = (Person) manager.get(r, graph2);
//		assertEquals(p.getId(), john.getId());
//		assertFalse(p.getName().equals(john.getName()));
//				
//		r = manager.update(john);
//		checkIsJohn((Person) manager.get(r, graph1));
//		checkIsJohn((Person) manager.get(r, graph2));
	}

	@Test
	public void testDelete1() throws Exception {
		assertTrue(manager.getContext(graph1).isResourceExist(subject));
		assertTrue(manager.getContext(graph2).isResourceExist(subject));
		manager.getContext(graph1).delete(subject);
		assertFalse(manager.getContext(graph1).isResourceExist(subject));
		assertNull(manager.getContext(graph1).get(subject));
		assertTrue(manager.getContext(graph2).isResourceExist(subject));
		assertNotNull(manager.getContext(graph2).get(subject));
	}

	@Test
	public void testDelete2() throws Exception {
		assertTrue(manager.getContext(graph1).isResourceExist(subject));
		assertTrue(manager.getContext(graph2).isResourceExist(subject));
		manager.getContext(graph1).delete(john.getId(), Person.class);
		assertFalse(manager.getContext(graph1).isResourceExist(subject));
		assertNull(manager.getContext(graph1).get(subject));
		assertTrue(manager.getContext(graph2).isResourceExist(subject));
		assertNotNull(manager.getContext(graph2).get(subject));
	}

}
