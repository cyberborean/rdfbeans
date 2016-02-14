/**
 * RDFBeanProxyTest.java
 * 
 * RDFBeans Sep 30, 2011 4:36:25 PM alex
 *
 * $Id:$
 *  
 */
package org.cyberborean.rdfbeans.test.proxy;

import java.util.Collection;

import junit.framework.TestCase;

import org.cyberborean.rdfbeans.RDFBeanManager;
import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.annotations.RDFSubject;
import org.ontoware.rdf2go.ModelFactory;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.node.Resource;

/**
 * RDFBeanProxyTest.
 *
 * @author alex
 *
 */
public class RDFBeanProxyTest extends TestCase {

	RDFBeanManager manager;
    Model model;
    Resource subject;    
  
    /** 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {    	
    	ModelFactory modelFactory = RDF2Go.getModelFactory();
        model = modelFactory.createModel();
        model.open();  
    	manager = new RDFBeanManager(model);
    }
    
    protected void tearDown() throws Exception {        
        model.close();
    }
    
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
