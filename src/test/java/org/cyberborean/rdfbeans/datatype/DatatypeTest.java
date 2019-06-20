package org.cyberborean.rdfbeans.datatype;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.cyberborean.rdfbeans.test.RDFBeansTestBase;
import org.cyberborean.rdfbeans.test.entities.DatatypeTestClass;
import org.eclipse.rdf4j.model.Resource;
import org.junit.Before;
import org.junit.Test;

public class DatatypeTest extends RDFBeansTestBase {
	
	DatatypeTestClass object;
    Resource resource;
    
    @Before
    public void setUp() throws Exception {
    	object = new DatatypeTestClass();
    	object.setStringValue("TEST");
    	object.setCharValue('a');
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
        object.setListValue(Arrays.asList(new Object[] {"a", "b", "c", "d"}));
        Set<Object> set = new HashSet<>(3);
    	set.add("foo"); set.add("bar"); set.add("baz");
    	object.setSetValue(set);
        SortedSet<Object> sortedSet = new TreeSet<>();
    	sortedSet.addAll(set);
        object.setSortedSetValue(sortedSet);
        resource = manager.add(object);
        
        this.dumpRepository();
    }
    
    @Test
    public void test() throws Exception {               
    	Object o = manager.get(resource);    	
    	assertNotNull(o);
    	assertTrue(o instanceof DatatypeTestClass);
    	DatatypeTestClass object2 = (DatatypeTestClass)o;
    	assertNotSame(object, object2);
    	assertEquals(object.getStringValue(), object2.getStringValue());
    	assertEquals(object.getCharValue(), object2.getCharValue());
    	assertEquals(object.isBooleanValue(), object2.isBooleanValue());
    	assertEquals(object.getIntValue(), object2.getIntValue());
    	assertEquals(object.getFloatValue(), object2.getFloatValue(), 0);
    	assertEquals(object.getDoubleValue(), object2.getDoubleValue(), 0);
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
      
}
