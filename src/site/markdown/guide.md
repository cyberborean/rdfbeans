Programming with RDFBeans
=========================

<!-- MACRO{toc|class=toc} -->

In this quick guide, we go through the basics of RDFBeans and will learn how to use RDFBean annotations and `RDFBeanManager` class to develop
Java applications with object models backed by RDF triples. 

Getting started
---------------

### Classes or interfaces?

Before you start, it is good to know about two RDF databinding techniques provided by
RDFBeans framework.

The first method is based on use of RDFBean classes and it is much more like
a traditional JavaBeans persistence technique. Here you declare a Java class with [RDFBeans annotations](rdfbean.html) and
the no-argument constructor, instantiate it and manipulate with the object data.
At any time, you can dump (marshal) or retrieve (unmarshal) the object state to/from the RDF model.

You can sub-class your RDFBean classes to create complex polymorphic object models, like you usually do in OOP. 
The RDFBeans-annotated properties will be inherited down through the levels of your class hierarchy.  

You may also want to extract interfaces from your classes to separate method signatures from their implementations.
In this case, all RDFBeans annotations can be moved to the interfaces so that the implementing classes would derive annotations implicitly.   

This is where the second RDFBeans databinding method comes into play. If you
have interfaces specifying RDFBean classes in your object model, you don't need their actual implementation classes.
Instead of this, the RDFBeans framework offers the special dynamic proxy mechanism for direct manipulation of RDF data
via annotated RDFBean interfaces. Under the hood, the framework uses the special [proxy objects](http://docs.oracle.com/javase/8/docs/technotes/guides/reflection/proxy.html) which are created dynamically at runtime
to access the RDF data through the interface methods. Setting of a RDFBean property causes immediate update
of the RDF model and the getter methods return actual values, retrieved from
the model, so you don't have to bother about state synchronization between
your objects in the Java heap and in the RDF model.

It is completely up to you which one method you should use in your application.
Moreover, it is perfectly ok to use both methods at the same time and on the
same RDF model.

### Make sure that your Java classes and/or interfaces are RDFBeans-compliant

This basically means that every class or interface to use within RDFBeans framework
must obey general [JavaBeans conventions](http://en.wikipedia.org/wiki/JavaBean):

 * The classes have a public no-argument constructor. 
 
 * Classes and interfaces declare conventionally named getter and setter methods for the fields
you plan to map to RDF properties.

### Available property types

The following Java data types are supported by RDFBeans framework and allowed for RDFBean properties:

* `java.lang.String`
* `boolean` / `java.lang.Boolean`
* `int` / `java.lang.Integer`
* `float` / `java.lang.Float`
* `double` / `java.lang.Double`
* `byte` / `java.lang.Byte`
* `long` / `java.lang.Long`
* `short` / `java.lang.Short`
* `java.util.Date`
* `java.net.URI`

Besides of those, an RDFBean property can be declared as:

* A Collection or an array of the above types
* Any RDFBeans class or interface
 

### Add required RDFBeans annotations

__RDFBean type declaration__ (`@RDFBean`) declares that the class or interface
is an RDFBean mapped to specified RDF type (commonly, a reference to an RDF-Schema class):

```java
@RDFBean("http://xmlns.com/foaf/0.1/Person")
public class Person
{ 
    // instances of this class will represent RDF resources of the foaf:Person type 
    ...
```

__Identifier property declaration__ (`@RDFSubject`) is applied to a single getter method to specify
that it returns an unique String value identifying the RDFBean instance. This value will be used to construct
an URI of the RDF resource that will represent this instance in the RDF model (a subject part of all data statements). 

This method may return a valid absolute URI string:

```java
@RDFSubject
public String getUri()
{   
    // must return absolute URI
    return uri;
}
```

Alternatively, you can declare a namespace prefix to construct the URI from an arbitrary String, returned by this method:

```
@RDFSubject(prefix="urn:some.uri.prefix/")
public String getId()
{ 
    // resource URI will be constructed by concatenation of the prefix and returned value
    return id;
}
```

__Property declarations__ (`@RDF`) are applied to the getter methods of
the data properties to declare their RDF counterparts (predicates):

```java
@RDF("http://xmlns.com/foaf/0.1/name")
public String getName()
{ 
   // this property will be represented as an RDF statement with the foaf:name predicate
   return name;
}
```

For complete specification of RDFBeans annotation conventions and examples, please refer to
[RDFBeans classes and interfaces format specification](rdfbean.html).


### Prepare your RDF storage

Use [RDF4J API](http://docs.rdf4j.org/) to set up and configure a [repository](http://docs.rdf4j.org/programming/#_the_repository_api)
 (an RDF database) that will be used to store RDF representations of your RDFBean objects.

In the examples below, we will use a `SailRepository` class that provides an interface to [various RDF databases](http://rdf4j.org/rdf4j-databases/) using adapters, known in RDF4J as SAIL 
(Storage and Inferencing Layer) objects.

For example, here's how to create a repository backed by a simple in-memory SAIL store:

```java
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
...    
Repository repo = new SailRepository(new MemoryStore());
repo.initialize();
```

... and a persistent on-disk "Native RDF" repository:

```java
...
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
...
File dataDir = new File("C:\\temp\\myRepository\\");      
Repository repo = new SailRepository(new NativeStore(dataDir));
repo.initialize();
```

Along with these two storage back-ends, native to RDF4J, there is a number of SAIL implementations to provide an interface to [third-party RDF databases](http://rdf4j.org/rdf4j-databases/#Third-party_RDF4J_databases). For detailed information about configuration of different repository types, please refer to 
[RDF4J documentation](http://docs.rdf4j.org/programming/#_the_repository_api).

In the end, don't forget to close the `Repository` object using `shutDown()` method:

```java
...
repo.initialize();
try {
    ...
}
finally {
    repo.shutDown();
}
```
 

RDFBeanManager
---------------

RDFBean management functions are accessible as methods of
the `RDFBeanManager` class. An `RDFBeanManager` instance is created with
a RDF4J `Repository` object that provides access to the unrelying RDF store.

```java
...
import org.cyberborean.rdfbeans.RDFBeanManager;
...
RDFBeanManager manager = new RDFBeanManager(repo);
```

The `Repository` object does not have to be initialized at this point, but it is required to invoke `repo.initialize()` before any calling any `RDFBeanManager`
method. 

You need to close the `RDFBeanManager` instance using `close()` method before shutting the repository down. Note that `RDFBeanManager` implements
`AutoCloseable`, so it can be done in a try-with-resources block:

```java
...
repo.initialize();
try (RDFBeanManager manager = new RDFBeanManager(repo)) {
   ...
}
finally {
    repo.shutDown();
}
```


Working with RDFBean classes
----------------------------

### Adding instances

To add (marshall) a current state of an RDFBean object into the underlying RDF model, use 
`add()` method:

```java
import org.eclipse.rdf4j.model.Resource;
...
Person person = new Person();
person.setId("http://example.com/persons/jdoe");
person.setName("John Doe");
...

Resource r = manager.add(person);
```

The method creates new RDF resource in the underlying model and populates it with the statements constructed from RDFBean data properties. 
The method returns a reference to the new resource (RDF4J [Resource](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/Resource.html) object).

If the resource representing this object already exists in the model, the method returns immediately without any model updates. You can use the `update()` method
(see below) to modify existing resources. 


### Updating instances

`update()` method updates an existing RDF resource in the model to synchronize it with the current state of an RDFBean object:

```java
...
person.setEmail("john.doe@example.com");
Resource r = manager.update(person);
```

The code above sets new property of our Person instance
and updates it in the RDF model, thus synchronizing the RDF resource with the
actual object state. 

### Retrieving instances

RDFBean class instances can be retrieved (unmarshalled) from their RDF representations
using `get()` method:

```java
...
Person person2 = manager.get("http://example.com/persons/jdoe", Person.class);
```

This method resolves the given RDFBean identifier and returns new instance of specified RDFBean class filled with data
retrieved from the RDF model.

In the example above, we assume that the RDFBean identifier has been declared
without namespace prefix. Otherwise, we would need to pass only the local part
of the identifier URI as an argument:

```java
... // assuming 'http://example.com/persons/' is a prefix declared in @RDFSubject
Person person2 = manager.get("jdoe", Person.class);
```

The `get()` method returns `null` if no RDF resource matching the given
RDFBean identifier and class is found.

If we already have a reference to an RDF resource (e.g., a value returned by `add()`
or `update()` method), we can bypass identifier resolving and retrive the matching RDFBean instance using
that value:

```java
...
Person person2 = (Person) manager.get(r);
```

The `getAll()` method iterates over all RDFBeans of a specified class in the
model. The method returns an instance of RDF4J `CloseableIteration` object that needs to be closed in the end:

```java
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
...
// Show names of all Persons in the model:
CloseableIteration<Person, Exception> iter = manager.getAll(Person.class);
while (iter.hasNext()) {
   Person person = iter.next();
   System.out.println(person.getName());
}
iter.close();
```


Working with RDFBean interfaces
-------------------------------

There is an alternative mapping technique which is based on using RDFBean interfaces
and [dynamic proxy objects](https://docs.oracle.com/javase/8/docs/technotes/guides/reflection/proxy.html).

Let's extract an interface from our RDFBean `Person` class and
annotate it the same way:

```java
@RDFBean("http://xmlns.com/foaf/0.1/Person")
public interface IPerson {

    @RDFSubject
    String getId();

    @RDF("http://xmlns.com/foaf/0.1/name")
    String getName();
    void setName(String name);

    //... other data properties
}
```

As soon as we have this interface, we can map it directly to an RDF resource in the model. No implementation class is needed. 
Instead of creating an object of a class implementing `IPerson`, we obtain an instance of the dynamic proxy object using `create()` method of
RDFBeanManager:

```java
IPerson person = manager.create("http://example.com/persons/jdoe", IPerson.class);
```

The `create()` method returns an object implementing our `IPerson`
interface and matching the RDFBean with the specified identifier in the model.
This proxy object has the same behaviour as an instance of the class implementing
that interface, except that the object data is stored not in the
Java heap, but directly in the underlying RDF model. So, the call:

```java
person.setName("John Doe");
```

will cause a statement with predicate `http://xmlns.com/foaf/0.1/name`
and the given literal value to be created or updated in the model. Similarly, the
`getName()` method returns the value retrieved directly from that statement.

If no resource matching the specified RDFBean identifier exists in the
model, `create()` returns an empty instance where all its getter methods
would return `null`. The underlying RDF model will be unchanged until the first
setter method is called.

Similarly to `getAll()`, there is a `createAll()` method to retrieve a collection of dynamic proxy objects for all existing
RDFBeans of a specific interface:

```java
// Show names of all Persons in the model:
Collection<IPerson> allPersons = manager.createAll(IPerson.class);
for (IPerson person: allPersons) {
   System.out.println(person.getName());
}
```


Deleting RDFBeans
------------------------------

RDFBean representation can be deleted from RDF model using `delete()` method with an RDFBean identifier and a reference 
to RDFBean class or interface:

```java
...
manager.delete("http://example.com/persons/jdoe", Person.class);
```


Advanced topics
---------------

### Cascade databinding

`add()` and `update()` methods performs cascade databinding. This means that
 if a marshalling object has other RDFBeans as its properties, they
will be marshalled and added to the model recursively.
It allows to manipulate the entire complex object models with
single method calls.

For example, let the `Person` class declare a `knows` property (probably,
mapped to `foaf:knows`) to link Persons one another (for simplicity, we assume that
cardinality of this property is 1):

```java
Person john = new Person();
john.setId("http://example.com/persons/john");
...
Person mary = new Person();
mary.setId("http://example.com/persons/mary");
...
john.setKnows(mary);
```

Calling `manager.add(john)` will result in adding both objects
(`john` and `mary`) and generating a statement to link their RDF
representations with the `knows` property.

Similarly, on retrieving the `john` RDFBean, the `mary` object
will be automatically unmarshalled and assigned to `john.knows` property,
thus reconstructing the whole graph of related objects.

The dynamic proxies follow the same principle: when a getter method is
called and the property references to another RDFBean, the proxy object is automatically
created and returned:

```java
IPerson john = manager.create("http://example.com/persons/john", IPerson.class);
IPerson mary = manager.create("http://example.com/persons/mary", IPerson.class);
john.setKnows(mary);
...
john.getKnows().setName("Mary"); // getKnows() returns the proxy equals to mary
System.out.println(mary.getName()); // -- will be "Mary"
```

### Arrays and Collections

RDFBeans framework supports Java arrays and basic Collection types, such as Sets and Lists. By default, the array or Collection properties
are represented with multiple RDF statements:

```java
private Collection<Person> knows;
...
@RDF("foaf:knows")
public Collection<Person> getKnows() {
	return knows;
}

...
Set<Person> others = new HashSet<>();
others.add(mary);
others.add(jim);
others.add(pete);
john.setKnows(others);
...
manager.add(john);
```

This will result in three `foaf:knows` statements generated for `john` in the RDF model. When these statements are read back to unmarshall the 
`john` object from the model, it is unlikely that the Collection will be restored in its original order. Therefore, the
default behavior might be undesirable for arrays and ordered Collections, like Lists. For these types, it is recommended to declare an [`rdf:Seq`](http://www.w3.org/TR/rdf-schema/#ch_seq)
 container type using the `@RDFContainer` annotation:

```java
@RDF("foaf:publications")
@RDFContainer(ContainerType.SEQ)
public Document[] getPublications() {
    return publications;
}
```

Arrays and Collections marshalled with `rdf:Seq` containers are guaranteed to retain original order of their elements.


### Transactions

By default, the framework provides atomicity of every single `add()`, `update()` or `delete()` operation. This means that modifications of all individual RDF statements
are performed in a single isolated [transaction](http://docs.rdf4j.org/programming/#_transactions) which is either commited after the modifications are done, or is rolled back in the case of an exception.

Sometimes, you may want to extend atomicity to multiple method calls, thus implementing transactional updates for a group of RDFBean objects. 
For instance, when a collection of RDFBeans is added to the repository in a loop, it makes sense to isolate that loop in a single transaction which 
will be commited after all objects are added successfully (or rolled back otherwise to discard all additions). 

To make this possible, `RDFBeanManager` exposes RDF4J [`RepositoryConnection`](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/repository/RepositoryConnection.html) 
instance, which is used internally to interact with the underlying Repository. Using `RepositoryConnection` methods, one can start, commit or rollback
transactions on the Repository in an explicit way:

```java
RepositoryConnection conn = manager.getRepositoryConnection(); // -- obtain a RepositoryConnection instance
conn.begin(); // -- start a transaction
try {
	// Add few RDFBean objects from a Collection. 
	// No actual data will be sent to the RDF store until current transaction is commited. 
    for (Person person: persons) {
    	manager.add(person);
    }
    conn.commit(); // commit the transaction and send above adds to the RDF store
}
catch (Throwable t) {
    // Something went wrong, we have to roll the transaction back and discard all adds made to this point.
    conn.rollback();
    throw t;
}
```

When `add()`, `update()` or `delete()` methods meet an active (unfinished) transaction, they do not try to begin new one, but add their updates to the
active transaction instead. Note that this works only within the _same thread_: as `RepositoryConnection` instances are thread-specific (see below),
their transaction status is not propagated to other threads.  

Setter methods of RDFBeans dynamic proxies use the similar approach: by default, updates of individual RDF statements are performed in a single transaction,
so every setter call is an atomic operation. With external transaction management, it is possible to group multiple calls into larger transactions, like this:

```java
RepositoryConnection conn = manager.getRepositoryConnection();
IPerson person = manager.create("http://example.com/persons/john", IPerson.class);
conn.begin(); // -- start a transaction
try {
	// Add few properties to IPerson instance
	// No actual data will be sent to the RDF store until current transaction is commited. 
    person.setName("John Doe");
    person.setEmail("johndoe@example.com");
    person.setHomepage("http://johndoe.example.com");
    
    conn.commit(); // commit the transaction and send above changes to the RDF store
}
catch (Throwable t) {
    // Something went wrong, we have to roll the transaction back and discard all adds made to this point.
    conn.rollback();
    throw t;
}
```

### Multithreaded access

`RDFBeanManager` class is designed to support concurrent access and its instances can be reused over multiple threads. When two or more threads invoke `RDFBeansManager` methods at the same
time, every thread operates on its own isolated `RepositoryConnection` instance to query and update the Repository. 

`RDFBeanManager` does not implement any locking mechanism to protect the store from conflicting updates, relying on used RDF store implementation in 
this respect. Concurrent transactions are resolved using the current [transaction isolation level](http://docs.rdf4j.org/programming/#_transaction_isolation_levels) of
`RepositoryConnection` instance. Memory and native stores use [`SNAPSHOT_READ`](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/IsolationLevels.html#SNAPSHOT_READ) 
isolation level by default and this can be changed with `RepositoryConnection.setIsolationLevel()` or for specific transaction using `begin()` method with the parameter:

```java
// We will start a transaction on a SERIALIZABLE level to ensure that there will be no conflicting concurrent updates
conn.begin(IsolationLevels.SERIALIZABLE); 
try {
	...
    conn.commit(); // commit the transaction and send above adds to the RDF store
}
catch (Throwable t) {
    conn.rollback();
    throw t;
}
```

`RepositoryConnection` objects are [not thread-safe](http://docs.rdf4j.org/programming/#_multithreaded_repository_access) and should not be shared 
over multiple threads. Every thread should obtain its own connection instance via `RDFBeanManager.getRepositoryConnection()`. 

Please refer to RDF4J documentation ([1](http://docs.rdf4j.org/programming/#_multithreaded_repository_access), [2](http://docs.rdf4j.org/programming/#_transaction_isolation_levels)) 
to learn more about transaction isolation and multithread access to the repositories.

 

### Anonymous RDFBeans 

If an RDFBean class does not declare an RDFBean identifier (`@RDFSubject`) property, or this property getter returns `null`, the object has a staus of an
_anonymous RDFBean_. Anonymous RDFBeans are represented in the RDF model as the
 [Blank nodes](http://www.w3.org/TR/2014/REC-rdf11-mt-20140225/#blank-nodes) and, unlike normal RDFBeans, cannot be addressed with their identifiers. 
 However, it is still possible to use these classes for declaring properties on other RDFBeans.








