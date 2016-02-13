/**
 * IAgent.java
 * 
 * RDFBeans Feb 15, 2011 12:12:25 PM alex
 *
 * $Id: IAgent.java 21 2011-04-02 09:15:34Z alexeya $
 *  
 */
package com.viceversatech.rdfbeans.test.foafexample.entities;

import java.util.Date;

import com.viceversatech.rdfbeans.annotations.RDF;
import com.viceversatech.rdfbeans.annotations.RDFBean;

/**
 * IAgent.
 *
 * @author alex
 *
 */
@RDFBean("foaf:Agent")
public interface IAgent extends IThing {

	/**
	 * @return the birthday
	 */
	@RDF("foaf:birthday")
	Date getBirthday();

	/**
	 * @param birthday the birthday to set
	 */
	void setBirthday(Date birthday);

	/**
	 * @return the mbox
	 */
	@RDF("foaf:mbox")
	String getMbox();

	/**
	 * @param mbox the mbox to set
	 */
	void setMbox(String mbox);

}