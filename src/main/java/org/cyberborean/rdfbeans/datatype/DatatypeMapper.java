/**
 * DatatypeMapper.java
 * 
 * RDFBeans Feb 3, 2011 1:32:06 AM alex
 *
 * $Id: DatatypeMapper.java 21 2011-04-02 09:15:34Z alexeya $
 *  
 */
package org.cyberborean.rdfbeans.datatype;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;

/**
 * DatatypeMapper.
 *
 * @author alex
 *
 */
public interface DatatypeMapper {	
	
	/**
	 * Returns a Java object reconstructed from the given RDF 
	 * literal value.
	 * 
	 * @param value RDF plain or typed literal
	 * @return Java object or null if the literal datatype is not supported.
	 */
	Object getJavaObject(Literal value);
	
	/**
	 * Returns an RDF literal representation of the given Java object.
	 * 
	 * @param value Java object
	 * @param valueFactory 
	 * @return RDF plain or typed literal, or null if the object class is not supported
	 */
	Literal getRDFValue(Object value, ValueFactory valueFactory);
	
}
