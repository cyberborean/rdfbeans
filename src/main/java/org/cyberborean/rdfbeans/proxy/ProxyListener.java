/**
 * 
 */
package org.cyberborean.rdfbeans.proxy;

import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.URI;

/**
 * An RDFBeanManager listener for dynamic proxies creation and property changing
 * events.
 * 
 * @author alex
 */
public interface ProxyListener {

	/**
	 * Invoked when a RDFBean proxy object is created in the RDF model.
	 * 
	 * @param object
	 * @param cls
	 * @param resource
	 */
	void objectCreated(Object object, Class<?> cls, Resource resource);
	
	/**
	 * Invoked when a RDFBean proxy object property is changed by a setter method  
	 * 
	 * @param object
	 * @param property
	 * @param value
	 */
	void objectPropertyChanged(Object object, URI property, Object newValue);
		
}
