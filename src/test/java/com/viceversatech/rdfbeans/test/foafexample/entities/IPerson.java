/**
 * IPerson.java
 * 
 * RDFBeans Feb 15, 2011 12:14:33 PM alex
 *
 * $Id: IPerson.java 30 2011-09-28 06:46:32Z alexeya $
 *  
 */
package com.viceversatech.rdfbeans.test.foafexample.entities;

import java.util.Set;

import com.viceversatech.rdfbeans.annotations.RDF;
import com.viceversatech.rdfbeans.annotations.RDFBean;
import com.viceversatech.rdfbeans.annotations.RDFContainer;
import com.viceversatech.rdfbeans.annotations.RDFContainer.ContainerType;
import com.viceversatech.rdfbeans.annotations.RDFNamespaces;
import com.viceversatech.rdfbeans.annotations.RDFSubject;

/**
 * IPerson.
 *
 * @author alex
 *
 */
@RDFBean("foaf:Person")
@RDFNamespaces("persons = http://rdfbeans.viceversatech.com/test-ontology/persons/")
public interface IPerson extends IAgent {
	
	@RDFSubject(prefix = "persons:")
	String getId();

	void setId(String id);

	@RDF("foaf:nick")
	@RDFContainer(ContainerType.ALT)
	String[] getNick();

	void setNick(String[] nick);

	String getNick(int i);

	void setNick(int i, String nick);

	@RDF("foaf:knows")
	//@RDFContainer(ContainerType.SEQ)
	Set<IPerson> getKnows();
	
	void setKnows(Set<IPerson> knows);
	
	@RDF(inverseOf="foaf:knows")
	Set<IPerson> getKnownBy();
	
	@RDF("foaf:publications")
	@RDFContainer(ContainerType.SEQ)
	IDocument[] getPublications();
	
	void setPublications(IDocument[] publications); 

}