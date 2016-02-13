/**
 * SubjectProperty.java
 * 
 * RDFBeans Feb 4, 2011 9:24:44 PM alex
 *
 * $Id: SubjectProperty.java 21 2011-04-02 09:15:34Z alexeya $
 *  
 */
package com.viceversatech.rdfbeans.reflect;

import java.beans.PropertyDescriptor;

import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;

import com.viceversatech.rdfbeans.annotations.RDFSubject;
import com.viceversatech.rdfbeans.exceptions.RDFBeanException;
import com.viceversatech.rdfbeans.exceptions.RDFBeanValidationException;

/**
 * SubjectProperty.
 * 
 * @author alex
 * 
 */
public class SubjectProperty extends AbstractRDFBeanProperty {

	private String prefix;
	private RDFBeanInfo rdfBeanInfo;

	/**
	 * @param propertyDescriptor
	 * @param rdfBeanInfo
	 * @param annotation
	 * @throws RDFBeanException
	 */
	public SubjectProperty(PropertyDescriptor propertyDescriptor,
			RDFBeanInfo rdfBeanInfo, RDFSubject annotation)
			throws RDFBeanValidationException {
		super(propertyDescriptor);
		this.rdfBeanInfo = rdfBeanInfo;
		if (annotation != null) {
			prefix = annotation.prefix();
		} else {
			throw new RDFBeanValidationException(rdfBeanInfo.getRDFBeanClass(), new NullPointerException());
		}
		if (!String.class.isAssignableFrom(propertyDescriptor.getReadMethod()
				.getReturnType())) {
			throw new RDFBeanValidationException("Return type of "
					+ propertyDescriptor.getReadMethod().getName()
					+ " method must be String.", rdfBeanInfo.getRDFBeanClass());
		}
		if ((propertyDescriptor.getWriteMethod() != null)
				&& !String.class.isAssignableFrom(propertyDescriptor
						.getWriteMethod().getParameterTypes()[0])) {
			throw new RDFBeanValidationException("Parameter type of "
					+ propertyDescriptor.getWriteMethod().getName()
					+ " method must be String.", rdfBeanInfo.getRDFBeanClass());
		}
	}

	@Override
	public Object getValue(Object rdfBean) throws RDFBeanException {
		Object value = super.getValue(rdfBean);
		if (value != null) {
			return getUri((String) value);
		}
		return null;
	}

	@Override
	public void setValue(Object rdfBean, Object v) throws RDFBeanException {
		URI uri = new URIImpl(v.toString());
		if (!uri.asJavaURI().isAbsolute()) {
			throw new RDFBeanException(
					"RDF subject value must be an absolute valid URI: " + v);
		}
		super.setValue(rdfBean, getUriPart(uri));
	}

	public String getPrefix() {
		return prefix;
	}

	public URI getUri(String uriPart) throws RDFBeanException {
		URI uri = rdfBeanInfo.createUri(prefix + uriPart);
		if (!uri.asJavaURI().isAbsolute()) {
			throw new RDFBeanException(
					"RDF subject value must be an absolute valid URI: "
							+ (prefix + uriPart));
		}
		return uri;
	}

	public String getUriPart(Resource subject) {
		if (!prefix.isEmpty()) {
			return subject.asURI().toString()
					.replace(rdfBeanInfo.createUriString(prefix), "");
		}
		return subject.asURI().toString();
	}

}
