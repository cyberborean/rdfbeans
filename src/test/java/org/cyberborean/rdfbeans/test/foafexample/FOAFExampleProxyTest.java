package org.cyberborean.rdfbeans.test.foafexample;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cyberborean.rdfbeans.test.RDFBeansTestBase;
import org.cyberborean.rdfbeans.test.foafexample.entities.IDocument;
import org.cyberborean.rdfbeans.test.foafexample.entities.IPerson;
import org.junit.Before;
import org.junit.Test;
import org.eclipse.rdf4j.model.Resource;

/**
 * A synthetic test for cascade proxy databinding and RDFBeans interfaces inheritance
 * 
 */
public class FOAFExampleProxyTest extends RDFBeansTestBase {
    
    IPerson john;
    IPerson mary;
    IPerson jim;
    
    Resource subject;    
  
    @Before
    public void setUp() throws Exception { 
    	
        john = manager.create("johndoe", IPerson.class);
        john.setName("John Doe");
        john.setMbox("johndoe@example.com");        
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, 1980);
        c.set(Calendar.MONTH, 0);
        c.set(Calendar.DAY_OF_MONTH, 1);
        john.setBirthday(c.getTime());
        String[] nicks = new String[] {"johndoe", "johnnydoe", "JohnnyTheTerrible"};
        john.setNick(nicks);
        
        IDocument d = manager.create("http://johndoe.example.com", IDocument.class);
        d.setName("John's homepage");
        john.setHomepage(d);
        
        mary = manager.create("marysmith", IPerson.class); 
        mary.setName("Mary Smith");
        mary.setMbox("marysmith@example.com");
        Set<IPerson> maryKnows = new HashSet<IPerson>();        
        maryKnows.add(john); // recursive link
        mary.setKnows(maryKnows);
        
        jim = manager.create("jimsmith", IPerson.class);
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
    }
    
    @Test
    public void testGetters() {    	
    	assertEquals(john.getName(), "John Doe");
    	assertEquals(john.getMbox(), "johndoe@example.com");    	
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
    	IDocument d = john.getHomepage();
    	assertNotNull(d);
    	assertEquals(d.getUri(), "http://johndoe.example.com");
    	assertEquals(d.getName(), "John's homepage");
    	Set<IPerson> johnKnows = john.getKnows();
    	assertFalse(johnKnows.isEmpty());
    	assertEquals(johnKnows.size(), 2);
    	for (IPerson p: johnKnows) {
    		assertTrue(p.equals(mary) || p.equals(jim));
    		assertNotNull(p.getName());
    		assertNotNull(p.getMbox());
    		assertTrue(p.getName().equals("Mary Smith") || p.getName().equals("Jim Smith"));
    		assertFalse(p.getKnows().isEmpty());
    		assertTrue(p.getKnows().contains(john));
    	}
    }
    
    @Test
    public void testUpdate() throws Exception {
    	assertEquals(john.getName(), "John Doe");
    	john.setName("John Doe II");
    	assertEquals(john.getName(), "John Doe II");
    	// Indexed properties
    	john.setNick(1, "johnnydoeII");
    	assertEquals(john.getNick(1), "johnnydoeII");
    	assertTrue(Arrays.equals(john.getNick(), new String[] {"johndoe", "johnnydoeII", "JohnnyTheTerrible"}));
    }
    
    @Test
    public void testInversions() throws Exception {
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
        //p2.setName("Publication #2"); -- inversions should work without storing an RDFBean in the model 
        IDocument p3 = manager.create("http://johndoe.example.com/pub3", IDocument.class);
        john.setPublications(new IDocument[] {p1, p2, p3});
        assertEquals(p1.getAuthor(), john);
        assertEquals(p2.getAuthor(), john);
        assertEquals(p3.getAuthor(), john);           
    }
    
    @Test
    public void testCreateAll() throws Exception {
    	Collection<IPerson> all = manager.createAll(IPerson.class);
    	assertEquals(all.size(), 3);
    	for (IPerson p : all) {
    		assertTrue(p.equals(john) || p.equals(mary) || p.equals(jim));
    	}
    }
   
}
