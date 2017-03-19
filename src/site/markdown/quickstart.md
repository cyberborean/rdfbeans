Quick Start Guide
====================

<!-- MACRO{toc|class=toc} -->

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

Use the RDF4J API to set up and initialize a repository that will be used to store RDF representations of your RDFBean objects. 

For example, you can use the built-in RDF4J Native RDF repository:

```java
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
...
File dataDir = new File("C:\\temp\\myRepository\\");      
repo = new SailRepository(new NativeStore(dataDir));
repo.initialize();
```

For detailed information about configuration of different repository types, please refer to 
[RDF4J documentation](http://docs.rdf4j.org/programming/#_the_repository_api).
 

RDFBeanManager
---------------

RDFBeans databinding functions are accessible as methods of
a single `RDFBeanManager` class. An `RDFBeanManager` instance is created with
an opened RDF4J `RepositoryConnection` that provides access to the unrelying RDF store.

```java
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.cyberborean.rdfbeans.RDFBeanManager;
...
try (RepositoryConnection con = repo.getConnection()) {
    RDFBeanManager manager = new RDFBeanManager(con);         
	// ... do something with RDFBeanManager
}
```
Note that you need to close the `RepositoryConnection` after the work is completed (or use "try-with-resource" block as in the above example).
The `Repository` object may also need to be properly closed by calling the `shutDown()` method.


### A note on multi-threading access

`RDFBeanManager` class, as well as RDF4J `RepositoryConnection` is not thread-safe. It is recommended that each thread obtain it’s own 
RepositoryConnection from a shared Repository object and create a separate RDFBeanManager instance on it.


Working with RDFBean classes
----------------------------

### Adding instances

To add (marshall) a current state of an RDFBean object into the underlying RDF model, use 
`add()` method:

```java
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

`update()` method updates the existing RDF resource in the model to synchronize it with the current state of an RDFBean object:

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

This method returns new instance of specified RDFBean class filled with data
retrieved from the RDF model.

In the example above, we assume that the RDFBean identifier has been declared
without namespace prefix. Otherwise, we would have to pass only the local part
of the identifier URI as an argument:

```java
... // assuming 'http://example.com/persons/' is a prefix declared in @RDFSubject
Person person2 = manager.get("jdoe", Person.class);
```

The `get()` method returns `null` if no RDF resource matching the given
RDFBean identifier and class is found.

If we already have a reference to the Resource (e.g., a value returned by `add()`
or `update()` method), we can retrive the matching RDFBean instance using
that value:

```java
...
Person person2 = (Person) manager.get(r);
```

The `getAll()` method iterates over all RDFBeans of a specified class in the
model:

```java
// Displays the names of all Persons in the model:
CloseableIteration<Person, Exception> iter = manager.getAll(Person.class);
while (iter.hasNext()) {
   Person p = iter.next();
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

As soon as we have this interface, we can map it directly to an RDF resource in the model.
For that, we need to obtain an instance of the dynamic proxy object using `create()` method of
RDFBeanManager:

```java
IPerson person = manager.create("http://example.com/persons/jdoe", IPerson.class);
```

The `create()` method returns an object implementing our `IPerson`
interface and matching the RDFBean with the specified identifier in the model.
This proxy object has the same behaviour as an instance of the class implementing
that interface, with the exception that the object data is stored not in the
Java heap, but directly in statements within the underlying RDF model. So, the call:

```java
person.setName("John Doe");
```

will cause a statement with predicate `http://xmlns.com/foaf/0.1/name`
and the given literal value to be created or updated in the model. Similarly, the
`getName()` method returns the value retrieved directly from that statement.

If no resource matching the specified RDFBean identifier exists in the
model, `create()` returns an empty instance, that is all its `getXXX()`
methods return nulls. The underlying RDF model will be unchanged until the first
`setXXX()` method is called.

To instantiate the proxy objects for existing RDFBeans, the `create()`
method with a Resource argument can be used. The method returns null if the
specified Resource doesn't exist in the model:

```java
IPerson person = manager.create(r);
```


Managing RDFBeans in the model
------------------------------

### Resolving RDFBean identifiers

If an RDFBean identifier is known, it can be resolved to an RDF resource
using the identifier's String value and RDFBean class:

```java
...
Resource r = manager.getResource("http://example.com/persons/jdoe", Person.class);
```

or, if a namespace prefix is set for identifiers:

```java
... // assuming 'http://example.com/persons/' is a prefix
Resource r = manager.getResource("jdoe", Person.class);
```

The method returns null if no matching Resource is found in the model.

### Deleting RDFBeans

RDFBean representation can be deleted from RDF model with `delete()` method:

```java
...
manager.delete("http://example.com/persons/jdoe", Person.class);
```

or, using a reference to the Resource: 

```java
...
manager.delete(r);
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

This will result in three `foaf:knows` statements generated for `john` in the RDF model. When these statements are parsed back to unmarshall the 
`john` object from the model, it is not guaranteed that the Collection will preserve the original order of its elements. Because of this, the
default behavior might be undesirable for arrays and ordered Collections, like Lists. For this types, it is recommended to use an ordered RDF Sequence ([rdf:Seq](http://www.w3.org/TR/rdf-schema/#ch_seq)).
container to hold the property values. This can be specified with the `@RDFContainer` annotation:

```java
@RDF("foaf:publications")
@RDFContainer(ContainerType.SEQ)
public Document[] getPublications() {
    return publications;
}
```

### Anonymous RDFBeans 

If an RDFBean class does not declare an RDFBean identifier (`@RDFSubject`) property, or this property value is not assigned, the object is 
_anonymous RDFBean_. Anonymous RDFBeans are represented in the RDF model as the
 [Blank nodes](http://www.w3.org/TR/2014/REC-rdf11-mt-20140225/#blank-nodes)  and have few limitations like unability of retrieving with 
 any identifier value. However, it is still possible to use these objects in properties of other RDFBeans.    


### Transactions

By default, RDFBeanManager methods add or delete individual RDF statements in a transaction-safe manner. A method starts new transaction on 
RepositoryConnection before any update and commit it automatically after updates are completed. If the method throws an exception, the entire 
transaction is rolled back that guarantees that all updates the method made to this point will not take effect.

The behaviour is different if the method is invoked when RepositoryConnection already has an active transaction. In this case, the method does not 
start new transaction, but re-uses existing one by adding new operations to it. The updates will not take effect until the transaction is committed. 
If an exception is thrown by the method, the transaction status is not changed (the client code is free to roll it back on it’s own).

With this explicit transaction management, one can group multiple RDFBeanManager operations and treat them as a single update, as shown in the below 
example:

```java
RepositoryConnection con = rdfBeanManager.getRepositoryConnection();
// start a transaction
con.begin();
try {
    // Add few RDFBean objects
    rdfBeanManager.add(object1);
    rdfBeanManager.add(object2);
    rdfBeanManager.add(object3);
    // Commit the above adds at once
    con.commit();
}
catch (Throwable t) {
    // Something went wrong, we roll the transaction back
    con.rollback();
    throw t;
}
```

