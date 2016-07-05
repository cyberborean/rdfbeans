RDFBeans Usage Guide
====================

<!-- MACRO{toc|fromDepth=2} -->

Getting started
---------------

Persisting Java objects with RDF in your application requires few easy steps:

### Make sure that your Java classes or interfaces are RDFBeans-compliant

This basically means that every class or interface you intend to use within RDFBeans framework
must obey general [JavaBeans conventions](http://en.wikipedia.org/wiki/JavaBean):
that is, the classes must have a public no-argument constructor and classes
and interfaces must declare conventionally named getter and setter methods for the fields
you plan to map to RDF properties. Serializability of the classes is not required.

### Add required RDFBeans annotations

_RDFBean type declaration_ (`@RDFBean`) declares that the class or interface
is an RDFBean mapped to specified RDF type:

```
@RDFBean("http://xmlns.com/foaf/0.1/Person")
public class Person
{ ...
```

_Property declarations_ (`@RDF`) are applied to the getter methods of
the data properties to declare their RDF counterparts:

```
@RDF("http://xmlns.com/foaf/0.1/name")
public String getName()
{ ...
```

_Identifier property declaration_ (`@RDFSubject`) is applied to a single getter method to mark
that it returns an unique String value identifying an RDFBean instance.

Identifiers in RDF are URIs, so this method must either return a valid absolute
URI, or a namespace prefix should be declared to construct the URI from the
prefix value and the String, returned by the method:

```
@RDFSubject
public String getUri()
{ ... // -- must return absolute URI
```

or

```
@RDFSubject(prefix="http://some.uri.prefix/")
public String getId()
{ ... // -- returns an arbitrary string
```

For complete specification of RDFBeans annotation conventions and examples, please refer to
[RDFBeans classes and interfaces format specification](rdfbean.html).


### Prepare your RDF storage

Set up and configure an [RDF2Go](http://semanticweb.org/wiki/RDF2Go)-supported RDF framework (Sesame or Jena) as
a back-end to store RDF representations of your RDFBeans classes.

### Go with RDFBeans

Add, retrieve, modify and delete your RDFBean objects using `RDFBeanManager` class and
RDF2Go [Model](http://mavenrepo.fzi.de/semweb4j.org/site/rdf2go.api/apidocs/org/ontoware/rdf2go/model/Model.html)
implementation, as it's shown below on this page.


Classes or interfaces?
----------------------

Before you start, you need to decide which of two databinding techniques supported by
RDFBeans framework to be used.

The first method is based on use of RDFBean classes and it is much more like
a traditional JavaBeans persistence technique. Here you declare a RDFBean class
with no-argument constructor, instantiate it and manipulate with properties data.
At any time, you can dump (marshal) the object state to the RDF model and retrieve
it from there.

The second method is based purely on the RDFBean interfaces. In this case, the
special proxy objects are created dynamically to access the RDF data directly
through RDFBean methods. Setting of a RDFBean property causes immediate update
of the RDF model and the getter methods return actual values, retrieved from
the model, so you don't have to bother about state synchronization between
your object model and RDF resources.

It is completely up to you which one method you should use in your application.
Moreover, it is perfectly ok to use both methods at the same time and on the
same RDF model.


RDFBeanManager
---------------

RDFBeans databinding functions are accessible as methods of
a single `RDFBeanManager` class. An `RDFBeanManager` instance is created with
a RDF2Go [Model](http://mavenrepo.fzi.de/semweb4j.org/site/rdf2go.api/apidocs/org/ontoware/rdf2go/model/Model.html)
which provides an abstraction layer to access an underlying physical
RDF storage. Currently, [RDF2Go](http://semanticweb.org/wiki/RDF2Go) project provides implementations of Model
interface (adapters) for Sesame 2.x and Jena frameworks.

A Model instance is passed as an argument to the `RDFBeanManager` constructor.
The Model implementations may require the model to be opened (initialized) before
and closed after use. The following example illustrates how to setup RDFBeans databinding with a
model adapter determined automatically via RDF2Go ModelFactory mechanism:

```
import org.cyberborean.rdfbeans.RDFBeanManager;
import org.ontoware.rdf2go.ModelFactory;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
...

ModelFactory modelFactory = RDF2Go.getModelFactory();
Model model = modelFactory.createModel();
model.open();
RDFBeanManager manager = new RDFBeanManager(model);
...
model.close();
```

An example with hardcoded Sesame 2.x NativeStore model implementation:

```
import org.cyberborean.rdfbeans.RDFBeanManager;
import org.ontoware.rdf2go.model.Model;
import org.openrdf.rdf2go.RepositoryModel;
import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.nativerdf.NativeStore;
...

Repository repository = new SailRepository(new NativeStore(new File("~/.sesame/test")));
repository.initialize();
Model model = new RepositoryModel(repository);
model.open();
RDFBeanManager manager = new RDFBeanManager(model);
...
model.close();
```

For detailed information on RDF2Go configuration for specific triple store
adapters, please refer to [RDF2Go documentation](http://semanticweb.org/wiki/RDF2Go#Documentation_and_Tutorial).


Working with RDFBean classes
----------------------------

### Adding instances

To add (marshall) a current state of an RDFBean object into the underlying RDF model, there is
`add()` method:

```
Person person = new Person();
person.setId("http://example.com/persons/jdoe");
person.setName("John Doe");
...

Resource r = manager.add(person);
```

If the RDFBean is not anonymous (i.e. has a not-null property annotated with
`@RDFSubject` - see [RDFBean format](rdfbean.html)), the method returns
[org.ontoware.rdf2go.model.node.Resource](http://mavenrepo.fzi.de/semweb4j.org/site/rdf2go.api/apidocs/org/ontoware/rdf2go/model/node/Resource.html)
object, representing an URI of RDF resource created in the
model. Otherwise, the Resource result represents a
[org.ontoware.rdf2go.model.node.BlankNode](http://mavenrepo.fzi.de/semweb4j.org/site/rdf2go.api/apidocs/org/ontoware/rdf2go/model/node/BlankNode.html)
 instance.


### Updating instances

`update()` method synchronizes the state of an RDFBean instance and the
state of its RDF representation of in the model:

```
...
person.setEmail("john.doe@example.com");
Resource r = manager.update(person);
```

The code above sets new property of our Person instance
and updates it in the RDF model, thus synchronizing the RDF resource with the
actual object state. Note that this method works properly only with unanonymous
RDFBeans - otherwise, it would insert another BlankNode into the model (i.e.
has the same behaviour as `add()`).

### Retrieving instances

RDFBean class instances can be retrieved (unmarshalled) from their RDF representations
using `get()` method:

```
...
Person person2 = manager.get("http://example.com/persons/jdoe", Person.class);
```

This method returns new instance of specified RDFBean class filled with data
retrieved from the RDF model.

In the example above, we assume that the RDFBean identifier has been declared
without namespace prefix. Otherwise, we would have to pass only the local part
of the identifier URI as an argument:

```
... // assuming 'http://example.com/persons/' is a prefix
Person person2 = manager.get("jdoe", Person.class);
```

The `get()` method returns `null` if no RDF resource matching the given
RDFBean identifier and class is found.

If we already have an RDF resource URI or a BlankNode (returned by `add()`
or `update()` methods for instance), we can retrive the matching RDFBean instance using
that value:

```
...
Person person2 = (Person) manager.get(r);
```

The `getAll()` method iterates over all RDFBeans of a specified class in the
model:

```
// Displays the names of all Persons in the model:
ClosableIterator iter = manager.getAll(Person.class);
while (iter.hasNext()) {
   Person p = (Person)iter.next();
   System.out.println(p.getName());
}
iter.close();
```


Working with RDFBean interfaces
-------------------------------

This is an alternative mapping technique which is based on using RDFBean interfaces
and dynamic proxy objects.

Let's extract the interface from our RDFBean `Person` class and
annotate it the same way:

```
@RDFBean("http://xmlns.com/foaf/0.1/Person")
public interface IPerson {

    @RDFSubject
    String getId();

    @RDF("http://xmlns.com/foaf/0.1/name")
    String getName();

    ...
```

As we have this interface, we can map it directly to an RDF resource in the model.
For that, we need to obtain an instance of the dynamic proxy object from
RDFBeanManager:

```
IPerson person = manager.create("http://example.com/persons/jdoe", IPerson.class);
```

The `create()` method returns an object implementing our `IPerson`
interface and matching the RDFBean with the specified identifier in the model.
This proxy object has the same behaviour as an instance of the class implementing
that interface, with the exception that all its data is stored not in the
Java heap, but directly in the underlying RDF model. So, the call:

```
person.setName("John Doe");
```

will cause a statement with RDF property `http://xmlns.com/foaf/0.1/name`
and the given literal value to be created or updated in the model. Similarly, the
`getName()` method of the proxy would return this value addressing directly
to that statement.

If no resource matching the specified RDFBean identifier exists in the
model, `create()` returns an empty instance, that is all its `getXXX()`
methods return nulls. The underlying RDF model will be unchanged until the first
`setXXX()` method is called.

To instantiate the proxy objects for existing RDFBeans, the `create()`
method with a Resource argument can be used. The method returns null if the
specified Resource doesn't exist in the model:

```
IPerson person = manager.create(r);
```

The reference to the Resource can be obtained with `getResource()` method
(see {Resolving RDFBean identifiers} below), or by querying the underlying
RDF model (see Querying the model).


Managing RDFBeans in the model
------------------------------

### Resolving RDFBean identifiers

If an RDFBean identifier is known, it can be resolved to an RDF resource
using the identifier's String value and RDFBean class:

```
...
Resource r = manager.getResource("http://example.com/persons/jdoe", Person.class);
```

or, if a namespace prefix is set for identifiers:

```
... // assuming 'http://example.com/persons/' is a prefix
Resource r = manager.getResource("jdoe", Person.class);
```

The method returns null if no matching Resource is found in the model.

### Deleting RDFBeans

RDFBean representation can be deleted from RDF model with `delete()` method:

```
...
manager.delete("http://example.com/persons/jdoe", Person.class);
```

or

```
...
Resource r = manager.getResource("http://example.com/persons/jdoe", Person.class);
if (r != null) {
    manager.delete(r);
}
```


Advanced topics
---------------

### Cascade databinding

`add()` and `update()` methods performs cascade databinding,
that is if a marshalling object has other RDFBeans as its properties, they
are marshalled and added to the model recursively.
It allows to manipulate with the whole complex object models with
single method calls.

For example, let the `Person` class declares a `knows` property (probably,
mapped to `foaf:knows`) to link Persons one another (for simplicity, put
cardinality of this property to 1):

```
Person john = new Person();
john.setId("http://example.com/persons/john");
...
Person mary = new Person();
mary.setId("http://example.com/persons/mary");
...
john.setKnows(mary);
```

Calling `manager.add(john)` will result in adding both objects
(`john` and `mary`) and generating a statement to relate their RDF
representations with the `knows` property.

Similarly, on retrieving the `john` object, the `mary` RDFBean
will be automatically unmarshalled and assigned to `john.knows` property,
thus reconstructing the whole graph of related objects.

The dynamic proxies follow the same principle: when a getter method is
called and the property references to another RDFBean, the proxy object is automatically
created and returned:

```
IPerson john = manager.create("http://example.com/persons/john", IPerson.class);
IPerson mary = manager.create("http://example.com/persons/mary", IPerson.class);
john.setKnows(mary);
...
john.getKnows().setName("Mary"); // getKnows() returns the proxy equals to mary
System.out.println(mary.getName()); // -- will be "Mary"
```

### Transactions

By default, each call of `add()`, `update()` or `delete()` methods will
commit the transaction into a RDF store after an operation is completed.
This behaviour can be changed by setting the autocommit mode to false with
`setAutocommit()` method. If autocommit is off, the transactions can be
commited by explicit calling of the `commit()` method of a underlying
RDF2Go Model interface:

```
// Adding multiple RDFBeans as a single transaction:
manager.setAutocommit(false);
for (Person p: persons) {
  manager.add(p);
}
manager.getModel().commit();
```

Note that the autocommit mode in RDFBeanManager is unrelated to the
autocommit settings of the underlying model. RDFBeanManager operations always set
the autocommit flag of the model to false to avoid commiting every single RDF
triple.

If dynamic proxies are used and autocommit mode is on, every setter method call
will commit a transaction. You can turn the autocommit off for manual transaction
management:

```
manager.setAutocommit(false);
IPerson john = manager.create("http://example.com/persons/john", IPerson.class);
john.setName("John");
System.out.println(john.getName()); // -- will be null
manager.getModel().commit();
System.out.println(john.getName()); // -- and now it is "John"
```

### Querying the model

RDFBeans framework does not include its own facilities to query the RDF model,
but it is possible to query the underlying RDF2Go model and retrieve RDFBean
objects from results.

Example of querying with the triple patterns:

```
// Find all persons with Google as a workplace homepage:

Set<Person> results = new HashSet<Person>();
URI predicate = model.createURI("http://xmlns.com/foaf/0.1/workplaceHomepage");
URI object = model.createURI("http://google.com");
ClosableIterator<Statement> ci = model.findStatements(Variable.ANY, predicate, object);
while (ci.hasNext()) {
  Resource r = ci.next().getSubject();
  results.add((Person)manager.get(r));
}
ci.close();
```

Example of using Sparql:

```
Set<Person> results = new HashSet<Person>();
ClosableIterator<QueryRow> ci = model.sparqlSelect(
  "SELECT ?r WHERE { ?r <http://xmlns.com/foaf/0.1/workplaceHomepage> <http://google.com> . }"
).iterator();
while (ci.hasNext()) {
  Resource r = ci.next().getValue("r").asResource();
  results.add((Person)manager.get(r));
}
ci.close();
```

