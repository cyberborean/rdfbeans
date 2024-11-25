package org.cyberborean.rdfbeans.reflect;

import java.beans.PropertyDescriptor;

import org.cyberborean.rdfbeans.annotations.RDF;
import org.cyberborean.rdfbeans.annotations.RDFContainer;
import org.cyberborean.rdfbeans.annotations.RDFContainer.ContainerType;
import org.cyberborean.rdfbeans.exceptions.RDFBeanValidationException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class RDFProperty extends AbstractRDFBeanProperty {

	private boolean inversionOfProperty;
	private IRI uri;
	private RDFContainer.ContainerType containerType = ContainerType.NONE;
	private RDFBeanInfo beanInfo;
	private boolean internalUri = true;
	/**
	 * @param propertyDescriptor
	 * @param rdfBeanInfo
	 * @throws RDFBeanValidationException
	 */
	public RDFProperty(PropertyDescriptor propertyDescriptor,
			RDFBeanInfo rdfBeanInfo, RDF annotation,
			RDFContainer containerAnnotation) throws RDFBeanValidationException {
		super(propertyDescriptor);
		if (annotation == null) {
			throw new RDFBeanValidationException(rdfBeanInfo.getRDFBeanClass(),
					new NullPointerException());
		}
		beanInfo = rdfBeanInfo;
		initUri(annotation);
		initContainerType(containerAnnotation);
	}

	private void initContainerType(RDFContainer containerAnnotation) throws RDFBeanValidationException {
		if (containerAnnotation != null) {
			if (inversionOfProperty) {
				throw new RDFBeanValidationException(
						RDFContainer.class.getSimpleName()
						+ " annotation  on " + propertyDescriptor.getName()
						+ " method is not allowed (\"inverseOf\" property)",
						beanInfo.getRDFBeanClass());
			}
			containerType = containerAnnotation.value();
		}
	}

	private void initUri(RDF annotation) throws RDFBeanValidationException {
		String uriValue = null;
		if ((annotation.inverseOf() != null) && !annotation.inverseOf().isEmpty()) {
			uriValue = annotation.inverseOf();
			inversionOfProperty = true;
		}
		else if ((annotation.value() != null) && !annotation.value().isEmpty()){
			uriValue = annotation.value();
		}
		if (uriValue == null) {
			throw new RDFBeanValidationException(
					"RDF property or \"inverseOf\" parameter is missing in "
							+ RDF.class.getName() + " annotation on property "
							+ propertyDescriptor.getName()
							+ "'s getter or setter", beanInfo.getRDFBeanClass());
		}
		internalUri = annotation.internal();
		try {
			uri = SimpleValueFactory.getInstance().createIRI(beanInfo.createUriString(uriValue));
		}
		catch (IllegalArgumentException iae) {
			throw new RDFBeanValidationException(
					"RDF property URI parameter of " + RDF.class.getName()
							+ " annotation on "
							+ propertyDescriptor.getName()
							+ " getter or setter must be an absolute valid URI: "
							+ uriValue, beanInfo.getRDFBeanClass(), iae);
		}
	}
	public boolean isInternalUri() {
		return internalUri;
	}

	public boolean isInversionOfProperty() {
		return inversionOfProperty;
	}
	
	public IRI getUri() {
		return uri;
	}

	public RDFContainer.ContainerType getContainerType() {
		return containerType;
	}
}
