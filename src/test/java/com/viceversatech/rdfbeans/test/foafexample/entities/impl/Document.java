/**
 * Document.java
 * 
 * RDFBeans Jan 28, 2011 9:45:08 AM alex
 *
 * $Id: Document.java 36 2012-12-09 05:58:20Z alexeya $
 *  
 */
package com.viceversatech.rdfbeans.test.foafexample.entities.impl;

import com.viceversatech.rdfbeans.test.foafexample.entities.IDocument;
import com.viceversatech.rdfbeans.test.foafexample.entities.IPerson;
import com.viceversatech.rdfbeans.test.foafexample.entities.IThing;


/**
 * Document.
 *
 * @author alex
 *
 */

public class Document extends Thing implements IDocument {  
	
	IThing owner;
	IPerson author;

	/* (non-Javadoc)
	 * @see com.viceversatech.rdfbeans.test.foafexample.entities.IDocument#getOwner()
	 */
	public IThing getOwner() {
		return owner;
	}

	/* (non-Javadoc)
	 * @see com.viceversatech.rdfbeans.test.foafexample.entities.IDocument#getAuthor()
	 */
	public IPerson getAuthor() {
		return author;
	}

	public void setOwner(IThing owner) {
		this.owner = owner;
	}

	public void setAuthor(IPerson author) {
		this.author = author;
	}

}
