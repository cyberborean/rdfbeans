/**
 * 
 */
package com.viceversatech.rdfbeans.generator.rdfs;

import java.util.HashSet;
import java.util.Set;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.vocabulary.RDFS;

/**
 * @author alex
 *
 */
public class RDFSProperty extends RDFSResource {


	public RDFSProperty(Resource uri, Model model) {
		super(uri, model);
	}
	
	public Set<RDFSClass> getDomainClasses() {		
		Set<RDFSClass> classes = new HashSet<RDFSClass>();
		ClosableIterator<Statement> ci = model.findStatements(uri, RDFS.domain, Variable.ANY);
		while (ci.hasNext()) {
			Statement st = ci.next();			
			classes.add(new RDFSClass((Resource) st.getObject(), model));
		}
		ci.close();
		return classes;
	}
	
	public RDFSClass getRangeClass() {
		ClosableIterator<Statement> ci = model.findStatements(uri, RDFS.range, Variable.ANY);
		RDFSClass cls = null;
		if (ci.hasNext()) {
			Statement st = ci.next();			
			cls = new RDFSClass((Resource) st.getObject(), model);
		}
		ci.close();
		return cls;
	}
}
