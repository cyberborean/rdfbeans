/**
 * 
 */
package org.cyberborean.rdfbeans.generator.rdfs;

import java.util.HashSet;
import java.util.Set;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.vocabulary.RDF;
import org.ontoware.rdf2go.vocabulary.RDFS;

/**
 * @author alex
 *
 */
public class RDFSModel {
	
	Model model;
	
	public RDFSModel(Model model) {
		this.model = model;
	}
	
	public Set<RDFSClass> getClasses() {
		Set<RDFSClass> classes = new HashSet<RDFSClass>();
		ClosableIterator<Statement> ci = model.findStatements(Variable.ANY, RDF.type, RDFS.Class);
		while (ci.hasNext()) {
			Statement st = ci.next();
			classes.add(new RDFSClass(st.getSubject(), model));
		}
		ci.close();
		return classes;
	}

}
