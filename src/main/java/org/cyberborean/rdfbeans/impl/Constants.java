package org.cyberborean.rdfbeans.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class Constants {

	public static final IRI BINDINGCLASS_PROPERTY = SimpleValueFactory.getInstance().createIRI(
			"http://viceversatech.com/rdfbeans/2.0/bindingClass");
	
	public static final IRI BINDINGIFACE_PROPERTY = SimpleValueFactory.getInstance().createIRI(
			"http://viceversatech.com/rdfbeans/2.0/bindingIface");
	
}
