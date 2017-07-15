/**
 * IPerson.java
 * 
 * Constants Mar 22, 2011 1:11:20 PM alex
 *
 * $Id:$
 *  
 */
package org.cyberborean.rdfbeans.test.examples.entities;

import java.net.URI;
import java.util.Collection;
import java.util.Date;

import org.cyberborean.rdfbeans.annotations.RDF;
import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.annotations.RDFContainer;
import org.cyberborean.rdfbeans.annotations.RDFNamespaces;
import org.cyberborean.rdfbeans.annotations.RDFSubject;
import org.cyberborean.rdfbeans.annotations.RDFContainer.ContainerType;

@RDFNamespaces({ 
	"foaf = http://xmlns.com/foaf/0.1/",
	"persons = http://rdfbeans.viceversatech.com/test-ontology/persons/" 
})
@RDFBean("foaf:Person")
public interface IPerson {

	/** RDFBean ID property declaration */
	@RDFSubject(prefix = "persons:")
	String getId();

	/** Getters and setters for RDFBean properties */

	@RDF("foaf:name")
	String getName();

	void setName(String name);

	@RDF("foaf:mbox")
	String getEmail();

	void setEmail(String email);

	@RDF("foaf:homepage")
	URI getHomepage();

	void setHomepage(URI homepage);

	@RDF("foaf:birthday")
	Date getBirthday();

	void setBirthday(Date birthday);

	@RDF("foaf:nick")
	@RDFContainer(ContainerType.ALT)
	String[] getNick();

	void setNick(String[] nick);

	String getNick(int i);

	void setNick(int i, String nick);

	@RDF("foaf:knows")
	Collection<IPerson> getKnows();

	void setKnows(Collection<IPerson> knows);

}