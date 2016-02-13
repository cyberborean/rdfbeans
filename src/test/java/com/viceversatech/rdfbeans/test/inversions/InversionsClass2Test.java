/**
 * 
 */
package com.viceversatech.rdfbeans.test.inversions;

import java.net.URI;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.ModelFactory;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;

import com.viceversatech.rdfbeans.RDFBeanManager;
import com.viceversatech.rdfbeans.annotations.RDF;
import com.viceversatech.rdfbeans.annotations.RDFBean;
import com.viceversatech.rdfbeans.annotations.RDFSubject;
import com.viceversatech.rdfbeans.test.examples.entities.Person;
import com.viceversatech.rdfbeans.test.inversions.InversionsClass1Test.Child;
import com.viceversatech.rdfbeans.test.inversions.InversionsClass1Test.Parent;

import junit.framework.TestCase;

/**
 * @author alex
 *
 */
public class InversionsClass2Test extends TestCase {
	
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
		
		@RDF("urn:test:hasChildren")
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
		
		@RDF(inverseOf="urn:test:hasChildren")
		public Parent getParent() {
			return parent;
		}
		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}
	
	Model model;
    RDFBeanManager manager;
	
	protected void setUp() throws Exception {        
        ModelFactory modelFactory = RDF2Go.getModelFactory();
        model = modelFactory.createModel();
        model.open();        
        manager = new RDFBeanManager(model);
    }

    protected void tearDown() throws Exception {        
        model.close();
    }
    
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
    	manager = new RDFBeanManager(model);
    	
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
    	manager = new RDFBeanManager(model);
    	
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
    	manager = new RDFBeanManager(model);
    	
    	parent = manager.get(parentId, Parent.class);
    	assertNotNull(parent);
    	Child[] children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 2);
    	
    	// update children
    	parent.setChildren(new Child[] {child1});
    	manager.update(parent);
    	
    	// Reinstantiate the RDFBeanManager to get rid of the objects cache
    	manager = new RDFBeanManager(model);
    	
    	parent = manager.get(parentId, Parent.class);
    	assertNotNull(parent);
    	children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 1);
    	assertEquals(children[0].getId(), child1.getId());
    }
    
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
    	manager = new RDFBeanManager(model);
    	
    	parent = manager.get(parentId, Parent.class);
    	assertNotNull(parent);
    	Child[] children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 2);
    	
    	// update children
    	children[0].setParent(null);
    	manager.update(children[0]);
    	
    	// Reinstantiate the RDFBeanManager to get rid of the objects cache
    	manager = new RDFBeanManager(model);
    	
    	parent = manager.get(parentId, Parent.class);
    	assertNotNull(parent);
    	children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 1);
    }

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
    	manager = new RDFBeanManager(model);
    	
    	parent = manager.get(parentId, Parent.class);
    	assertNotNull(parent);
    	Child[] children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 2);
    	
    	// remove children
    	manager.delete(child2.getId(), Child.class);
    	assertNull(manager.get(child2.getId(), Child.class));
    	
    	// Reinstantiate the RDFBeanManager to get rid of the objects cache
    	manager = new RDFBeanManager(model);
    	
    	parent = manager.get(parentId, Parent.class);
    	assertNotNull(parent);
    	children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 1);
    	assertEquals(children[0].getId(), child1.getId());
    }
    
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
    	manager = new RDFBeanManager(model);
    	
    	parent = manager.get(parentId, Parent.class);
    	assertNotNull(parent);
    	Child[] children = parent.getChildren();
    	assertNotNull(children);
    	assertEquals(children.length, 2);
    	
    	// remove parent
    	manager.delete(parentId, Parent.class);
    	assertNull(manager.get(parentId, Parent.class));
    	
    	
    	// Reinstantiate the RDFBeanManager to get rid of the objects cache
    	manager = new RDFBeanManager(model);
    	
    	ClosableIterator<Child> childIter = manager.getAll(Child.class);
    	while (childIter.hasNext()) {
    		Parent p = childIter.next().getParent();
    		assertNull(p);
    	}
    }
}
