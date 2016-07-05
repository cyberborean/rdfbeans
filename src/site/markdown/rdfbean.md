RDFBean classes and interfaces
==============================

<!-- MACRO{toc|fromDepth=2} -->

This document describes a set of programming conventions for
Java classes and interfaces, compatible with RDFBeans framework ("RDFBeans"), as well as the
RDFBeans annotation scheme.

General conventions
-------------------

An RDFBean class must follow generic conventions required for JavaBean classes:

* The class must have a public default (no-arg) constructor.
* The class properties must be accessible using getter and setter methods
  following the standard naming convention.

An RDFBean interace defines getter and setter methods for RDFBean properties.

Examples
--------

### Complete example of an RDFBean class

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

### Complete example of an RDFBean interface

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


RDF namespaces declaration
--------------------------

Annotation: `@RDFNamespaces`  
Applied to: Class or interface declaration  
Value: String or String array (required)  

@RDFNamespaces annotation specifies one or more RDF namespace prefixes in the
format: `<prefix> = <uri>`

Examples:

```
@RDFNamespaces("owl = http://www.w3.org/2002/07/owl#")
```

```
@RDFNamespaces(
  {
    "foaf = http://xmlns.com/foaf/0.1/",
    "persons = http://rdfbeans.viceversatech.com/test-ontology/persons/"
  }
)
```


RDFBean type declaration
------------------------

Annotation: `@RDFBean`  
Applied to: Class or interface declaration  
Value: String (required)  

`@RDFBean` annotation indicates that the annotated class (interace) is an
RDFBean and declares a qualified name or absolute URI of a RDF type (e.g.
a reference to RDF-Schema Class) of RDF resources representing the
instances of this class in the model.

Example:

```
@RDFBean("foaf:Person")
public class Person {
...
```


RDFBean identifier property declaration
---------------------------------------

Annotation: `@RDFSubject`  
Applied to: Method declaration  
Parameter: `prefix` (String, optional)  

@RDFSubject annotation indicates that the annotated getter method returns a
String value of RDFBean identifier.

The `prefix` parameter defines the optional prefix part of RDFBean
identifier and must contain either a namespace URI or a reference to namespace
defined by @RDFNamespaces annotation.

If `prefix` parameter is set, it is expected that the method returns a
local part of RDFBean identifier. Otherwise, the method must return a value of
RDFBean identifier as a fully qualified name.

Examples:

```
@RDFSubject(prefix="http://rdfbeans.example.com/persons/")
public String getPersonId() {
...
```

```
@RDFSubject(prefix="persons:")
public String getPersonId() {
...
```

```
@RDFSubject
public String getPersonId() {
... // A fully qualified name must be returned
```


RDFBean property declaration
----------------------------

Annotation: `@RDF`  
Applied to: Method declaration  
Value: String (required)  

@RDF annotation declares a RDFBean data property. The annotations must be
applied to getter methods of RDFBean class or interface.

The mandatory String value defines a qualified name or absolute URI of an
RDF property (predicate) mapped to this property.

Example:

```
@RDF("foaf:name")
public String getName() {
...
```


Container type declaration
--------------------------

Annotation: `@RDFContainer`  
Applied to: Method declaration  
Value: `RDFContainer.ContainerType` (optional)  
Default value: `RDFContainer.ContainerType.NONE`  

@RDFContainer annotation extends RDFBean property declaration
(@RDF) for the properties of Java array or Collection types. The annotation
takes a constant from `RDFContainer.ContainerType` enumeration as an
argument to specify how the multiple values must be represented in RDF.

If @RDFContainer annotation is undefined or takes the default
`RDFContainer.ContainerType.NONE` argument, the property is represented
as a set of individual RDF statements created for each value. The order of
elements is not guaranteed in this case.

Otherwise, multiple values are represented as a RDF Container of a type
specified by `RDFContainer.ContainerType` constant:

* `BAG` - RDF Bag container
* `SEQ` - RDF Seq container
* `ALT` - RDF Alt container


Examples:

```
@RDF("foaf:nick")
@RDFContainer(ContainerType.ALT)
public String[] getNick() {
...
```
```
@RDF("foaf:knows")
@RDFContainer(ContainerType.NONE) // -- this is unnecessary
public Set<Person> getKnows() {
...
```


Property types
--------------

### Literals

The following Java data types are supported for RDFBean literal properties
by default:

* `String`
* `Boolean`
* `java.util.Date`
* `Integer`
* `Float`
* `Double`
* `Byte`
* `Long`
* `Short`
* `java.net.URI`


### RDFBeans

RDFBean property may be declared with type of a RDFBean class or interface.
The framework supports automatic binding of these objects with the RDF model
(cascade databinding).

### Arrays and collections

Arrays and Collections containing the objects of a literal or RDFBean type
are allowed as RDFBean property types. About RDF representation of arrays
and collections, see "Container type declaration" above.

If a property is declared with a collection interface or an abstract class, the
following collection implementations are instantiated by default:

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


