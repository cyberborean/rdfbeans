package org.cyberborean.rdfbeans.exceptions;


public class RDFBeanValidationException extends RDFBeanException {

	Class rdfBeanClass = null;
	
	public Class getRDFBeanClass() {
		return rdfBeanClass;
	}

	public RDFBeanValidationException(String string, Class rdfBeanClass, Throwable cause) {
		super(rdfBeanClass.getName() + ": " + string, cause);
		this.rdfBeanClass = rdfBeanClass;
	}
	
	public RDFBeanValidationException(Class rdfBeanClass, Throwable cause) {
		this("", rdfBeanClass, cause);
	}

	public RDFBeanValidationException(String string, Class rdfBeanClass) {
		this(string, rdfBeanClass, null);
	}

}
