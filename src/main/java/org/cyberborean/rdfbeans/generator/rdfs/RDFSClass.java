/**
 * 
 */
package org.cyberborean.rdfbeans.generator.rdfs;

import java.util.HashSet;
import java.util.Set;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.vocabulary.RDF;
import org.ontoware.rdf2go.vocabulary.RDFS;

/**
 * @author alex
 *
 */
public class RDFSClass extends RDFSResource {


	public RDFSClass(Resource uri, Model model) {
		super(uri, model);
	}
	
	public Set<RDFSClass> getSuperClasses() {		
		Set<RDFSClass> classes = new HashSet<RDFSClass>();
		ClosableIterator<Statement> ci = model.findStatements(uri, RDFS.subClassOf, Variable.ANY);
		while (ci.hasNext()) {
			Statement st = ci.next();			
			if (!st.getObject().equals(RDFS.Resource)) {
				classes.add(new RDFSClass((Resource) st.getObject(), model));
			}
		}
		ci.close();
		return classes;
	}
	
	public Set<RDFSClass> getSubClasses() {		
		Set<RDFSClass> classes = new HashSet<RDFSClass>();
		ClosableIterator<Statement> ci = model.findStatements(Variable.ANY, RDFS.subClassOf, uri);
		while (ci.hasNext()) {
			Statement st = ci.next();			
			classes.add(new RDFSClass(st.getSubject(), model));
		}
		ci.close();
		return classes;
	}
	
	public Set<RDFSProperty> getPropertiesInDomain() {		
		Set<RDFSProperty> props = new HashSet<RDFSProperty>();
		ClosableIterator<Statement> ci = model.findStatements(Variable.ANY, RDFS.domain, uri);
		while (ci.hasNext()) {
			Statement st = ci.next();			
			props.add(new RDFSProperty(st.getSubject(), model));
		}
		ci.close();
		return props;
	}
	
	public Set<RDFSProperty> getPropertiesInRange() {		
		Set<RDFSProperty> props = new HashSet<RDFSProperty>();
		ClosableIterator<Statement> ci = model.findStatements(Variable.ANY, RDFS.range, uri);
		while (ci.hasNext()) {
			Statement st = ci.next();			
			props.add(new RDFSProperty(st.getSubject(), model));
		}
		ci.close();
		return props;
	}
		
}
