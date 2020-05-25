package org.cyberborean.rdfbeans.test.namedgraphs;

import static org.junit.Assert.fail;

import org.cyberborean.rdfbeans.annotations.RDF;
import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.annotations.RDFSubject;
import org.cyberborean.rdfbeans.exceptions.RDFBeanException;
import org.cyberborean.rdfbeans.test.RDFBeansTestBase;
import org.eclipse.rdf4j.model.IRI;
import org.junit.Before;
import org.junit.Test;

public class Issue40Test extends RDFBeansTestBase {
	
	IRI graphA;
	IRI graphB;
	
	@Before
	public void setUp() throws Exception {		
		graphA = manager.getRepositoryConnection().getValueFactory().createIRI("urn:contexts:A");
		graphB = manager.getRepositoryConnection().getValueFactory().createIRI("urn:contexts:B");
	}

	@Test
    public void test() throws Exception { 
		A a = manager.getContext(graphA).create("a1", A.class);
		
		B bNull = a.getB();
		
		B b = manager.getContext(graphB).create("b1", B.class);		
		b.setText("Hello World!");
		
		try {
			a.setB(b);
			fail();
		}
		catch (RDFBeanException e) {}
			
	}
	
	
	@RDFBean("http://rdfbeans.cyberborean.org/test/A")
	public static interface A {
		
		@RDFSubject(prefix = "id:a:")
		String getId();
		
		@RDF("http://rdfbeans.cyberborean.org/test/bProp")
		B getB();
		void setB(B b);
	}
	
	@RDFBean("http://rdfbeans.cyberborean.org/test/B")
	public static interface B {
		
		@RDFSubject(prefix = "id:b:")
		String getId();
		
		@RDF("http://rdfbeans.cyberborean.org/test/text")
		String getText();
		void setText(String s);
	}
	
}
