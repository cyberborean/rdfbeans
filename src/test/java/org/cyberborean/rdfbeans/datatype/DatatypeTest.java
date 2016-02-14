/**
 * DatatypeTest.java
 * 
 * RDFBeans Feb 4, 2011 4:24:41 PM alex
 *
 * $Id: DatatypeTest.java 40 2014-04-05 04:07:09Z alexeya $
 *  
 */
package org.cyberborean.rdfbeans.datatype;

import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.cyberborean.rdfbeans.RDFBeanManager;
import org.cyberborean.rdfbeans.test.entities.DatatypeTestClass;
import org.ontoware.rdf2go.ModelFactory;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Syntax;
import org.ontoware.rdf2go.model.node.Resource;

/**
 * DatatypeTest.
 *
 * @author alex
 *
 */
public class DatatypeTest extends TestCase {
	
	DatatypeTestClass object;
	Model model;
    RDFBeanManager manager;
    Resource resource;
    
    /** 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
    	object = new DatatypeTestClass();
    	object.setStringValue("TEST");
    	object.setBooleanValue(true);
    	object.setIntValue(100);
    	object.setFloatValue(-3.141f);
    	object.setDoubleValue(Math.PI);
    	object.setByteValue(Byte.MAX_VALUE);
    	object.setLongValue(Long.MAX_VALUE);
    	object.setShortValue(Short.MAX_VALUE);
    	object.setDateValue(new Date());
    	object.setUriValue(URI.create("http://rdfbeans.sourceforge.net"));
    	int[] array = new int[] {0, 1, 2, 3, 4};
    	object.setArrayValue(array);
    	object.setListValue(Arrays.asList(new String[] {"a", "b", "c", "d"}));
    	Set set = new HashSet(3);
    	set.add("foo"); set.add("bar"); set.add("baz");
    	object.setSetValue(set);
    	SortedSet sortedSet = new TreeSet();
    	sortedSet.addAll(set);
    	object.setSortedSetValue(sortedSet);    	
    	
    	ModelFactory modelFactory = RDF2Go.getModelFactory();
        model = modelFactory.createModel();
        model.open();        
        manager = new RDFBeanManager(model);
        resource = manager.add(object);        
    }
    
    protected void tearDown() throws Exception {        
        model.close();
    }
    
    public void test() throws Exception {               
    	Object o = manager.get(resource);    	
    	assertNotNull(o);
    	assertTrue(o instanceof DatatypeTestClass);
    	DatatypeTestClass object2 = (DatatypeTestClass)o;
    	assertNotSame(object, object2);
    	assertEquals(object.getStringValue(), object2.getStringValue());
    	assertEquals(object.isBooleanValue(), object2.isBooleanValue());
    	assertEquals(object.getIntValue(), object2.getIntValue());
    	assertEquals(object.getFloatValue(), object2.getFloatValue());
    	assertEquals(object.getDoubleValue(), object2.getDoubleValue());
    	assertEquals(object.getByteValue(), object2.getByteValue());
    	assertEquals(object.getLongValue(), object2.getLongValue());
    	assertEquals(object.getShortValue(), object2.getShortValue());
    	assertEquals(object.getDateValue(), object2.getDateValue());
    	assertEquals(object.getUriValue(), object2.getUriValue());
    	assertTrue(Arrays.equals(object.getArrayValue(), object2.getArrayValue()));
    	assertTrue(Arrays.equals(object.getListValue().toArray(), object2.getListValue().toArray()));
    	assertTrue(object.getSetValue().containsAll(object2.getSetValue()));
    	Iterator s1 = object.getSortedSetValue().iterator();
    	Iterator s2 = object2.getSortedSetValue().iterator();
    	while (s1.hasNext() && s2.hasNext()) {
    		assertEquals(s1.next(), s2.next());
    	}    	
    }
    
    public void _testDump() throws Exception {
    	// DEBUG dump the model
        Syntax syntax = Syntax.RdfXml;
        model.writeTo(System.out, syntax);
        // ---
    }
    
    

}
