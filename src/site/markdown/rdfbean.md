RDFBean classes and interfaces
==============================

<!-- MACRO{toc|class=toc} -->

This document describes a set of programming conventions for
Java classes and interfaces, compatible with RDFBeans framework ("RDFBeans"), as well as the
RDFBeans annotation scheme.

General conventions
-------------------

An RDFBean class must follow generic conventions required for JavaBean classes:

* The class must have a public default (no-arg) constructor.
* The class properties must be accessible using getter and setter methods
  following the standard naming convention.

An RDFBean interface defines getter and setter methods for RDFBean properties.

RDFBeans annotations
--------------------

### RDFBean type: `@RDFBean`

Annotation: `@RDFBean`  
Applied to: Class or interface declaration  
Value: String (required)  

@RDFBean annotation declares that the annotated class or interface defines a RDFBean object.

The mandatory value element of this annotation specifies a qualified name or an absolute URI 
of an RDF type of resources representing instances of this class (interface) in the RDF model.

Examples:

```java
 @RDFBean("foaf:Person") 
 public class Person { 
     ...
 }

 @RDFBean("http://xmlns.com/foaf/0.1/Person") 
 public interface Person { 
     ...
 }
```

### RDFBean identifier property: `@RDFSubject`

Annotation: `@RDFSubject`  
Applied to: Method declaration  
Value: `prefix` (String, optional)  

`@RDFSubject` annotation indicates that the annotated getter method returns a value of the RDFBean object identifier property.

An RDFBean class or interface should declare only one identifier property. If the property is declared, all identifier properties 
inherited from other classes (interfaces) are ignored. Otherwise, if no identifier property is declared on a given class or interface, 
it can be inherited from the nearest ancestors.

If no identifier property is found in the classes/interfaces hierarchy, the RDFBean object cannot be represented with an RDF resource 
in the model. However, it is still possible to represent it as a blank node (anonymous RDFBean).

The prefix parameter defines the optional prefix part of RDFBean identifier and must contain either a namespace URI or a reference to 
namespace defined by RDFNamespaces annotation.

If the prefix is specified, it is expected that the method returns a local part of RDFBean identifier. Otherwise, the method must return 
a value of RDFBean identifier as a fully qualified name.

Examples:

```java
@RDFSubject(prefix="http://rdfbeans.example.com/persons/") 
public String  getPersonId() {
   ...
```

```java
@RDFNamespaces("persons=http://rdfbeans.example.com/persons/");
...
@RDFSubject(prefix="persons:") 
public String getPersonId() { 
   ...
```

```java
@RDFSubject 
public String getPersonId() { 
   ... // A fully qualified name must be returned
```

### RDFBean property: `@RDF` 

Annotation: `@RDF`  
Applied to: Method declaration  
Value: String or `inverseOf` (String) (required)  

@RDF annotation declares a RDFBean data property. The annotations must be
applied to getter methods of RDFBean class or interface.

The String value defines a qualified name or absolute URI of an
RDF property (predicate) mapped to this property.

Example:

```java
@RDF("foaf:name")
public String getName() { 
    return name;
}
```

Alternative `inverseOf` element specifies that this property is an inversion of a property defined on RDFBeans class returned by this method:

```java
@RDFBean("urn:test:Parent")
public class Parent {
	...	
	@RDF(inverseOf="urn:test:hasParent")
	public Child[] getChildren() {
		return children;
	}
	...
}

@RDFBean("urn:test:Child")
public class Child {
	...	
	@RDF("urn:test:hasParent")
	public Parent getParent() {
		return parent;
	}
	...
}
```  

### Container type declaration: `@RDFContainer`

Annotation: `@RDFContainer`  
Applied to: Method declaration  
Value: `RDFContainer.ContainerType` (optional)  
Default value: `RDFContainer.ContainerType.NONE`  

`@RDFContainer` annotation supplements RDFBean property declaration (`@RDF` annotation) for properties of array and Collection types. 
The value element is a constant from RDFContainer.ContainerType enumeration that specifies a type of RDF container to hold values of this array 
or Collection in the RDF model.

If no `@RDFContainer` annotation is declared, each value of this array or Collection is represented with an individual RDF statement. 
It is not possible to guarantee any order of elements in this case.

Otherwise, multiple values are represented with an RDF Container as specified by `RDFContainer.ContainerType` element:

	* `RDFContainer.ContainerType.BAG` - rdf:Bag
	* `RDFContainer.ContainerType.SEQ` - rdf:Seq
	* `RDFContainer.ContainerType.ALT` - rdf:Alt
	* `RDFContainer.ContainerType.LIST` - rdf:List collection

Examples:

```java
@RDF("foaf:nick")
@RDFContainer(ContainerType.ALT)
public String[] getNick() {
...
```

### RDF namespaces declaration: `@RDFNamespaces`

Annotation: `@RDFNamespaces`  
Applied to: Class or interface declaration  
Value: String or String array (required)  

@RDFNamespaces annotation specifies one or more RDF namespace prefixes in the
format: `<prefix> = <uri>`

Examples:

```java
@RDFNamespaces("owl = http://www.w3.org/2002/07/owl#")
```

```java
@RDFNamespaces(
  {
    "foaf = http://xmlns.com/foaf/0.1/",
    "persons = http://rdfbeans.viceversatech.com/test-ontology/persons/"
  }
)
```


Property types
--------------

### Literals

The following Java classes and primitive types are supported in RDFBean literal property declarations:

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


### RDFBeans

RDFBean property may be declared with type of a RDFBean class or interface.
The framework provides automatic binding of these objects with the RDF model data
(cascade databinding).

### Arrays and collections

Arrays and Collections containing the objects of a literal or RDFBean type
are allowed as RDFBean property types. About RDF representation of arrays
and collections, see "Container type declaration" above.

If the Collection property is declared with an interface, or an abstract class, the
following implementation classes are instantiated:

* `java.util.HashSet` for `java.util.Collection`, `java.util.Set` and `java.util.AbstractSet`
* `java.util.TreeSet` for `java.util.SortedSet`
* `java.util.ArrayList` for `java.util.List` and `java.util.AbstractList`


Declaration inheritance
-----------------------

RDFBean classes inherit RDFBeans annotations declared on their superclasses and
interfaces.

RDFBean interfaces inherit annotations declared on their superinterfaces.

### Conflicts resolving

In the case of conflicting declarations, the lowest in the classes/interfaces
hierarchy take higher priority.

Examples
-----------------

### RDFBean class

```
package org.cyberborean.rdfbeans.test.examples.entities;

import java.net.URI;
import java.util.Collection;
import java.util.Date;

import org.cyberborean.rdfbeans.annotations.RDF;
import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.annotations.RDFContainer;
import org.cyberborean.rdfbeans.annotations.RDFContainer.ContainerType;
import org.cyberborean.rdfbeans.annotations.RDFNamespaces;
import org.cyberborean.rdfbeans.annotations.RDFSubject;

@RDFNamespaces({
    "foaf = http://xmlns.com/foaf/0.1/",
    "persons = http://rdfbeans.viceversatech.com/test-ontology/persons/"
})
@RDFBean("foaf:Person")
public class Person {

    private String id;
    private String name;
    private String email;
    private URI homepage;
    private Date birthday;
    private String[] nick;
    private Collection<Person> knows;

    /** Default no-arg constructor */
    public Person() {
    }

    @RDFSubject(prefix = "persons:")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @RDF("foaf:name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @RDF("foaf:mbox")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @RDF("foaf:homepage")
    public URI getHomepage() {
        return homepage;
    }

    public void setHomepage(URI homepage) {
        this.homepage = homepage;
    }

    @RDF("foaf:birthday")
    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    @RDF("foaf:nick")
    @RDFContainer(ContainerType.ALT)
    public String[] getNick() {
        return nick;
    }

    public void setNick(String[] nick) {
        this.nick = nick;
    }

    public String getNick(int i) {
        return nick[i];
    }

    public void setNick(int i, String nick) {
        this.nick[i] = nick;
    }

    @RDF("foaf:knows")
    public Collection<Person> getKnows() {
        return knows;
    }

    public void setKnows(Collection<Person> knows) {
        this.knows = knows;
    }
}
```

### RDFBean interface

```
package org.cyberborean.rdfbeans.test.examples.entities;

import java.net.URI;
import java.util.Collection;
import java.util.Date;

import org.cyberborean.rdfbeans.annotations.RDF;
import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.annotations.RDFContainer;
import org.cyberborean.rdfbeans.annotations.RDFContainer.ContainerType;
import org.cyberborean.rdfbeans.annotations.RDFNamespaces;
import org.cyberborean.rdfbeans.annotations.RDFSubject;

@RDFNamespaces({
    "foaf = http://xmlns.com/foaf/0.1/",
    "persons = http://rdfbeans.viceversatech.com/test-ontology/persons/"
})
@RDFBean("foaf:Person")
public interface IPerson {

    /** RDFBean ID property declaration */
    @RDFSubject(prefix = "persons:")
    String getId();

    /** Getters and setters for RDFBean properties */

    @RDF("foaf:name")
    String getName();

    void setName(String name);

    @RDF("foaf:mbox")
    String getEmail();

    void setEmail(String email);

    @RDF("foaf:homepage")
    URI getHomepage();

    void setHomepage(URI homepage);

    @RDF("foaf:birthday")
    Date getBirthday();

    void setBirthday(Date birthday);

    @RDF("foaf:nick")
    @RDFContainer(ContainerType.ALT)
    String[] getNick();

    void setNick(String[] nick);

    String getNick(int i);

    void setNick(int i, String nick);

    @RDF("foaf:knows")
    Collection<IPerson> getKnows();

    void setKnows(Collection<IPerson> knows);

}
```


