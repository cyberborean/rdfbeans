
package org.cyberborean.rdfbeans.reflect;

import java.beans.PropertyDescriptor;

import org.cyberborean.rdfbeans.annotations.RDFSubject;
import org.cyberborean.rdfbeans.exceptions.RDFBeanException;
import org.cyberborean.rdfbeans.exceptions.RDFBeanValidationException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class SubjectProperty extends AbstractRDFBeanProperty {

	private String prefix;
	private RDFBeanInfo rdfBeanInfo;

	/**
	 * @param propertyDescriptor
	 * @param rdfBeanInfo
	 * @param annotation
	 * @throws RDFBeanValidationException
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
		IRI iri;
		try {
			iri = SimpleValueFactory.getInstance().createIRI(v.toString());
		}
		catch (IllegalArgumentException iae) {
			throw new RDFBeanException(
					"RDF subject value must be an absolute valid URI: " + v);
		}
		super.setValue(rdfBean, getUriPart(iri));
	}

	public String getPrefix() {
		return prefix;
	}

	public IRI getUri(String uriPart) throws RDFBeanException {
		try {
			return SimpleValueFactory.getInstance().createIRI(prefix + uriPart);
		}
		catch (IllegalArgumentException iae) {
			throw new RDFBeanException(
					"RDF subject value must be an absolute valid URI: "
							+ (prefix + uriPart));
		}
	}

	public String getUriPart(IRI subjectUri) {
		if (!prefix.isEmpty()) {
			return subjectUri.stringValue().substring(prefix.length());
		}
		return subjectUri.stringValue();
	}

}
