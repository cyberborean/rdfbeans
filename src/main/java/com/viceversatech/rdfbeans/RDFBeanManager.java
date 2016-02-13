/**
 * RDFBeanManager.java
 * 
 * RDFBeans Mar 2, 2011 9:45:17 AM alex
 *
 * $Id:$
 *  
 */
package com.viceversatech.rdfbeans;

import java.lang.reflect.Proxy;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.WeakHashMap;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.Literal;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.model.node.impl.URIImpl;

import com.viceversatech.rdfbeans.annotations.RDFContainer.ContainerType;
import com.viceversatech.rdfbeans.annotations.RDFSubject;
import com.viceversatech.rdfbeans.datatype.DatatypeMapper;
import com.viceversatech.rdfbeans.datatype.DefaultDatatypeMapper;
import com.viceversatech.rdfbeans.exceptions.RDFBeanException;
import com.viceversatech.rdfbeans.exceptions.RDFBeanValidationException;
import com.viceversatech.rdfbeans.proxy.ProxyInstancesPool;
import com.viceversatech.rdfbeans.proxy.ProxyListener;
import com.viceversatech.rdfbeans.proxy.RDFBeanDelegator;
import com.viceversatech.rdfbeans.reflect.RDFBeanInfo;
import com.viceversatech.rdfbeans.reflect.RDFProperty;
import com.viceversatech.rdfbeans.reflect.SubjectProperty;

/**
 * 
 * RDFBeans databinding functions are accessible as methods of a single
 * RDFBeanManager class. An RDFBeanManager instance is created with a RDF2Go
 * Model which provides an abstraction layer to access an underlying physical
 * RDF storage. Currently, RDF2Go project provides implementations of Model
 * interface (adapters) for Sesame 2.x and Jena frameworks.
 * 
 * A Model instance is passed as an argument to the RDFBeanManager constructor.
 * The Model implementations may require the model to be opened (initialized)
 * before and closed after use. The following example illustrates how to setup
 * RDFBeans databinding with a model adapter determined automatically via RDF2Go
 * ModelFactory mechanism:
 * 
 * <pre>
 * import com.viceversatech.rdfbeans.RDFBeanManager; 
 * import org.ontoware.rdf2go.ModelFactory; 
 * import org.ontoware.rdf2go.RDF2Go; 
 * import org.ontoware.rdf2go.model.Model; 
 * ...
 * 
 * ModelFactory modelFactory = RDF2Go.getModelFactory(); 
 * Model model = modelFactory.createModel(); 
 * model.open(); 
 * RDFBeanManager manager = new RDFBeanManager(model); 
 * ... 
 * model.close();
 * </pre>
 * 
 * An example with hardcoded Sesame 2.x NativeStore model implementation:
 * 
 * <pre>
 * import com.viceversatech.rdfbeans.RDFBeanManager; 
 * import org.ontoware.rdf2go.model.Model; 
 * import org.openrdf.rdf2go.RepositoryModel;
 * import org.openrdf.repository.Repository; 
 * import org.openrdf.repository.sail.SailRepository; 
 * import org.openrdf.sail.nativerdf.NativeStore; 
 * ...
 * 
 * Repository repository = new SailRepository(new NativeStore(new File("~/.sesame/test"))); 
 * repository.initialize(); 
 * Model model = new RepositoryModel(repository); 
 * model.open(); 
 * RDFBeanManager manager = new RDFBeanManager(model); 
 * ... 
 * model.close();
 * </pre>
 * 
 * For detailed information on RDF2Go configuration for specific triple store
 * adapters, please refer to RDF2Go documentation.
 * 
 * 
 * @author alex
 * @version $Id:$
 * 
 */
public class RDFBeanManager {

	public static final URI BINDINGCLASS_PROPERTY = new URIImpl(
			"http://viceversatech.com/rdfbeans/2.0/bindingClass");
	public static final URI BINDINGIFACE_PROPERTY = new URIImpl(
			"http://viceversatech.com/rdfbeans/2.0/bindingIface");

	private Model model;
	private boolean autocommit = true;
	private ClassLoader classLoader;
	private DatatypeMapper datatypeMapper = new DefaultDatatypeMapper();

	private WeakHashMap<Object, Resource> resourceCache;
	private WeakHashMap<Resource, Object> objectCache;
	private ProxyInstancesPool proxies;
	private List<ProxyListener> proxyListeners = new Vector<ProxyListener>();

	/**
	 * Creates new RDFBeanManager instance upon the given RDF2Go model.
	 * 
	 * @param model
	 */
	public RDFBeanManager(Model model) {
		this.model = model;
		this.classLoader = this.getClass().getClassLoader();
		this.proxies = new ProxyInstancesPool(this);
	}

	// ====================== RDFBean classes functionality ====================

	/**
	 * Marshall the state of an RDFBean object to an RDF resource (a set of
	 * triple statements) in the underlying RDF model.
	 * 
	 * <p>
	 * If the RDFBean object has not-null property, annotated with
	 * {@link RDFSubject}, the method returns absolute URI of RDF resource.
	 * Otherwise, RDF BlankNode is returned.
	 * 
	 * <p>
	 * If an RDF representation of the given unanonymous object already exists
	 * in the model, the method immediately returns the RDF resource without
	 * changing the model.
	 * 
	 * <p>
	 * If autocommit mode is on (see {@link setAutocommit(boolean)}), the
	 * statements are commited into the RDF model in a single transaction.
	 * Otherwise, the transaction is delayed until the <code>commit()</code>
	 * method of the underlying Model implementation is invoked.
	 * 
	 * @param o
	 *            RDFBean to add
	 * @return Resource URI or BlankNode for anonymous RDFBean
	 * @throws RDFBeanException
	 *             If the object is not a valid RDFBean
	 * 
	 * @see update(Object)
	 * @see setAutocommit(boolean)
	 */
	public synchronized Resource add(Object o) throws RDFBeanException {
		return addOrUpdate(o, false);
	}

	/**
	 * Marshall the state of an RDFBean object to update an existing RDF
	 * resource in the underlying RDF model.
	 * 
	 * <p>
	 * If no resource for the given object exists, or the object is anonymous
	 * RDFBean represented with a {@link BlankNode}, the method works like
	 * {@link #add(Object) add()}.
	 * 
	 * <p>
	 * If autocommit mode is on (see {@link setAutocommit(boolean)}), the
	 * statements are commited into the RDF model in a single transaction.
	 * Otherwise, the transaction is delayed until the <code>commit()</code>
	 * method of the underlying Model implementation is invoked.
	 * 
	 * @param o
	 *            RDFBean to update
	 * @return Resource URI or BlankNode for anonymous RDFBean
	 * @throws RDFBeanException
	 *             If the object is not a valid RDFBean
	 * 
	 * @see add(Object)
	 * @see setAutocommit(boolean)
	 */
	public synchronized Resource update(Object o) throws RDFBeanException {
		return addOrUpdate(o, true);
	}

	/**
	 * Unmarshall an RDF resource to an instance of the specified RDFBean class.
	 * 
	 * @param r
	 *            Resource URI (or BlankNode for anonymous RDFBeans).
	 * @param rdfBeanClass
	 *            Java class of RDFBean
	 * @return Unmarshalled RDFBean object, or null if the resource does not
	 *         exists
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean or an instance of this
	 *             class cannot be created
	 * @see get(Resource)
	 * @see get(String,Class)
	 * @see getAll(Class)
	 */

	public <T> T get(Resource r, Class<T> rdfBeanClass) throws RDFBeanException {
		if (!this.isResourceExist(r)) {
			return null;
		}
		return this._get(r, rdfBeanClass);
	}

	/**
	 * 
	 * Unmarshall an RDF resource to an RDFBean instance.
	 * 
	 * <p>
	 * The method tries to autodetect an RDFBean Java class using information
	 * added to the model at marshalling. If a binding class information is not
	 * found, RDFBeanException is thrown.
	 * 
	 * @param r
	 *            Resource URI or BlankNode for anonymous RDFBeans.
	 * @return Unmarshalled RDFBean object, or null if the resource does not
	 *         exists in the model
	 * @throws RDFBeanException
	 *             If the binding class cannot be detected, is not a valid
	 *             RDFBean or an instance of this class cannot be created
	 * 
	 * @see get(Resource,Class)
	 * @see get(String,Class)
	 * @see getAll(Class)
	 */
	public Object get(Resource r) throws RDFBeanException {
		if (!this.isResourceExist(r)) {
			return null;
		}
		Class<?> cls = getBindingClass(r);
		if (cls == null) {
			throw new RDFBeanException("Cannot detect a binding class for "
					+ r.asURI().toString());
		}
		return this._get(r, cls);
	}

	/**
	 * Unmarshall an RDF resource matching specified RDFBean identifier to an
	 * instance of the specified RDFBean class.
	 * 
	 * <p>
	 * If a namespace prefix is defined in {@link RDFSubject} declaration for
	 * this RDFBean class, the provided identifier value is interpreted as a
	 * local part of fully qualified RDFBean name (RDF resource URI). Otherwise,
	 * the fully qualified name must be provided.
	 * 
	 * @param stringId
	 *            RDFBean ID value
	 * @param rdfBeanClass
	 *            Java class of RDFBean
	 * @return The unmarshalled Java object, or null no resource matching the
	 *         given ID is found exists
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean or an instance of this
	 *             class cannot be created
	 * @see get(Resource)
	 * @see get(Resource,Class)
	 * @see getAll(Class)
	 */
	public <T> T get(String stringId, Class<T> rdfBeanClass)
			throws RDFBeanException {
		Resource r = this.getResource(stringId, rdfBeanClass);
		if (r != null) {
			return this.get(r, rdfBeanClass);
		}
		return null;
	}

	/**
	 * Obtain an iterator over all instances of specified RDFBean class stored
	 * in the RDF model
	 * 
	 * <p>
	 * The returned Iterator performs lazy unmarshalling of RDFBean objects (on
	 * every <code>next()</code> call) without any specific order. When
	 * iterating is done, the caller must invoke the <code>close()</code> method
	 * of ClosableIterator to release the resources of the underlying RDF model
	 * implementation.
	 * 
	 * @param rdfBeanClass
	 *            Java class of RDFBeans
	 * @return Iterator over RDFBean instances
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean
	 */
	public <T> ClosableIterator<T> getAll(final Class<T> rdfBeanClass)
			throws RDFBeanException {
		RDFBeanInfo rbi = RDFBeanInfo.get(rdfBeanClass);
		URI type = rbi.getRDFType();
		if (type == null) {
			return new ClosableIterator<T>() {
				public void close() {
				}

				public boolean hasNext() {
					return false;
				}

				public T next() {
					return null;
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
		final ClosableIterator<Statement> sts = model.findStatements(
				Variable.ANY, org.ontoware.rdf2go.vocabulary.RDF.type, type);
		return new ClosableIterator<T>() {
			public boolean hasNext() {
				return sts.hasNext();
			}

			public T next() {
				try {
					return _get(sts.next().getSubject(), rdfBeanClass);
				} catch (RDFBeanException ex) {
					throw new RuntimeException(ex);
				}
			}

			public void close() {
				sts.close();
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * Check if a RDF resource exists in the underlying model.
	 * 
	 * @param r
	 *            Resource URI or BlankNode
	 * @return true, if the model contains the statements with the given
	 *         subject.
	 */
	public boolean isResourceExist(Resource r) {
		return model.contains(r, Variable.ANY, Variable.ANY);
	}
	
	public boolean isResourceExist(Resource r, Class rdfBeanClass) throws RDFBeanValidationException {
		RDFBeanInfo rbi = RDFBeanInfo.get(rdfBeanClass);
		URI type = rbi.getRDFType();
		return (type != null) &&
				isResourceExist(r) &&				
				model.contains(r, org.ontoware.rdf2go.vocabulary.RDF.type, type);
	}

	/**
	 * Resolve the RDFBean identifier to an RDF resource URI.
	 * 
	 * <p>
	 * If a namespace prefix is defined in {@link RDFSubject} declaration for
	 * this RDFBean class, the provided identifier value is interpreted as a
	 * local part of fully qualified RDFBean name (RDF resource URI). Otherwise,
	 * the fully qualified name must be provided.
	 * 
	 * @param stringId
	 *            RDFBean ID value
	 * @param rdfBeanClass
	 *            Java class of RDFBean
	 * @return Resource URI, or null if no resource matching the given RDFBean
	 *         ID found.
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean
	 */
	public Resource getResource(String stringId, Class rdfBeanClass)
			throws RDFBeanException {
		SubjectProperty subject = RDFBeanInfo.get(rdfBeanClass)
				.getSubjectProperty();
		if (subject != null) {
			URI r = subject.getUri(stringId);
			if (isResourceExist(r)) {
				return r;
			}
		}
		return null;
	}

	/**
	 * Delete the RDF resource from underlying model.
	 * 
	 * <p>
	 * If autocommit mode is on (see {@link setAutocommit(boolean)}), the
	 * statements are removed from the RDF model as a single transaction.
	 * Otherwise, the transaction is delayed until the <code>commit()</code>
	 * method of the underlying Model implementation is invoked.
	 * 
	 * @param r
	 *            Resource URI
	 * @see delete(String,Class)
	 * @see setAutocommit(boolean)
	 */
	public void delete(Resource r) {
		model.setAutocommit(false);
		removeResource(r);
		if (this.autocommit) {
			model.commit();
		}
	}

	/**
	 * Delete an RDF resource matching the specified RDFBean identifier from
	 * underlying model.
	 * 
	 * <p>
	 * If autocommit mode is on (see {@link setAutocommit(boolean)}), the
	 * statements are removed from the RDF model as a single transaction.
	 * Otherwise, the transaction is delayed until the <code>commit()</code>
	 * method of the underlying Model implementation is invoked.
	 * 
	 * @param stringId
	 *            RDFBean ID value
	 * @param rdfBeanClass
	 *            Java class of RDFBean
	 * @throws RDFBeanException
	 *             If the class is not a valid RDFBean
	 * 
	 * @see delete(Resource)
	 * @see setAutocommit(boolean)
	 */
	public void delete(String stringId, Class rdfBeanClass)
			throws RDFBeanException {
		Resource r = this.getResource(stringId, rdfBeanClass);
		if (r != null) {
			this.delete(r);
		}
	}

	private synchronized Resource addOrUpdate(Object o, boolean update)
			throws RDFBeanException {
		this.resourceCache = new WeakHashMap<Object, Resource>();
		model.setAutocommit(false);
		Resource node = marshal(o, update);
		if (this.autocommit) {
			model.commit();
		}
		return node;
	}

	private synchronized Resource marshal(Object o, boolean update)
			throws RDFBeanException {
		// Check if object is already marshalled
		Resource subject = this.resourceCache.get(o);
		if (subject != null) {
			// return cached node
			return subject;
		}
		// Indentify RDF type
		Class cls = o.getClass();
		RDFBeanInfo rbi = RDFBeanInfo.get(cls);
		URI type = rbi.getRDFType();
		model.addStatement(type, BINDINGCLASS_PROPERTY,
				model.createPlainLiteral(cls.getName()));
		// introspect RDFBEan
		SubjectProperty sp = rbi.getSubjectProperty();
		if (sp != null) {
			Object value = sp.getValue(o);
			if (value != null) {
				subject = (URI) value;
			} 
			else {
				// NOP no pb, will create blank node
			}
		}
		if (subject == null) {
			// Blank node
			subject = model.createBlankNode();
		} else if (model.contains(subject, Variable.ANY, Variable.ANY)) {
			// Resource is already in the model
			if (update) {
				// Remove existing triples
				model.removeStatements(subject, Variable.ANY, Variable.ANY);
			} 
			else {
				// Will not be added
				return subject;
			}
		}
		// Add subject to cache
		this.resourceCache.put(o, subject);
		// Add rdf:type
		model.addStatement(subject, org.ontoware.rdf2go.vocabulary.RDF.type,
				type);
		// Add properties
		for (RDFProperty p : rbi.getProperties()) {
			URI predicate = p.getUri();
			Object value = p.getValue(o);
			if (p.isInversionOfProperty()) {
				model.removeStatements(Variable.ANY, predicate, subject);
			}
			if (value != null) {				
				if (isCollection(value)) {
					// Collection
					Collection values = (Collection) value;
					if (p.getContainerType() == ContainerType.NONE) {
						// Create multiple triples
						for (Object v : values) {
							Node object = createNode(v);
							if (object != null) {								
								if (p.isInversionOfProperty()) {
									if (object instanceof Resource) {										
										model.addStatement((Resource)object, predicate, subject);
									}
									else {
										throw new RDFBeanException("Value of the \"inverseOf\" property " + 
												p.getPropertyDescriptor().getName() + " of class " + 
												rbi.getRDFBeanClass().getName() + " must be of " +
												"an RDFBean type (was: " + object.getClass().getName() + ")");
									}
								}
								else {
									model.addStatement(subject, predicate, object);
								}
							}
						}
					} 
					else {
						if (!p.isInversionOfProperty()) {
							// Create RDF Container bNode
							URI ctype = org.ontoware.rdf2go.vocabulary.RDF.Bag;
							if (p.getContainerType() == ContainerType.SEQ) {
								ctype = org.ontoware.rdf2go.vocabulary.RDF.Seq;
							} else if (p.getContainerType() == ContainerType.ALT) {
								ctype = org.ontoware.rdf2go.vocabulary.RDF.Alt;
							}
							BlankNode collection = model.createBlankNode();
							model.addStatement(collection,
									org.ontoware.rdf2go.vocabulary.RDF.type, ctype);
							int i = 0;
							for (Object v : values) {
								Node object = createNode(v);
								if (object != null) {
									model.addStatement(collection,
											org.ontoware.rdf2go.vocabulary.RDF
													.li(i), object);
									i++;
								}
							}
							model.addStatement(subject, predicate, collection);
						}
						else {
							throw new RDFBeanException("RDF container type is not allowed for a \"inverseOf\" property " +
									p.getPropertyDescriptor().getName() + " of class " + 
									rbi.getRDFBeanClass().getName());
						}
					}
				} 
				else {
					// Single value
					Node object = createNode(value);
					if (object != null) {
						if (p.isInversionOfProperty()) {
							if (object instanceof Resource) {
								model.addStatement((Resource)object, predicate, subject);
							}
							else {
								throw new RDFBeanException("Value of the \"inverseOf\" property " + 
										p.getPropertyDescriptor().getName() + " of class " + 
										rbi.getRDFBeanClass().getName() + " must be of " +
										"an RDFBean type (was: " + object.getClass().getName() + ")");
							}
						}
						else {
							model.addStatement(subject, predicate, object);
						}
					}
				}
			}
		}
		return subject;
	}

	private synchronized Node createNode(Object value)
			throws RDFBeanException {
		// 1. Check if a Literal
		Literal l = getDatatypeMapper().getRDFValue(value, model);
		if (l != null) {
			return l;
		}
		// 2. Check if another RDFBean
		if (RDFBeanInfo.isRdfBean(value)) {
			return marshal(value, false);
		}
		// 3. Check if URI
		if (java.net.URI.class.isAssignableFrom(value.getClass())) {
			return model.createURI(((java.net.URI) value).toString());
		}
		// No RDF binding for this value
		System.err.println("Cannot bind value: " + value);
		return null;
	}

	private static boolean isCollection(Object value) {
		return value instanceof Collection;
	}

	private Class<?> getBindingClass(Resource r) throws RDFBeanException {
		Class<?> cls = null;
		ClosableIterator<Statement> ts = model.findStatements(r,
				org.ontoware.rdf2go.vocabulary.RDF.type, Variable.ANY);
		if (ts.hasNext()) {
			cls = this.getBindingClass(ts.next().getObject().asURI());
		}
		ts.close();
		return cls;
	}

	private Class<?> getBindingClass(URI rdfType) throws RDFBeanException {
		String className = null;
		ClosableIterator<Statement> cs = model.findStatements(rdfType,
				BINDINGCLASS_PROPERTY, Variable.ANY);
		if (cs.hasNext()) {
			className = cs.next().getObject().asLiteral().getValue();
		}
		cs.close();
		if (className != null) {
			try {
				return Class.forName(className, true, classLoader);
			} catch (ClassNotFoundException ex) {
				throw new RDFBeanException("Class " + className
						+ " bound to RDF type <" + rdfType + "> is not found",
						ex);
			}
		}
		return null;
	}

	private <T> T _get(Resource r, Class<T> cls) throws RDFBeanException {
		this.objectCache = new WeakHashMap<Resource, Object>();
		// Unmarshal the resource
		return unmarshal(r, cls);
	}

	private <T> T unmarshal(Resource resource, Class<T> cls)
			throws RDFBeanException {
		// Check if the object is already retrieved
		T o = (T) this.objectCache.get(resource);
		if (o != null) {
			return o;
		}
		// Instantiate RDFBean
		try {
			o = cls.newInstance();
		} catch (Exception ex) {
			throw new RDFBeanException(ex);
		}
		this.objectCache.put(resource, o);
		// introspect RDFBean
		RDFBeanInfo rbi = RDFBeanInfo.get(cls);
		SubjectProperty subjectProperty = rbi.getSubjectProperty();
		if ((subjectProperty != null) && !(resource instanceof BlankNode)) {
			String id = resource.asURI().toString();
			subjectProperty.setValue(o, id);
		}
		for (RDFProperty p : rbi.getProperties()) {
			// Get values
			URI predicate = p.getUri();
			ClosableIterator<Statement> statements;
			if (p.isInversionOfProperty()) {
				statements = model.findStatements(Variable.ANY, predicate, resource);
				if (!statements.hasNext()) {
					// try a container
					statements = model.sparqlConstruct("CONSTRUCT { ?subject <" + p.getUri() + "> <" + resource + "> } " + 
										  "WHERE { ?subject <" + p.getUri() + "> ?container. " +
										  		  "?container ?li <" + resource + ">" +
										  		" }").iterator();
				}
			}
			else {
				statements = model.findStatements(resource, predicate, Variable.ANY);				
			}

			//Collect all values
			List<Node> values = new ArrayList<Node>();
			while (statements.hasNext()) {
				values.add(p.isInversionOfProperty()? statements.next().getSubject() : statements.next().getObject());
			}
			statements.close();
			
			if (values.isEmpty()) {				
				continue;
			}
			
			// Determine field type
			Class fClass = p.getPropertyType();
			if (Collection.class.isAssignableFrom(fClass) || fClass.isArray()) {
				// Collection property - collect all values
				// Check if an array or interface or abstract class
				if (fClass.isArray() || List.class.equals(fClass)
						|| AbstractList.class.equals(fClass)) {
					fClass = ArrayList.class;
				} else if (SortedSet.class.equals(fClass)) {
					fClass = TreeSet.class;
				} else if (Set.class.equals(fClass)
						|| AbstractSet.class.equals(fClass)
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
				for (Node value: values) {
					Object object = unmarshalObject(value);
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
			} 
			else {
				// Not a collection - get the first value only
				Node value = values.iterator().next();
				Object object = unmarshalObject(value);
				if (object != null) {
					if ((object instanceof Collection)
							&& ((Collection) object).iterator().hasNext()) {
						object = ((Collection) object).iterator().next();
					}
					p.setValue(o, object);
				}				
			}
		}
		return o;
	}

	private Object unmarshalObject(Node object) throws RDFBeanException {
		if (object instanceof Literal) {
			// literal
			return getDatatypeMapper().getJavaObject(object.asLiteral());
		} else if (object instanceof BlankNode) {
			// Blank node - check if an RDF collection
			Resource r = (Resource) object;
			if (model.contains(r, org.ontoware.rdf2go.vocabulary.RDF.type,
					org.ontoware.rdf2go.vocabulary.RDF.Alt)
					|| model.contains(r,
							org.ontoware.rdf2go.vocabulary.RDF.type,
							org.ontoware.rdf2go.vocabulary.RDF.Bag)
					|| model.contains(r,
							org.ontoware.rdf2go.vocabulary.RDF.type,
							org.ontoware.rdf2go.vocabulary.RDF.Seq)) {
				// Collect all items (ordered)
				ArrayList items = new ArrayList();
				int i = 0;
				Object item = null;
				do {
					item = null;
					ClosableIterator<Statement> itemst = model.findStatements(
							(Resource) object,
							org.ontoware.rdf2go.vocabulary.RDF.li(i),
							Variable.ANY);
					if (itemst.hasNext()) {
						item = unmarshalObject(itemst.next().getObject());
						if (item != null) {
							items.add(item);
						}
						i++;
					}
					itemst.close();
				} while (item != null);
				// Return collection
				return items;
			}
		}
		// Resource
		/*
		Class cls = getBindingClass((Resource) object);
		if (cls != null) {
			return unmarshal((Resource) object, cls);
		}
		throw new RDFBeanException("Unknown binding class for resource " + object.toString());
		*/
				
		Class cls = null;
		try {
			cls = getBindingClass((Resource) object);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (cls != null) {
			return unmarshal((Resource) object, cls);
		}
		
		//URI
		return object.asURI().asJavaURI();
	}

	private void removeResource(Resource resource) {
		/*ClosableIterator<Statement> sts = model.findStatements(subject,
				Variable.ANY, Variable.ANY);
		while (sts.hasNext()) {
			model.removeStatement(sts.next());
		}
		sts.close();*/

		// remove where is a subject 
		model.removeStatements(resource, Variable.ANY, Variable.ANY);
		
		// remove where is an object
		model.removeStatements(Variable.ANY, Variable.ANY, resource);
		
		proxies.purge(resource);
		
	}

	// ================== RDFBean dynamic proxy functionality ==================

	/**
	 * Create new dynamic proxy instance that implements the specified RDFBean
	 * interface and backed by the specified RDF resource in the underlying
	 * RDF model.
	 * 
	 * @param r
	 *            Resource URI
	 * @param iface
	 *            RDFBean interface
	 * @return New RDFBean dynamic proxy object with the specified interface
	 * @throws RDFBeanException
	 *             If iface is not valid RDFBean interface
	 *             
	 * @see create(String,Class)
	 * @see create(Resource)
	 */

	public <T> T create(Resource r, Class<T> iface) throws RDFBeanException {
		return createInternal(r, RDFBeanInfo.get(iface), iface) ;
	}

	/**
	 * Create new dynamic proxy instance that implements the specified RDFBean
	 * interface and backed by an RDF resource matching to the given RDFBean ID.
	 * 
	 * <p>
	 * If a namespace prefix is defined in {@link RDFSubject} declaration for
	 * this RDFBean interface, the provided identifier value is interpreted as a
	 * local part of fully qualified RDFBean name (RDF resource URI). Otherwise,
	 * the fully qualified name must be provided.
	 * 
	 * @param id
	 *            RDFBean ID value
	 * @param iface
	 *            RDFBean interface
	 * @return New RDFBean dynamic proxy object with the specified interface
	 * @throws RDFBeanException
	 *             if iface is not valid RDFBean interface or there is an error
	 *             resolving RDFBean identifier
	 *             
	 * @see create(Resource,Class)
	 * @see create(Resource)
	 */

	public <T> T create(String id, Class<T> iface) throws RDFBeanException {
		RDFBeanInfo rbi = RDFBeanInfo.get(iface);
		URI uri = resolveUri(id, rbi);
		if (uri == null) {
			throw new RDFBeanException("Cannot resolve RDFBean ID: " + id);
		}
		if (!uri.asJavaURI().isAbsolute()) {
			throw new RDFBeanException(
					"RDF subject value must be an absolute valid URI: " + uri);
		}
		return createInternal(uri, rbi, iface);
	}
	
	private <T> T createInternal(Resource r, RDFBeanInfo rbi, Class<T> iface) throws RDFBeanException {
		boolean newObject = false;
		if (!isResourceExist(r)) {
			model.addStatement(r, org.ontoware.rdf2go.vocabulary.RDF.type, rbi.getRDFType());
			model.addStatement(rbi.getRDFType(), RDFBeanManager.BINDINGIFACE_PROPERTY, 
				model.createPlainLiteral(rbi.getRDFBeanClass().getName()));
			newObject = true;
		}
		T obj = proxies.getInstance(r, rbi, iface);
		if (newObject) {
			fireObjectCreated(obj, iface, r);
		}
		return obj;
	}

	/**
	 * Create new dynamic proxy instance backed by an existing RDF resource.
	 * 
	 * <p>
	 * The method tries to autodetect an RDFBean Java interface using
	 * information retrieved from the model. If a binding interface information
	 * is not found, RDFBeanException is thrown.
	 * 
	 * @param r
	 *            Resource URI
	 * @return New RDFBean dynamic proxy object or null if the resource does not
	 *         exist or is not bound to any RDFBean interface
	 * @throws RDFBeanException
	 *             If interface bound to the RDF type is not found
	 *             
	 * @see create(Resource,Class)
	 * @see create(String,Class)
	 */

	public Object create(Resource r) throws RDFBeanException {
		Class<?> iface = getBindingIface(r);
		if (iface != null) {
			return create(r, iface);
		}
		return null;
	}
	
	/**
	 * Returns a collection of dynamic proxy instances for existing RDF resources
	 * 
	 * @param iface
	 * 			RDFBean interface
	 * @return Collection of dynamic proxy objects with specified interface
	 * @throws RDFBeanException
	 */
	public <T> Collection<T> createAll(Class<T> iface) throws RDFBeanException {
		RDFBeanInfo rbi = RDFBeanInfo.get(iface);
		URI type = rbi.getRDFType();
		Collection<T> result = new HashSet<T>(); 
		if (type == null) {
			return result;
		}
		ClosableIterator<Statement> sts = model.findStatements(
				Variable.ANY, org.ontoware.rdf2go.vocabulary.RDF.type, type);
		try {
			while (sts.hasNext()) {
				T proxy = createInternal(sts.next().getSubject(), rbi, iface);
				result.add(proxy);
			}
		}
		finally {
			sts.close();
		}		
		return result;
	}

	protected Class<?> getBindingIface(Resource r) throws RDFBeanException {
		Class<?> cls = null;
		ClosableIterator<Statement> ts = model.findStatements(r,
				org.ontoware.rdf2go.vocabulary.RDF.type, Variable.ANY);
		if (ts.hasNext()) {
			cls = this.getBindingIface(ts.next().getObject().asURI());
		}
		ts.close();
		return cls;
	}

	protected Class<?> getBindingIface(URI rdfType) throws RDFBeanException {
		String className = null;
		ClosableIterator<Statement> cs = model.findStatements(rdfType,
				BINDINGIFACE_PROPERTY, Variable.ANY);
		if (cs.hasNext()) {
			className = cs.next().getObject().asLiteral().getValue();
		}
		cs.close();
		if (className != null) {
			try {
				return Class.forName(className, true, classLoader);
			} catch (ClassNotFoundException ex) {
				throw new RDFBeanException("Interface " + className
						+ " bound to RDF type <" + rdfType
						+ "> cannot be located by the classloader", ex);
			}
		}
		return null;
	}

	private URI resolveUri(String id, RDFBeanInfo rbi) throws RDFBeanException {
		URI uri = model.createURI(id);
		if (!uri.asJavaURI().isAbsolute()) {
			SubjectProperty sp = rbi.getSubjectProperty();
			if (sp != null) {
				uri = sp.getUri(id);
			}
		}
		return uri;
	}

	// ============================ Common methods =============================

	/**
	 * Return the underlying RDF model.
	 * 
	 * @return the model
	 */
	public Model getModel() {
		return model;
	}

	/**
	 * Check if autocommit mode is on
	 * 
	 * <p>
	 * If autocommit mode is on, the transactions with the RDF model will be 
	 * immediately commited on invocation of {@link add(Object)}, {@link update(Object)} and 
	 * {@link delete(Resource)} methods, as well as of the setter methods of the
	 * dynamic proxy objects. Otherwise, the transactions must be commited by explicit
	 * invocation of the <code>commit()</code> method of the Model implementation.
	 * 
	 * <p>
	 * By default, the autocommit mode is on.
	 * 
	 * @return True if autocommit mode is on.
	 * 
	 * @see setAutocommit(boolean)
	 */
	public boolean isAutocommit() {
		return autocommit;
	}

	/**
	 * Set autocommit mode.
	 * 
	 * <p>
	 * If autocommit mode is on, the transactions with the RDF model will be 
	 * immediately commited on invocation of {@link add(Object)}, {@link update(Object)} and 
	 * {@link delete(Resource)} methods, as well as of the setter methods of the
	 * dynamic proxy objects. Otherwise, the transactions must be commited by explicit
	 * invocation of the <code>commit()</code> method of the Model implementation.
	 * 
	 * <p>
	 * By default, the autocommit mode is on.
	 *  
	 * @param autocommit
	 *            false to set the autocommit mode off or true to on
	 *            
	 * @see isAutocommit()
	 */
	public void setAutocommit(boolean autocommit) {
		this.autocommit = autocommit;
	}

	/**
	 * Return the current ClassLoader for loading RDFBean classes.
	 * 
	 * <p>
	 * By default, the classes are loaded by the ClassLoader of this RDFBeanManager.  
	 * 
	 * @return the current ClassLoader instance
	 * 
	 * @see setClassLoader(ClassLoader)
	 */
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	/**
	 * Set a custom ClassLoader instance for loading RDFBean classes.
	 * 
	 * <p>
	 * By default, the classes are loaded by the ClassLoader of this RDFBeanManager.
	 *   
	 * @param classLoader
	 *            the ClassLoader instance to set
	 *            
	 * @see getClassLoader()           
	 */
	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Return a current DatatypeMapper implementation.
	 * 
	 * @return the datatypeMapper
	 * 
	 * @see setDatatypeMapper(DatatypeMapper)
	 */
	public DatatypeMapper getDatatypeMapper() {
		return datatypeMapper;
	}

	/**
	 * Set a DatatypeMapper implementation.
	 * 
	 * @param datatypeMapper
	 *            the datatypeMapper to set
	 * 
	 * @see getDatatypeMapper()
	 */
	public void setDatatypeMapper(DatatypeMapper datatypeMapper) {
		this.datatypeMapper = datatypeMapper;
	}
	
	public void addProxyListener(ProxyListener l) {
		this.proxyListeners.add(l);
	}
	
	public void removeProxyListener(ProxyListener l) {
		this.proxyListeners.remove(l);
	}
	
	public List<ProxyListener> getProxyListeners() {
		return Collections.unmodifiableList(proxyListeners);
	}
	
	private void fireObjectCreated(Object object, Class<?> cls, Resource resource) {
		for (ProxyListener l : getProxyListeners()) {
			l.objectCreated(object, cls, resource);
		}
	}	

}
