/**
 * IThing.java
 * 
 * RDFBeans Feb 15, 2011 12:11:16 PM alex
 *
 * $Id: IThing.java 21 2011-04-02 09:15:34Z alexeya $
 *  
 */
package com.viceversatech.rdfbeans.test.foafexample.entities;

import com.viceversatech.rdfbeans.annotations.RDF;
import com.viceversatech.rdfbeans.annotations.RDFBean;
import com.viceversatech.rdfbeans.annotations.RDFNamespaces;
import com.viceversatech.rdfbeans.annotations.RDFSubject;

/**
 * IThing.
 *
 * @author alex
 *
 */
@RDFNamespaces({
	"owl = http://www.w3.org/2002/07/owl#", 
	"foaf = http://xmlns.com/foaf/0.1/"
	})
@RDFBean("owl:Thing")
public interface IThing {

	/**
	 * @return the uri
	 */
	@RDFSubject
	String getUri();

	/**
	 * @param uri the uri to set
	 */
	void setUri(String uri);

	/**
	 * @return the name
	 */
	@RDF("foaf:name")
	String getName();

	/**
	 * @param name the name to set
	 */
	void setName(String name);

	/**
	 * @return the homepage
	 */
	@RDF("foaf:homepage")
	IDocument getHomepage();

	/**
	 * @param homepage the homepage to set
	 */
	void setHomepage(IDocument homepage);

}