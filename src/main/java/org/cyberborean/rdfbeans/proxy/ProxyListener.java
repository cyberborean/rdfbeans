package org.cyberborean.rdfbeans.proxy;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;

/**
 * An RDFBeanManager listener for dynamic proxies creation and property changing
 * events.
 * 
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
	 * @param newValue
	 */
	void objectPropertyChanged(Object object, IRI property, Object newValue);
}
