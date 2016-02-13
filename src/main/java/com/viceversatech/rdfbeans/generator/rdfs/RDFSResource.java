/**
 * 
 */
package com.viceversatech.rdfbeans.generator.rdfs;

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
public abstract class RDFSResource {

	Resource uri;
	Model model;

	public RDFSResource(Resource uri, Model model) {
		this.uri = uri;
		this.model = model;
	}
	
	public String getLabel() {
		String s = null;
		ClosableIterator<Statement> ci = model.findStatements(uri, RDFS.label, Variable.ANY);
		if (ci.hasNext()) {
			Statement st = ci.next();	
			s = st.getObject().asLiteral().getValue();
		}
		ci.close();
		return s;
	}
	
	public String getComment() {
		String s = null;
		ClosableIterator<Statement> ci = model.findStatements(uri, RDFS.comment, Variable.ANY);
		if (ci.hasNext()) {
			Statement st = ci.next();	
			s = st.getObject().asLiteral().getValue();
		}
		ci.close();
		return s;
	}
	
	@Override
	public int hashCode() {		
		return uri.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof RDFSResource)) {
			return false;
		}
		return ((RDFSResource)obj).uri.equals(uri);
	}

	@Override
	public String toString() {
		return uri.toString();
	}

	public Resource getUri() {
		return uri;
	}
}
