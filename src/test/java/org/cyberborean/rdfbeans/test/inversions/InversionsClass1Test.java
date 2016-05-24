package org.cyberborean.rdfbeans.test.inversions;

import static org.junit.Assert.*;

import org.cyberborean.rdfbeans.RDFBeanManager;
import org.cyberborean.rdfbeans.annotations.RDF;
import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.annotations.RDFSubject;
import org.cyberborean.rdfbeans.test.RDFBeansTestBase;
import org.junit.Test;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;

/**
 * @author alex
 *
 */
public class InversionsClass1Test extends RDFBeansTestBase  {
	
	@RDFBean("urn:test:Parent")
	public static class Parent {
		String id;
		Child[] children;
		
		@RDFSubject
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		
		@RDF(inverseOf="urn:test:hasParent")
		public Child[] getChildren() {
			return children;
		}
		public void setChildren(Child[] children) {
			this.children = children;
		}
	}
	
	@RDFBean("urn:test:Child")
	public static class Child {
		String id;
		Parent parent;
		
		@RDFSubject
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		
		@RDF("urn:test:hasParent")
		public Parent getParent() {
			return parent;
		}
		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}
	    
	@Test
    public void testInversions1() throws Exception {    	
    	String parentId = "urn:test:beans/Parent"; 
    			
    	Parent parent = new Parent();
    	parent.setId(parentId);
    	
    	Child child1 = new Child();
    	child1.setId("urn:test:beans/Child1");
    	
    	Child child2 = new Child();
    	child2.setId("urn:test:beans/Child2");
    	
    	parent.setChildren(new Child[]{child1, child2});
    	
    	manager.add(parent);
    	
    	// Reinstantiate the RDFBeanManager to get rid of the objects cache
    	manager = new RDFBeanManager(manager.getRepositoryConnection());
    	
    	parent = manager.get(parentId, Parent.class);
    	assertNotNull(parent);
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
    			
    	Parent parent = new Parent();
    	parent.setId(parentId);
    	
    	Child child1 = new Child();
    	child1.setId("urn:test:beans/Child1");
    	child1.setParent(parent);
    	
    	Child child2 = new Child();
    	child2.setId("urn:test:beans/Child2");
    	child2.setParent(parent);
    	
    	manager.add(child1);
    	manager.add(child2);
    	
    	// Reinstantiate the RDFBeanManager to get rid of the objects cache
    	manager = new RDFBeanManager(manager.getRepositoryConnection());
    	
    	parent = manager.get(parentId, Parent.class);
    	assertNotNull(parent);
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
		
    	Parent parent = new Parent();
    	parent.setId(parentId);
    	
    	Child child1 = new Child();
    	child1.setId("urn:test:beans/Child1");
    	
    	Child child2 = new Child();
    	child2.setId("urn:test:beans/Child2");
    	
    	parent.setChildren(new Child[]{child1, child2});
    	
    	manager.add(parent);
    	
    	// Reinstantiate the RDFBeanManager to get rid of the objects cache
    	manager = new RDFBeanManager(manager.getRepositoryConnection());
    	
    	parent = manager.get(parentId, Parent.class);
    	assertNotNull(parent);
    	Child[] children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 2);
    	
    	// update children
    	parent.setChildren(new Child[] {child1});
    	manager.update(parent);
    	
    	// Reinstantiate the RDFBeanManager to get rid of the objects cache
    	manager = new RDFBeanManager(manager.getRepositoryConnection());
    	
    	parent = manager.get(parentId, Parent.class);
    	assertNotNull(parent);
    	children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 1);
    	assertEquals(children[0].getId(), child1.getId());
    }
    
    @Test
    public void testUpdate2() throws Exception {
    	String parentId = "urn:test:beans/Parent"; 
		
    	Parent parent = new Parent();
    	parent.setId(parentId);
    	
    	Child child1 = new Child();
    	child1.setId("urn:test:beans/Child1");
    	child1.setParent(parent);
    	
    	Child child2 = new Child();
    	child2.setId("urn:test:beans/Child2");
    	child2.setParent(parent);
    	
    	manager.add(child1);
    	manager.add(child2);
    	
    	// Reinstantiate the RDFBeanManager to get rid of the objects cache
    	manager = new RDFBeanManager(manager.getRepositoryConnection());
    	
    	parent = manager.get(parentId, Parent.class);
    	assertNotNull(parent);
    	Child[] children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 2);
    	
    	// update children
    	child2.setParent(null);
    	manager.update(child2);
    	
    	// Reinstantiate the RDFBeanManager to get rid of the objects cache
    	manager = new RDFBeanManager(manager.getRepositoryConnection());
    	
    	parent = manager.get(parentId, Parent.class);
    	assertNotNull(parent);
    	children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 1);
    	assertEquals(children[0].getId(), child1.getId());
    }
    
    @Test
    public void testDelete1() throws Exception {
    	String parentId = "urn:test:beans/Parent"; 
		
    	Parent parent = new Parent();
    	parent.setId(parentId);
    	
    	Child child1 = new Child();
    	child1.setId("urn:test:beans/Child1");
    	
    	Child child2 = new Child();
    	child2.setId("urn:test:beans/Child2");
    	
    	parent.setChildren(new Child[]{child1, child2});
    	
    	manager.add(parent);
    	
    	// Reinstantiate the RDFBeanManager to get rid of the objects cache
    	manager = new RDFBeanManager(manager.getRepositoryConnection());
    	
    	parent = manager.get(parentId, Parent.class);
    	assertNotNull(parent);
    	Child[] children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 2);
    	
    	// remove children
    	manager.delete(child2.getId(), Child.class);
    	assertNull(manager.get(child2.getId(), Child.class));
    	
    	// Reinstantiate the RDFBeanManager to get rid of the objects cache
    	manager = new RDFBeanManager(manager.getRepositoryConnection());
    	
    	parent = manager.get(parentId, Parent.class);
    	assertNotNull(parent);
    	children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 1);
    	assertEquals(children[0].getId(), child1.getId());
    }
    
    @Test
    public void testDelete2() throws Exception {
    	String parentId = "urn:test:beans/Parent"; 
		
    	Parent parent = new Parent();
    	parent.setId(parentId);
    	
    	Child child1 = new Child();
    	child1.setId("urn:test:beans/Child1");
    	child1.setParent(parent);
    	
    	Child child2 = new Child();
    	child2.setId("urn:test:beans/Child2");
    	child2.setParent(parent);
    	
    	manager.add(child1);
    	manager.add(child2);
    	
    	// Reinstantiate the RDFBeanManager to get rid of the objects cache
    	manager = new RDFBeanManager(manager.getRepositoryConnection());
    	
    	parent = manager.get(parentId, Parent.class);
    	assertNotNull(parent);
    	Child[] children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 2);
    	
    	// remove parent
    	manager.delete(parentId, Parent.class);
    	assertNull(manager.get(parentId, Parent.class));    	
    	
    	// Reinstantiate the RDFBeanManager to get rid of the objects cache
    	manager = new RDFBeanManager(manager.getRepositoryConnection());
    	
    	CloseableIteration<Child, Exception> childIter = manager.getAll(Child.class);
    	while (childIter.hasNext()) {
    		Parent p = childIter.next().getParent();
    		assertNull(p);
    	}
    }

}
