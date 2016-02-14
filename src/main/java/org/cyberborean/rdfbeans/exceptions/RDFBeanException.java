package org.cyberborean.rdfbeans.exceptions;

/**
 * 
 * 
 * 
 */
public class RDFBeanException extends Exception {

    /**
     * @param string
     */
    public RDFBeanException(String string) {
        super(string);
    }
    
    public RDFBeanException(String string, Throwable cause) {
        super(string, cause);
    }
    
    public RDFBeanException(Throwable cause) {
        super(cause);
    }
}
