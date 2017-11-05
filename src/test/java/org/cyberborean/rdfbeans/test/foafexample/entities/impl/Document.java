package org.cyberborean.rdfbeans.test.foafexample.entities.impl;

import org.cyberborean.rdfbeans.test.foafexample.entities.IDocument;
import org.cyberborean.rdfbeans.test.foafexample.entities.IPerson;
import org.cyberborean.rdfbeans.test.foafexample.entities.IThing;


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
	 * @see org.cyberborean.rdfbeans.test.foafexample.entities.IDocument#getOwner()
	 */
	public IThing getOwner() {
		return owner;
	}

	/* (non-Javadoc)
	 * @see org.cyberborean.rdfbeans.test.foafexample.entities.IDocument#getAuthor()
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
