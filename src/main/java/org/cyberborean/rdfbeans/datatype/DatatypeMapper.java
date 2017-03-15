
package org.cyberborean.rdfbeans.datatype;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;

/**
 * DatatypeMapper.
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
