package org.cyberborean.rdfbeans.test.examples;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import org.cyberborean.rdfbeans.test.RDFBeansTestBase;
import org.cyberborean.rdfbeans.test.examples.entities.Person;
import org.junit.Before;
import org.junit.Test;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;


public class ExampleClassTest extends RDFBeansTestBase {

    Person john;
    Person mary;
    Person jim;
    
    Resource subject;
    
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
        String[] nicks = new String[] {"johndoe", "johnnydoe", "JohnnyTheTerrible"};
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
        //jim.setId("jimsmith");      // will be anonymous node
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
        subject = manager.add(john);
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
        for (Person p: john.getKnows()) {            
            boolean found = false;
        	for (Person pknows: p2.getKnows()) {
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
    }
    
    @Test
    public void testGet() throws Exception {                
        Person p2 = (Person) manager.get(subject);
        checkIsJohn(p2);
        p2 = manager.get(john.getId(), Person.class);
        checkIsJohn(p2);
        p2 = manager.get(subject, Person.class);
        checkIsJohn(p2);
    }        
    
    @Test
    public void testGetAll() throws Exception {                
        CloseableIteration<Person, Exception> iter = manager.getAll(Person.class);
        Set<Person> s = new HashSet<>();
        while (iter.hasNext()) {
            Object o = iter.next();
            assertTrue(o instanceof Person);
            s.add((Person)o);
        }
        iter.close();
        assertEquals(s.size(), 3);
    }
    
    @Test
    public void testGetResource() throws Exception {                
        Resource r = manager.getResource(john.getId(), Person.class);
        assertEquals(r, subject);        
    }    
    
    @Test
    public void testUpdate() throws Exception {
        john.setName("John C. Doe");
        String[] nicks = Arrays.copyOf(john.getNick(), john.getNick().length + 1);
        nicks[nicks.length-1] = "John C. Doe";
        john.setNick(nicks);
        Resource r = manager.update(john);
        checkIsJohn((Person)manager.get(r));
    }
    
    @Test
    public void testDelete1() throws Exception {
        assertTrue(manager.isResourceExist(subject));
        manager.delete(subject);
        assertFalse(manager.isResourceExist(subject));
        assertNull(manager.get(subject));
    }
    
    @Test
    public void testDelete2() throws Exception {
        assertTrue(manager.isResourceExist(subject));
        manager.delete(john.getId(), Person.class);
        assertFalse(manager.isResourceExist(subject));
        assertNull(manager.get(subject));
    }

}
