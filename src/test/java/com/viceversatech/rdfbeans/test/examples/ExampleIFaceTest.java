/**
 * ExamplesTest.java
 * 
 * RDFBeans Mar 22, 2011 12:44:48 PM alex
 *
 * $Id:$
 *  
 */
package com.viceversatech.rdfbeans.test.examples;

import java.net.URI;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.ontoware.rdf2go.ModelFactory;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Syntax;
import org.ontoware.rdf2go.model.node.Resource;

import com.viceversatech.rdfbeans.RDFBeanManager;
import com.viceversatech.rdfbeans.test.examples.entities.IPerson;


public class ExampleIFaceTest extends TestCase {

    IPerson john;
    IPerson mary;
    IPerson jim;
    
    Model model;
    RDFBeanManager manager;
    Resource subject;
    
    /** 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
    	
    	ModelFactory modelFactory = RDF2Go.getModelFactory();
        model = modelFactory.createModel();
        model.open();  
    	manager = new RDFBeanManager(model);
    	
    	john = manager.create("johndoe", IPerson.class);
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
        
        mary = manager.create("marysmith", IPerson.class); 
        mary.setName("Mary Smith");
        mary.setEmail("marysmith@example.com");
        Set<IPerson> maryKnows = new HashSet<IPerson>();        
        maryKnows.add(john); // recursive link
        mary.setKnows(maryKnows);
        
        jim = manager.create("jimsmith", IPerson.class);
        jim.setName("Jim Smith");
        jim.setEmail("jimsmith@example.com");
        Set<IPerson> jimKnows = new HashSet<IPerson>();
        jimKnows.add(mary);
        jimKnows.add(john); // recursive link
        jim.setKnows(jimKnows);
        
        Set<IPerson> johnKnows = new HashSet<IPerson>();
        johnKnows.add(mary);
        johnKnows.add(jim);
        john.setKnows(johnKnows);
        
    }

    /** 
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {        
        model.close();
    }
    
    public void testGetters() throws Exception {    	
    	assertEquals(john.getName(), "John Doe");
    	assertEquals(john.getEmail(), "johndoe@example.com");    	
    	assertNotNull(john.getBirthday());
    	Calendar c = Calendar.getInstance();
    	c.setTime(john.getBirthday());
    	assertEquals(c.get(Calendar.YEAR), 1980);
    	assertEquals(c.get(Calendar.MONTH), 0);
    	assertEquals(c.get(Calendar.DAY_OF_MONTH), 1);    	
    	String[] nicks = john.getNick();
    	assertEquals(nicks.length, 3);
    	assertTrue(Arrays.equals(nicks, new String[] {"johndoe", "johnnydoe", "JohnnyTheTerrible"}));
    	for (int i = 0; i < nicks.length; i++) {
    		assertEquals(nicks[i], john.getNick(i));
    	}    	
    	assertEquals(john.getHomepage(), new URI("http://johndoe.example.com"));
    	Collection<IPerson> johnKnows = john.getKnows();
    	assertFalse(johnKnows.isEmpty());
    	assertEquals(johnKnows.size(), 2);
    	for (IPerson p: johnKnows) {
    		assertTrue(p.equals(mary) || p.equals(jim));
    		assertNotNull(p.getName());
    		assertNotNull(p.getEmail());
    		assertTrue(p.getName().equals("Mary Smith") || p.getName().equals("Jim Smith"));
    		assertFalse(p.getKnows().isEmpty());
    		assertTrue(p.getKnows().contains(john));
    	}
    }
    
    public void testUpdate() throws Exception {
    	assertEquals(john.getName(), "John Doe");
    	john.setName("John Doe II");
    	assertEquals(john.getName(), "John Doe II");
    	// Indexed properties
    	john.setNick(1, "johnnydoeII");
    	assertEquals(john.getNick(1), "johnnydoeII");
    	assertTrue(Arrays.equals(john.getNick(), new String[] {"johndoe", "johnnydoeII", "JohnnyTheTerrible"}));
    }
    
    public void _testDump() throws Exception {
    	// DEBUG dump the model
        Syntax syntax = Syntax.RdfXml;
        model.writeTo(System.out, syntax);
        // ---
    }
}
