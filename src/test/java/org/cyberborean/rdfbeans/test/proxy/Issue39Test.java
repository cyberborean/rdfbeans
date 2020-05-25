package org.cyberborean.rdfbeans.test.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.cyberborean.rdfbeans.annotations.RDF;
import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.annotations.RDFContainer;
import org.cyberborean.rdfbeans.annotations.RDFContainer.ContainerType;
import org.cyberborean.rdfbeans.annotations.RDFSubject;
import org.cyberborean.rdfbeans.test.RDFBeansTestBase;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.junit.Test;

public class Issue39Test extends RDFBeansTestBase {

	@Test
	public void test() {
		IRI iri = manager.getRepositoryConnection().getValueFactory().createIRI("urn:test:test1");
		
		TestIface test = manager.create(iri, TestIface.class);
		String[] strings = new String[] {"one", "two", "three"};		
		test.setStrings(strings);
		
		IRI prop = manager.getRepositoryConnection().getValueFactory().createIRI("http://rdfbeans.cyberborean.org/test/strings");
		BNode bNode1 = (BNode) manager.getRepositoryConnection().getStatements(iri, prop, null).next().getObject();
		assertEquals(4, countChildStatements(bNode1));
		
		strings = new String[] {"apples", "oranges", "bananas"};
		test.setStrings(strings);
		
		BNode bNode2 = (BNode) manager.getRepositoryConnection().getStatements(iri, prop, null).next().getObject();
		assertEquals(4, countChildStatements(bNode2));
		assertFalse(bNode2.getID().equals(bNode1.getID()));
		
		// old BNode must be removed at this point
		assertFalse(manager.getRepositoryConnection().hasStatement(iri, prop, bNode1, false));		
		assertEquals(0, countChildStatements(bNode1));
	}
	
	private int countChildStatements(BNode bNode) {
		RepositoryResult<Statement> stmts = manager.getRepositoryConnection().getStatements(bNode, null, null);
		int count = 0;
		while (stmts.hasNext()) {
			stmts.next();
			count++;
		}
		return count;
	}

	@RDFBean("http://rdfbeans.cyberborean.org/test/1")
	public static interface TestIface {
		
		@RDFSubject
		String getUri();
		
		@RDFContainer(ContainerType.SEQ)
		@RDF("http://rdfbeans.cyberborean.org/test/strings")
		String[] getStrings();
		void setStrings(String[] strings);
	}
}
