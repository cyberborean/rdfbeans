package org.cyberborean.rdfbeans.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;

import org.cyberborean.rdfbeans.annotations.RDFContainer.ContainerType;
import org.cyberborean.rdfbeans.datatype.DatatypeMapper;
import org.cyberborean.rdfbeans.exceptions.RDFBeanException;
import org.cyberborean.rdfbeans.exceptions.RDFBeanValidationException;
import org.cyberborean.rdfbeans.reflect.RDFBeanInfo;
import org.cyberborean.rdfbeans.reflect.RDFProperty;
import org.cyberborean.rdfbeans.reflect.SubjectProperty;
import org.cyberborean.rdfbeans.util.LockKeeper;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

public class Marshaller {

	private final LockKeeper locks;
	private DatatypeMapper datatypeMapper;

	public Marshaller(LockKeeper locks, DatatypeMapper dataTypeMapper) {
		this.locks = locks;
		this.datatypeMapper = dataTypeMapper;
	}

	public Resource marshal(RepositoryConnection conn, Object o, boolean update, IRI context) throws RDFBeanException, RepositoryException {
		return marshal(conn, o, update, new WeakHashMap<>(), context);
	}
	
	private Resource marshal(RepositoryConnection conn, Object o, boolean update, Map<Object, Resource> resourceCache, IRI context) throws RDFBeanException, RepositoryException {
		// Check if object is already marshalled
		Resource subject = resourceCache.get(o);
		if (subject != null && !update) {
			// return cached node
			return subject;
		}

		// introspect RDFBEan
		Class cls = o.getClass();
		RDFBeanInfo rbi = RDFBeanInfo.get(cls);
		SubjectProperty sp = rbi.getSubjectProperty();
		if (sp != null) {
			Object value = sp.getValue(o);
			if (value != null) {
				subject = (IRI) value;
			} else {
				// NOP no pb, will create blank node
			}
		}
		
		if (subject == null) {
			// Blank node
			subject = conn.getValueFactory().createBNode();
		}

		// acquire write lock on this resource
		ReadWriteLock lock = locks.getLock(subject);
		lock.writeLock().lock();
		try {			
			 if (!(subject instanceof BNode) && conn.hasStatement(subject, null, null, false, (IRI)context)) {
				// Resource is already in the model
				if (update) {
					// Remove existing triples
					conn.remove(subject, null, null, (IRI)context);
				} else {
					// Will not be added
					return subject;
				}
			}

			// Add subject to cache
			resourceCache.put(o, subject);

			// Add rdf:type
			IRI type = rbi.getRDFType();
			conn.add(subject, RDF.TYPE, type, (IRI)context);
			conn.add(type, Constants.BINDINGCLASS_PROPERTY, conn.getValueFactory().createLiteral(cls.getName()), (IRI)context);
			addSuperClassTypes(conn, rbi, context);
			// Add properties
			for (RDFProperty p : rbi.getProperties()) {
				IRI predicate = p.getUri();
				Object value = p.getValue(o);
				if (p.isInversionOfProperty()) {
					conn.remove((Resource) null, predicate, subject, (IRI)context);
				}
				if (value != null) {
					if (isCollection(value)) {
						// Collection
						Collection values = (Collection) value;
						if (p.getContainerType() == ContainerType.NONE) {
							// Create multiple triples
							for (Object v : values) {
								Value object = toRdf(conn, v, resourceCache, context);
								if (object != null) {
									if (p.isInversionOfProperty()) {
										if (object instanceof Resource) {
											ReadWriteLock invLock = null;
											if (!object.equals(subject)) {
												invLock = locks.getLock(object);
												invLock.writeLock().lock();
											}
											try {												
												conn.add((Resource) object, predicate, subject, (IRI)context);
											}
											finally {
												if (invLock != null) {
													invLock.writeLock().unlock();
												}
											}
										} else {
											throw new RDFBeanException("Value of the \"inverseOf\" property "
													+ p.getPropertyDescriptor().getName() + " of class "
													+ rbi.getRDFBeanClass().getName() + " must be of "
													+ "an RDFBean type (was: " + object.getClass().getName() + ")");
										}
									} else {
										conn.add(subject, predicate, object, (IRI)context);
									}
								}
							}
						} else if (p.getContainerType() == ContainerType.LIST) {
							if (p.isInversionOfProperty()) {
								throw new RDFBeanException(
										"RDF container type is not allowed for a \"inverseOf\" property "
												+ p.getPropertyDescriptor().getName() + " of class "
												+ rbi.getRDFBeanClass().getName());
							}
							marshalLinkedList(conn, values, subject, p, resourceCache, context);
						} else {
							if (!p.isInversionOfProperty()) {
								// Create RDF Container bNode
								IRI ctype = RDF.BAG;
								if (p.getContainerType() == ContainerType.SEQ) {
									ctype = RDF.SEQ;
								} else if (p.getContainerType() == ContainerType.ALT) {
									ctype = RDF.ALT;
								}
								BNode collection = conn.getValueFactory().createBNode();
								conn.add(collection, RDF.TYPE, ctype, (IRI)context);
								int i = 1;
								for (Object v : values) {
									Value object = toRdf(conn, v, resourceCache, context);
									if (object != null) {
										conn.add(collection, conn.getValueFactory().createIRI(RDF.NAMESPACE, "_" + i++),
												object, (IRI)context);
									}
								}
								conn.add(subject, predicate, collection, (IRI)context);
							} else {
								throw new RDFBeanException(
										"RDF container type is not allowed for a \"inverseOf\" property "
												+ p.getPropertyDescriptor().getName() + " of class "
												+ rbi.getRDFBeanClass().getName());
							}
						}
					} else {
						// Single value
						Value object = toRdf(conn, value, resourceCache, context);
						if (object != null) {
							if ( value instanceof java.net.URI && !p.isInternalUri()) {
								object =conn.getValueFactory().createLiteral(value.toString(), conn.getValueFactory().createIRI("http://www.w3.org/2001/XMLSchema#anyURI"));
							}
							if (p.isInversionOfProperty()) {
								if (object instanceof Resource) {
									ReadWriteLock invLock = null;
									if (!object.equals(subject)) {
										invLock = locks.getLock(object);
										invLock.writeLock().lock();
									}
									try {												
										conn.add((Resource) object, predicate, subject, (IRI)context);
									}
									finally {
										if (invLock != null) {
											invLock.writeLock().unlock();
										}
									}
									
								} else {
									throw new RDFBeanException(
											"Value of the \"inverseOf\" property " + p.getPropertyDescriptor().getName()
													+ " of class " + rbi.getRDFBeanClass().getName() + " must be of "
													+ "an RDFBean type (was: " + object.getClass().getName() + ")");
								}
							} else {
								conn.add(subject, predicate, object, (IRI)context);
							}
						}
					}
				}
			}
			return subject;
		} finally {
			lock.writeLock().unlock();
		}
	}

	private void marshalLinkedList(RepositoryConnection conn, Collection values, Resource subject, RDFProperty property, Map<Object, Resource> resourceCache, IRI context)
			throws RDFBeanException, RepositoryException {
		BNode listHead = conn.getValueFactory().createBNode();
		conn.add(subject, property.getUri(), listHead, (IRI)context);
		Iterator<Object> value = values.iterator();
		do {
			if (value.hasNext()) {
				Value valueNode = toRdf(conn, value.next(), resourceCache, context);
				conn.add(listHead, RDF.FIRST, valueNode, (IRI)context);
			}
			if (value.hasNext()) {
				BNode newHead = conn.getValueFactory().createBNode();
				conn.add(listHead, RDF.REST, newHead, (IRI)context);
				listHead = newHead;
			} else {
				conn.add(listHead, RDF.REST, RDF.NIL, (IRI)context);
			}
		} while (value.hasNext());
	}

	private Value toRdf(RepositoryConnection conn, Object value, Map<Object, Resource> resourceCache, IRI context) throws RDFBeanException, RepositoryException {
		// Check if another RDFBean
		if (RDFBeanInfo.isRdfBean(value)) {
			return marshal(conn, value, false, resourceCache, context);
		}
		// Check if URI
		if (java.net.URI.class.isAssignableFrom(value.getClass())) {
			return conn.getValueFactory().createIRI(value.toString());
		}
		// Check if a Literal
		Literal l = datatypeMapper.getRDFValue(value, conn.getValueFactory());
		if (l != null) {
			return l;
		}
		throw new RDFBeanException(
				"Unsupported class [" + value.getClass().getName() + "] of value " + value.toString());
	}

	private static boolean isCollection(Object value) {
		return value instanceof Collection;
	}

	public DatatypeMapper getDatatypeMapper() {
		return datatypeMapper;
	}

	public void setDatatypeMapper(DatatypeMapper dataTypeMapper) {
		this.datatypeMapper = dataTypeMapper;
	}
	

	private void addSuperClassTypes(RepositoryConnection conn, RDFBeanInfo rbi, IRI context) throws RDFBeanValidationException {
		Class<?> superClass = rbi.getRDFBeanClass().getSuperclass();
		if (superClass != null && RDFBeanInfo.isRdfBeanClass(superClass)) {
			RDFBeanInfo superRbi = RDFBeanInfo.get(superClass);
			if (superRbi != null) {
				conn.add(rbi.getRDFType(), RDFS.SUBCLASSOF, superRbi.getRDFType(), (IRI)context);
				addSuperClassTypes(conn, superRbi, context);
			}
		}
	}
}
