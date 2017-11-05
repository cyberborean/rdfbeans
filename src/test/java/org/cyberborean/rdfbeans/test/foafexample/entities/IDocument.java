package org.cyberborean.rdfbeans.test.foafexample.entities;

import org.cyberborean.rdfbeans.annotations.RDF;
import org.cyberborean.rdfbeans.annotations.RDFBean;

/**
 * IDocument.
 *
 * @author alex
 *
 */
@RDFBean("foaf:Document")
public interface IDocument extends IThing {
	
	@RDF(inverseOf="foaf:homepage")
	IThing getOwner();
	
	@RDF(inverseOf="foaf:publications")
	IPerson getAuthor();
	
}