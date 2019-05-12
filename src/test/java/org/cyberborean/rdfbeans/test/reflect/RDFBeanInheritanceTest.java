package org.cyberborean.rdfbeans.test.reflect;

import static org.junit.Assert.assertEquals;

import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.exceptions.RDFBeanException;
import org.cyberborean.rdfbeans.reflect.RDFBeanInfo;
import org.cyberborean.rdfbeans.test.RDFBeansTestBase;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.Test;

// see https://github.com/cyberborean/rdfbeans/issues/35
public class RDFBeanInheritanceTest extends RDFBeansTestBase {
	
	private static final String RDF_TYPE_A = "http://cyberborean.org/rdfbeans/test/A";
	private static final String RDF_TYPE_B = "http://cyberborean.org/rdfbeans/test/B";
	private static final String RDF_TYPE_C = "http://cyberborean.org/rdfbeans/test/C";

	@Test
	public void testClasses() throws RepositoryException, RDFBeanException {		
		// class must inherit @RDFBean annotation from the nearest parent, thanks to @Inherited meta-annotation
		RDFBeanInfo rbi = RDFBeanInfo.get(X.class);
		assertEquals(RDF_TYPE_C, rbi.getRDFType().toString());
	}
	
	@Test
	public void testInterfaces() throws RepositoryException, RDFBeanException {		
		// interface must inherit @RDFBean annotation from the nearest extended interface
		RDFBeanInfo rbi = RDFBeanInfo.get(IX.class);
		assertEquals(RDF_TYPE_C, rbi.getRDFType().toString());
	}

	@RDFBean(RDF_TYPE_A)
	static class A {}
	
	@RDFBean(RDF_TYPE_B)
	static class B extends A {}
	
	@RDFBean(RDF_TYPE_C)
	static class C extends A {}
	
	static class D extends C {}
	
	static class X extends D {}
	

	@RDFBean(RDF_TYPE_A)
	static interface IA {}
	
	@RDFBean(RDF_TYPE_B)
	static interface IB extends IA {}
	
	@RDFBean(RDF_TYPE_C)
	static interface IC extends IA {}
	
	static interface ID extends IC {}
	
	static interface IX extends ID {}
}
