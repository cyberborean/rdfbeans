package org.cyberborean.rdfbeans.datatype;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for standard datatypes not covered by XML-Schema
 */
public class Java {

	public static final String NAMESPACE = "http://cyberborean.org/rdfbeans/datatype/java#";
	
	public final static IRI CHAR = SimpleValueFactory.getInstance()
			.createIRI(NAMESPACE + "char");
	
	
}
