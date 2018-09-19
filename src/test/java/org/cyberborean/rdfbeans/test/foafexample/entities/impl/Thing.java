package org.cyberborean.rdfbeans.test.foafexample.entities.impl;

import org.cyberborean.rdfbeans.test.foafexample.entities.IDocument;
import org.cyberborean.rdfbeans.test.foafexample.entities.IThing;


/**
 * Thing.
 *
 * @author alex
 *
 */

public abstract class Thing implements IThing {	    
	 
	String uri;
    String name;	 
    IDocument homepage;
    
	/* (non-Javadoc)
	 * @see org.cyberborean.rdfbeans.test.entities.foaf.IThing#getUri()
	 */
	public String getUri() {
		return uri;
	}
	/* (non-Javadoc)
	 * @see org.cyberborean.rdfbeans.test.entities.foaf.IThing#setUri(java.lang.String)
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}
		
	 /* (non-Javadoc)
	 * @see org.cyberborean.rdfbeans.test.entities.foaf.IThing#getName()
	 */
    public String getName() {
        return name;
    }
    /* (non-Javadoc)
	 * @see org.cyberborean.rdfbeans.test.entities.foaf.IThing#setName(java.lang.String)
	 */
    public void setName(String name) {
        this.name = name;
    } 
    
    /* (non-Javadoc)
	 * @see org.cyberborean.rdfbeans.test.entities.foaf.IThing#getHomepage()
	 */
	public IDocument getHomepage() {
		return homepage;
	}
    
	/* (non-Javadoc)
	 * @see org.cyberborean.rdfbeans.test.entities.foaf.IThing#setHomepage(org.cyberborean.rdfbeans.test.entities.foaf.Document)
	 */
	public void setHomepage(IDocument homepage) {
		this.homepage = homepage;
	}
    
    public int hashCode() {
        return this.getUri() != null? getUri().hashCode(): super.hashCode();
    }
    
    public boolean equals(Object o) {
        return ((o instanceof Thing) && (o.hashCode() == this.hashCode()));
    }
    
    public String toString() {
    	return this.getUri() != null? uri : super.toString();
    }

}
