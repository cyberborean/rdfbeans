package org.cyberborean.rdfbeans.test.rdfbeanmanager;

import static org.junit.Assert.assertTrue;

import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.annotations.RDFSubject;
import org.cyberborean.rdfbeans.exceptions.RDFBeanException;
import org.cyberborean.rdfbeans.test.RDFBeansTestBase;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.Test;

public class RDFSClassesHierarchyTest extends RDFBeansTestBase {

	@Test
	public void testClasses() throws RepositoryException, RDFBeanException {
		manager.add(new C());
		try (RepositoryConnection conn = manager.getRepositoryConnection()) {
			assertTrue(
				conn.hasStatement(
						SimpleValueFactory.getInstance().createIRI(RDF_TYPE_C),
						RDFS.SUBCLASSOF,
						SimpleValueFactory.getInstance().createIRI(RDF_TYPE_B), false)				
			);
			assertTrue(
					conn.hasStatement(
							SimpleValueFactory.getInstance().createIRI(RDF_TYPE_B),
							RDFS.SUBCLASSOF,
							SimpleValueFactory.getInstance().createIRI(RDF_TYPE_A), false)				
				);
		}
	}
	
	@Test
	public void testInterfaces() throws RepositoryException, RDFBeanException {
		manager.create(RDFBEAN_ID, IC.class);
		try (RepositoryConnection conn = manager.getRepositoryConnection()) {
			assertTrue(
				conn.hasStatement(
						SimpleValueFactory.getInstance().createIRI(RDF_TYPE_C),
						RDFS.SUBCLASSOF,
						SimpleValueFactory.getInstance().createIRI(RDF_TYPE_B), false)				
			);
			assertTrue(
					conn.hasStatement(
							SimpleValueFactory.getInstance().createIRI(RDF_TYPE_B),
							RDFS.SUBCLASSOF,
							SimpleValueFactory.getInstance().createIRI(RDF_TYPE_A), false)				
				);
		}
	}
	
	private static final String RDFBEAN_ID = "urn:ids:0";
	private static final String RDF_TYPE_A = "http://cyberborean.org/rdfbeans/test/A";
	private static final String RDF_TYPE_B = "http://cyberborean.org/rdfbeans/test/B";
	private static final String RDF_TYPE_C = "http://cyberborean.org/rdfbeans/test/C";
	
	@RDFBean(RDF_TYPE_A)
	public static class A {
		
		@RDFSubject
		public String getId() {
			return RDFBEAN_ID;
		}
	}
	
	@RDFBean(RDF_TYPE_B)
	public static class B extends A {}
	
	@RDFBean(RDF_TYPE_C)
	public static class C extends B {}
	
	@RDFBean(RDF_TYPE_A)
	public static interface IA {}
	
	@RDFBean(RDF_TYPE_B)
	public static interface IB extends IA {}
	
	@RDFBean(RDF_TYPE_C)
	public static interface IC extends IB {}
		

}
