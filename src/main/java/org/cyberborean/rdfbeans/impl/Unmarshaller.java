package org.cyberborean.rdfbeans.impl;

import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;

import org.cyberborean.rdfbeans.datatype.DatatypeMapper;
import org.cyberborean.rdfbeans.exceptions.RDFBeanException;
import org.cyberborean.rdfbeans.reflect.RDFBeanInfo;
import org.cyberborean.rdfbeans.reflect.RDFProperty;
import org.cyberborean.rdfbeans.reflect.SubjectProperty;
import org.cyberborean.rdfbeans.util.LockKeeper;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;

public class Unmarshaller {

	private Map<IRI, Class> classCache = new HashMap<>();
	private LockKeeper locks;
	private DatatypeMapper datatypeMapper;
	private ClassLoader classLoader;

	public Unmarshaller(LockKeeper locks, DatatypeMapper dataTypeMapper, ClassLoader classLoader) {
		this.locks = locks;
		this.datatypeMapper = dataTypeMapper;
		this.classLoader = classLoader;
	}

	public <T> T unmarshal(RepositoryConnection conn, Resource resource, Class<T> cls, IRI context)
			throws RDFBeanException, RDF4JException {		
		return unmarshal(conn, resource, cls, new WeakHashMap<>(), context);
	}

	private <T> T unmarshal(RepositoryConnection conn, Resource resource, Class<T> cls,
			Map<Resource, Object> objectCache, IRI context) throws RDFBeanException, RDF4JException {

		// Check if the object is already retrieved
		T o = (T) objectCache.get(resource);
		if (o != null) {
			return o;
		}

		// acquire read lock on this resource
		ReadWriteLock lock = locks.getLock(resource);
		lock.readLock().lock();
		try {
			if (cls == null) {
				cls = (Class<T>) getBindingClass(conn, resource, context);
				if (cls == null) {
					throw new RDFBeanException("Cannot detect a binding class for " + resource.stringValue());
				}
			}
			// Instantiate RDFBean
			try {
				o = cls.newInstance();
			} catch (Exception ex) {
				throw new RDFBeanException(ex);
			}
			objectCache.put(resource, o);
			// introspect RDFBean
			RDFBeanInfo rbi = RDFBeanInfo.get(cls);
			SubjectProperty subjectProperty = rbi.getSubjectProperty();
			if ((subjectProperty != null) && !(resource instanceof BNode)) {
				String id = resource.stringValue();
				subjectProperty.setValue(o, id);
			}
			for (RDFProperty p : rbi.getProperties()) {
				// Get values
				IRI predicate = p.getUri();
				CloseableIteration<Statement, ? extends RDF4JException> statements;
				if (p.isInversionOfProperty()) {
					statements = conn.getStatements(null, predicate, resource, false, (IRI)context);
					if (!statements.hasNext()) {
						// try a container
						GraphQuery q = conn.prepareGraphQuery(QueryLanguage.SPARQL,
								"CONSTRUCT { ?subject <" + p.getUri() + "> <" + resource + "> } " + "WHERE { ?subject <"
										+ p.getUri() + "> ?container. " + "?container ?li <" + resource + ">" + " }");
						statements = q.evaluate();
					}
				} else {
					statements = conn.getStatements(resource, predicate, null, false, (IRI)context);
				}

				// Collect all values
				List<Value> values = new ArrayList<>();
				try {
					while (statements.hasNext()) {
						values.add(p.isInversionOfProperty() ? statements.next().getSubject()
								: statements.next().getObject());
					}
				} finally {
					statements.close();
				}

				if (values.isEmpty()) {
					continue;
				}

				// Determine field type
				Class fClass = p.getPropertyType();
				if (Collection.class.isAssignableFrom(fClass) || fClass.isArray()) {
					// Collection property - collect all values
					// Check if an array or interface or abstract class
					if (fClass.isArray() || List.class.equals(fClass) || AbstractList.class.equals(fClass)) {
						fClass = ArrayList.class;
					} else if (SortedSet.class.equals(fClass)) {
						fClass = TreeSet.class;
					} else if (Set.class.equals(fClass) || AbstractSet.class.equals(fClass)
							|| Collection.class.equals(fClass)) {
						fClass = HashSet.class;
					}
					// Instantiate collection
					Collection items;
					try {
						items = (Collection) fClass.newInstance();
					} catch (Exception ex) {
						throw new RDFBeanException(ex);
					}
					// Collect values
					for (Value value : values) {
						Object object = unmarshalObject(conn, value, objectCache, context);
						if (object != null) {
							if (object instanceof Collection) {
								items.addAll((Collection) object);
							} else {
								items.add(object);
							}
						}
					}
					// Assign collection property
					p.setValue(o, items);
				} else {
					// Not a collection - get the first value only
					Value value = values.iterator().next();
					Object object = unmarshalObject(conn, value, objectCache, context);
					if (object != null) {
						if ((object instanceof Collection) && ((Collection) object).iterator().hasNext()) {
							object = ((Collection) object).iterator().next();
						}
						p.setValue(o, object);
					}
				}
			}
			return o;
		} finally {
			lock.readLock().unlock();
		}
	}

	private Object unmarshalObject(RepositoryConnection conn, Value object, Map<Resource, Object> objectCache, IRI context)
			throws RDFBeanException, RDF4JException {
		if (object instanceof Literal) {
			// literal
			return datatypeMapper.getJavaObject((Literal) object);
		} else if (object instanceof BNode) {
			// Blank node - check if an RDF collection
			Resource r = (Resource) object;

			if (conn.hasStatement(r, RDF.TYPE, RDF.BAG, false, (IRI)context) || conn.hasStatement(r, RDF.TYPE, RDF.SEQ, false, (IRI)context)
					|| conn.hasStatement(r, RDF.TYPE, RDF.ALT, false, (IRI)context)) {
				// Collect all items (ordered)
				ArrayList items = new ArrayList();
				int i = 1;
				Object item;
				do {
					item = null;
					RepositoryResult<Statement> itemst = conn.getStatements((Resource) object,
							conn.getValueFactory().createIRI(RDF.NAMESPACE, "_" + i), null, false, (IRI)context);
					try {
						if (itemst.hasNext()) {
							item = unmarshalObject(conn, itemst.next().getObject(), objectCache, context);
							if (item != null) {
								items.add(item);
							}
							i++;
						}
					} finally {
						itemst.close();
					}
				} while (item != null);
				// Return collection
				return items;
			} else if (conn.hasStatement(r, RDF.FIRST, null, false, (IRI)context)) {
				// Head-Tail list, also collect all items
				ArrayList<Object> items = new ArrayList<Object>();
				addList(conn, items, r, objectCache, context);
				return items;
			}
		}

		// Resource
		Class<?> cls = null;
		try {
			cls = getBindingClass(conn, (Resource) object);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (cls != null) {
			return unmarshal(conn, (Resource) object, cls, objectCache, context);
		}

		// URI ?
		return java.net.URI.create(object.stringValue());
	}

	private void addList(RepositoryConnection conn, List<Object> list, final Resource currentHead,
			Map<Resource, Object> objectCache, IRI context) throws RDF4JException, RDFBeanException {
		// add the "first" items.
		RepositoryResult<Statement> firstStatements = conn.getStatements(currentHead, RDF.FIRST, null, false, (IRI)context);
		while (firstStatements.hasNext()) {
			// multi-headed lists are possible, but flattened here.
			Object item = unmarshalObject(conn, firstStatements.next().getObject(), objectCache, context);
			if (item != null) {
				list.add(item);
			}
		}
		firstStatements.close();

		// follow non-rdf:nil rest(s), if any.
		RepositoryResult<Statement> restStatements = conn.getStatements(currentHead, RDF.REST, null, false, (IRI)context);
		while (restStatements.hasNext()) {
			Value nextHead = restStatements.next().getObject();
			if (!RDF.NIL.equals(nextHead)) {
				if (nextHead instanceof BNode) {
					addList(conn, list, (BNode) nextHead, objectCache, context);
				}
			}
		}
		restStatements.close();
	}

	private Class<?> getBindingClass(RepositoryConnection conn, Resource r, Resource... contexts)
			throws RDFBeanException, RepositoryException {
		Class<?> cls = null;
		try (CloseableIteration<Statement, RepositoryException> ts = conn.getStatements(r, RDF.TYPE, null, false, contexts)) {
			while (cls == null && ts.hasNext()) {
				Value type = ts.next().getObject();
				if (type instanceof IRI) {
					cls = getBindingClassForType(conn, (IRI) type, contexts);
				} else {
					throw new RDFBeanException("Resource " + r.stringValue() + " has invalid RDF type "
							+ type.stringValue() + ": not a URI");
				}
			}
		}
		return cls;
	}

	private Class<?> getBindingClassForType(RepositoryConnection conn, IRI rdfType, Resource... contexts)
			throws RDFBeanException, RepositoryException {
		Class cls = classCache.get(rdfType);
		if (cls != null) {
			return cls;
		}
		String className = null;
		RepositoryResult<Statement> ts = null;
		try {
			ts = conn.getStatements(rdfType, Constants.BINDINGCLASS_PROPERTY, null, false, contexts);
			if (ts.hasNext()) {
				Value type = ts.next().getObject();
				if (type instanceof Literal) {
					className = type.stringValue();
				} else {
					throw new RDFBeanException("Value of " + Constants.BINDINGCLASS_PROPERTY.stringValue()
							+ " property must be a literal");
				}
			}
		} finally {
			if (ts != null) {
				ts.close();
			}
		}

		if (className != null) {
			try {
				cls = Class.forName(className, true, classLoader);
				classCache.put(rdfType, cls);
				return cls;
			} catch (ClassNotFoundException ex) {
				throw new RDFBeanException("Class " + className + " bound to RDF type <" + rdfType + "> is not found",
						ex);
			}
		}
		return null;
	}

	public DatatypeMapper getDatatypeMapper() {
		return datatypeMapper;
	}

	public void setDatatypeMapper(DatatypeMapper datatypeMapper) {
		this.datatypeMapper = datatypeMapper;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

}
