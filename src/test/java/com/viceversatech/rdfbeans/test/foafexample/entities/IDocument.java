/**
 * IDocument.java
 * 
 * RDFBeans Feb 15, 2011 12:13:31 PM alex
 *
 * $Id: IDocument.java 30 2011-09-28 06:46:32Z alexeya $
 *  
 */
package com.viceversatech.rdfbeans.test.foafexample.entities;

import com.viceversatech.rdfbeans.annotations.RDF;
import com.viceversatech.rdfbeans.annotations.RDFBean;

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