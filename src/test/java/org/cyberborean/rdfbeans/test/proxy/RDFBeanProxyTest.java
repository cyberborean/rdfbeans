package org.cyberborean.rdfbeans.test.proxy;

import static org.junit.Assert.*;

import java.util.Collection;

import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.annotations.RDFSubject;
import org.cyberborean.rdfbeans.test.RDFBeansTestBase;
import org.junit.Test;

/**
 * RDFBeanProxyTest.
 *
 * @author alex
 *
 */
public class RDFBeanProxyTest extends RDFBeansTestBase {

    @Test
    public void testEquals() throws Exception {    
    	TestIface o1 = manager.create("urn:test:object", TestIface.class);
    	TestIface o2 = manager.create("urn:test:object", TestIface.class);
    	TestIface o3 = manager.create("urn:test:object1", TestIface.class);
    	assertTrue(o1.equals(o2));
    	//assertNotSame(o1, o2); // -- not true after proxy caching was implemented
    	assertSame(o1, o2);
    	assertFalse(o1.equals(o3));
    	Collection<TestIface> all = manager.createAll(TestIface.class);
    	assertEquals(all.size(), 2);
    }
    
    @Test
    public void testHashCode() throws Exception {    
    	TestIface o1 = manager.create("urn:test:object", TestIface.class);
    	TestIface o2 = manager.create("urn:test:object", TestIface.class);
    	TestIface o3 = manager.create("urn:test:object1", TestIface.class);
    	assertTrue(o1.hashCode() == o2.hashCode());
    	assertFalse(o1.hashCode() == o3.hashCode());
    }
    
    
    @RDFBean("http://cyberborean.org/rdfbeans/2.0/test/proxy/ProxyTestIface")
	public interface TestIface {
    	@RDFSubject
    	String getUri();
	}
}
