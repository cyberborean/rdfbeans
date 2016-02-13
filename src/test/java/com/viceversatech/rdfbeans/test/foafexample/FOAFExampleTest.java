
package com.viceversatech.rdfbeans.test.foafexample;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.ModelFactory;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Syntax;
import org.ontoware.rdf2go.model.node.Resource;

import com.viceversatech.rdfbeans.RDFBeanManager;
import com.viceversatech.rdfbeans.test.foafexample.entities.IPerson;
import com.viceversatech.rdfbeans.test.foafexample.entities.impl.Document;
import com.viceversatech.rdfbeans.test.foafexample.entities.impl.Person;

/**
 * A synthetic test for cascade databinding and RDFBeans classes inheritance 
 * 
 * @version $Id: FOAFExampleTest.java 30 2011-09-28 06:46:32Z alexeya $
 * @author Alex Alishevskikh, alexeya(at)gmail.com
 * 
 */
public class FOAFExampleTest extends TestCase {
    
    Person john;
    Person mary;
    Person jim;
    
    Model model;
    RDFBeanManager manager;
    Resource subject;
    
    /** 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        john = new Person();
        john.setId("johndoe");
        john.setName("John Doe");
        john.setMbox("johndoe@example.com");        
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, 1980);
        c.set(Calendar.MONTH, 0);
        c.set(Calendar.DAY_OF_MONTH, 1);
        john.setBirthday(c.getTime());
        String[] nicks = new String[] {"johndoe", "johnnydoe", "JohnnyTheTerrible"};
        john.setNick(nicks);
        Document d = new Document();
        d.setUri("http://johndoe.example.com");
        d.setName("John's homepage");
        john.setHomepage(d);
        
        mary = new Person();        
        mary.setId("marysmith");        
        mary.setName("Mary Smith");
        mary.setMbox("marysmith@example.com");
        Set<IPerson> maryKnows = new HashSet<IPerson>();        
        maryKnows.add(john); // recursive link
        mary.setKnows(maryKnows);
        
        jim = new Person();
        //jim.setId("jimsmith");      // will be anonymous node
        jim.setName("Jim Smith");
        jim.setMbox("jimsmith@example.com");
        Set<IPerson> jimKnows = new HashSet<IPerson>();
        jimKnows.add(mary);
        jimKnows.add(john); // recursive link
        jim.setKnows(jimKnows);
        
        Set<IPerson> johnKnows = new HashSet<IPerson>();
        johnKnows.add(mary);
        johnKnows.add(jim);
        john.setKnows(johnKnows);
        
        // ----
        ModelFactory modelFactory = RDF2Go.getModelFactory();
        model = modelFactory.createModel();
        model.open();        
        manager = new RDFBeanManager(model);
        
        // ---
        subject = manager.add(john);
    }

    /** 
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {        
        model.close();
    }
    
    private void checkIsJohn(Person p2) {
    	assertNotNull(p2);
    	// -- to enable equality check, Person must override equals()/hashCode()
    	//assertEquals(john, p2);
        assertEquals(john.getId(), p2.getId());
        assertEquals(john.getName(), p2.getName());
        assertEquals(john.getMbox(), p2.getMbox());
        assertEquals(john.getHomepage(), p2.getHomepage());
        assertEquals(john.getBirthday(), p2.getBirthday());
        assertNotNull(p2.getNick());
        assertEquals(john.getNick().length, p2.getNick().length);
        for (int i = 0; i < john.getNick().length; i++) {
            assertTrue(john.getNick(i).equals(p2.getNick(i)));
        }
        assertEquals(john.getKnows().size(), p2.getKnows().size());
        for (IPerson p: john.getKnows()) {            
            boolean found = false;
        	for (IPerson pknows: p2.getKnows()) {
            	if (p.getName().equals(pknows.getName())) {
            		found = true;
            		break;
            	}
            }
        	assertTrue(found);
        }
        
        // -- to enable the checks below, Person must override custom equals()/hashCode()
        /*
        for (IPerson p: john.getKnows()) {            
            assertTrue(p2.getKnows().contains(p));
        }
        assertTrue(p2.getKnows().contains(mary));
        assertTrue(p2.getKnows().contains(jim));
        assertTrue(mary.getKnows().contains(p2));
        assertTrue(jim.getKnows().contains(p2));
        */
    }
    
    public void testCheckResourceExists() {
        assertTrue(manager.isResourceExist(subject));
    }
    
    public void testGet() throws Exception {                
        Person p2 = (Person)manager.get(subject);
        checkIsJohn(p2);
        p2 = manager.get(john.getId(), Person.class);
        checkIsJohn(p2);
        p2 = manager.get(subject, Person.class);
        checkIsJohn(p2);
    }        
    
    public void testGetAll() throws Exception {                
        ClosableIterator<Person> iter = manager.getAll(Person.class);
        Set s = new HashSet();
        while (iter.hasNext()) {
            Object o = iter.next();
            assertTrue(o instanceof Person);
            s.add(o);
        }
        iter.close();
        assertEquals(s.size(), 3);
    }
    
    public void testGetResource() throws Exception {                
        Resource r = manager.getResource(john.getId(), Person.class);
        assertEquals(r, subject);        
    }    
    
    
    public void testUpdate() throws Exception {
        john.setName("John C. Doe");
        john.getHomepage().setUri("http://johndoe.example.com/home");
        String[] nicks = Arrays.copyOf(john.getNick(), john.getNick().length + 1);
        nicks[nicks.length-1] = "John C. Doe";
        john.setNick(nicks);
        Resource r = manager.update(john);
        checkIsJohn((Person)manager.get(r));
    }
    
    public void testDelete1() throws Exception {
        assertTrue(manager.isResourceExist(subject));
        manager.delete(subject);
        assertFalse(manager.isResourceExist(subject));
        assertNull(manager.get(subject));
    }
    
    public void testDelete2() throws Exception {
        assertTrue(manager.isResourceExist(subject));
        manager.delete(john.getId(), Person.class);
        assertFalse(manager.isResourceExist(subject));
        assertNull(manager.get(subject));
    }
    
    public void testInversions() throws Exception {
   /*
    TODO
        checkIsJohn(john);
    	assertNotNull(john.getHomepage().getOwner());
    	assertEquals(john.getHomepage().getOwner(), john);
    	
    	Set<IPerson> knowsJohn = john.getKnownBy();
    	assertFalse(knowsJohn.isEmpty());
    	assertTrue(knowsJohn.contains(mary));
    	assertTrue(knowsJohn.contains(jim));
    	assertFalse(knowsJohn.contains(john));
    	
    	Set<IPerson> knowsJim = jim.getKnownBy();
    	assertFalse(knowsJim.isEmpty());
    	assertTrue(knowsJim.contains(john));
    	assertFalse(knowsJim.contains(mary));
    	
    	IDocument p1 = manager.create("http://johndoe.example.com/pub1", IDocument.class);
        p1.setName("Publication #1");
        IDocument p2 = manager.create("http://johndoe.example.com/pub2", IDocument.class);
        p2.setName("Publication #2");
        IDocument p3 = manager.create("http://johndoe.example.com/pub3", IDocument.class);
        p3.setName("Publication #3");
        john.setPublications(new IDocument[] {p1, p2, p3});
        assertEquals(p1.getAuthor(), john);
        assertEquals(p2.getAuthor(), john);
        assertEquals(p3.getAuthor(), john);           	
       */
    	
    }

    public void _testDump() throws Exception {
    	// DEBUG dump the model
        Syntax syntax = Syntax.RdfXml;
        model.writeTo(System.out, syntax);
        // ---
    }
}
