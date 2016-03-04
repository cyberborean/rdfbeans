package org.cyberborean.rdfbeans.test.inversions;

import static org.junit.Assert.*;

import org.cyberborean.rdfbeans.annotations.RDF;
import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.annotations.RDFSubject;
import org.cyberborean.rdfbeans.test.RDFBeansTestBase;
import org.junit.Test;

/**
 * @author alex
 *
 */
public class InversionsIFace2Test extends RDFBeansTestBase  {
	
	@RDFBean("urn:test:Parent")
	public static interface Parent {
		
		@RDFSubject
		public String getId();
		public void setId(String id);
		
		@RDF("urn:test:hasChildren")
		public Child[] getChildren();		
		public void setChildren(Child[] children);
	}
	
	@RDFBean("urn:test:Child")
	public static interface Child {

		@RDFSubject
		public String getId();
		public void setId(String id);
		
		@RDF(inverseOf="urn:test:hasChildren")
		public Parent getParent();
		public void setParent(Parent parent);
	}
	
	@Test
    public void testInversions1() throws Exception {    	
    	String parentId = "urn:test:beans/Parent"; 
    			
    	Parent parent = manager.create(parentId, Parent.class);    	
    	Child child1 = manager.create("urn:test:beans/Child1", Child.class);
    	Child child2 = manager.create("urn:test:beans/Child2", Child.class);
    	    	    	
    	parent.setChildren(new Child[]{child1, child2});
    	
    	Child[] children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 2);
    	for (Child child: children) {
    		assertTrue(child.getId().equals(child1.getId()) || child.getId().equals(child2.getId()));
    		assertEquals(child.getParent(), parent);
    	}
    }
    
	@Test
	public void testInversions2() throws Exception {    	
    	String parentId = "urn:test:beans/Parent"; 
    			
    	Parent parent = manager.create(parentId, Parent.class);    	
    	Child child1 = manager.create("urn:test:beans/Child1", Child.class);
    	Child child2 = manager.create("urn:test:beans/Child2", Child.class);
    	
    	child1.setParent(parent);
    	child2.setParent(parent);
    	
    	Child[] children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 2);
    	for (Child child: children) {
    		assertTrue(child.getId().equals(child1.getId()) || child.getId().equals(child2.getId()));
    		assertEquals(child.getParent(), parent);
    	}
    }
    
	@Test
    public void testUpdate1() throws Exception {
    	String parentId = "urn:test:beans/Parent"; 
		
    	Parent parent = manager.create(parentId, Parent.class);    	
    	Child child1 = manager.create("urn:test:beans/Child1", Child.class);
    	Child child2 = manager.create("urn:test:beans/Child2", Child.class);
    	    	    	
    	parent.setChildren(new Child[]{child1, child2});
    	
    	Child[] children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 2);
    	
    	// update children
    	parent.setChildren(new Child[] {child1});
    	
    	children = parent.getChildren();
    	assertEquals(children.length, 1);
    	assertEquals(children[0].getId(), child1.getId());
    }
    
	@Test
    public void testUpdate2() throws Exception {
    	String parentId = "urn:test:beans/Parent"; 
		
    	Parent parent = manager.create(parentId, Parent.class);    	
    	Child child1 = manager.create("urn:test:beans/Child1", Child.class);
    	Child child2 = manager.create("urn:test:beans/Child2", Child.class);
    	
    	child1.setParent(parent);
    	child2.setParent(parent);
    	
    	Child[] children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 2);
    	
    	// update children
    	child2.setParent(null);
    	
    	children = parent.getChildren();
    	assertEquals(children.length, 1);
    	assertEquals(children[0].getId(), child1.getId());
    }
    
	@Test
    public void testDelete1() throws Exception {
    	String parentId = "urn:test:beans/Parent"; 
		
    	Parent parent = manager.create(parentId, Parent.class);    	
    	Child child1 = manager.create("urn:test:beans/Child1", Child.class);
    	Child child2 = manager.create("urn:test:beans/Child2", Child.class);
    	    	    	
    	parent.setChildren(new Child[]{child1, child2});
    	
    	Child[] children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 2);
    	
    	// remove children
    	manager.delete(child2.getId(), Child.class);
    	assertNull(manager.get(child2.getId(), Child.class));

    	children = parent.getChildren();
    	assertEquals(children.length, 1);
    	assertEquals(children[0].getId(), child1.getId());
    }
    
	@Test
    public void testDelete2() throws Exception {
    	String parentId = "urn:test:beans/Parent"; 
		
    	Parent parent = manager.create(parentId, Parent.class);    	
    	Child child1 = manager.create("urn:test:beans/Child1", Child.class);
    	Child child2 = manager.create("urn:test:beans/Child2", Child.class);
    	
    	child1.setParent(parent);
    	child2.setParent(parent);
    	
    	Child[] children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 2);
    	
    	// remove parent
    	manager.delete(parentId, Parent.class);
    	assertNull(manager.get(parentId, Parent.class));
    	
    	assertNull(child1.getParent());
    	assertNull(child2.getParent());
    }

}
