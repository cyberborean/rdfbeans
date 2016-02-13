package com.viceversatech.rdfbeans.test.foafexample.entities.impl;


import java.util.Set;

import com.viceversatech.rdfbeans.test.foafexample.entities.IDocument;
import com.viceversatech.rdfbeans.test.foafexample.entities.IPerson;

public final class Person extends Agent implements IPerson {    
	
	
	String id;
    String[] nick;
    Set<IPerson> knows;
    
    
    /* (non-Javadoc)
	 * @see com.viceversatech.rdfbeans.test.entities.foaf.IPerson#getId()
	 */
	public String getId() {
		return id;
	}
	    
	/* (non-Javadoc)
	 * @see com.viceversatech.rdfbeans.test.entities.foaf.IPerson#setId(java.lang.String)
	 */
	public void setId(String id) {
		this.id = id;
		//setUri("http://rdfbeans.viceversatech.com/test-ontology/persons-1/" + id);
	}
     
    
	/* (non-Javadoc)
	 * @see com.viceversatech.rdfbeans.test.entities.foaf.IPerson#getNick()
	 */
    public String[] getNick() {
        return nick;
    }
	
    /* (non-Javadoc)
	 * @see com.viceversatech.rdfbeans.test.entities.foaf.IPerson#setNick(java.lang.String[])
	 */
    public void setNick(String[] nick) {
        this.nick = nick;
    }
    
    /* (non-Javadoc)
	 * @see com.viceversatech.rdfbeans.test.entities.foaf.IPerson#getNick(int)
	 */
    public String getNick(int i) {
    	return nick[i];
    }
    
    /* (non-Javadoc)
	 * @see com.viceversatech.rdfbeans.test.entities.foaf.IPerson#setNick(int, java.lang.String)
	 */
    public void setNick(int i, String nick) {
        this.nick[i] = nick;
    }
    
    /* (non-Javadoc)
	 * @see com.viceversatech.rdfbeans.test.entities.foaf.IPerson#getKnows()
	 */
    public Set<IPerson> getKnows() {
        return knows;
    }
    
    /* (non-Javadoc)
	 * @see com.viceversatech.rdfbeans.test.entities.foaf.IPerson#setKnows(java.util.Set)
	 */
    public void setKnows(Set<IPerson> knows) {
        this.knows = knows;
    }

	/* (non-Javadoc)
	 * @see com.viceversatech.rdfbeans.test.foafexample.entities.IPerson#getKnownBy()
	 */
	public Set<IPerson> getKnownBy() {
		// TODO Auto-generated method stub
		return null;
	}


	/* (non-Javadoc)
	 * @see com.viceversatech.rdfbeans.test.foafexample.entities.IPerson#getPublications()
	 */
	public IDocument[] getPublications() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.viceversatech.rdfbeans.test.foafexample.entities.IPerson#setPublications(com.viceversatech.rdfbeans.test.foafexample.entities.IDocument[])
	 */
	public void setPublications(IDocument[] publications) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.viceversatech.rdfbeans.test.foafexample.entities.IPerson#setPublications(int, com.viceversatech.rdfbeans.test.foafexample.entities.IDocument)
	 */
	public void setPublications(int index, IDocument publication) {
		// TODO Auto-generated method stub
		
	}

        
}
