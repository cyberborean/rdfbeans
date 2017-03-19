# RDFBeans framework

RDFBeans is an object-RDF mapping framework for Java. It provides ORM-like databinding functionality for RDF data models 
with two basic techniques:

  * Classic object persistence: basic CRUD (Create, Retrieve, Update and Delete) operations on the state of JavaBean-like POJO objects

  * Dynamic proxy mechanism for transparent access of RDF data using Java interfaces mapped directly 
    to the underlying RDF model structures

RDFBeans is built upon [Eclipse RDF4J](http://rdf4j.org/) (former Sesame) API
to provide object persistence with a number of RDF storage implementations, including
[third party RDF database solutions](http://rdf4j.org/about/rdf4j-databases/).

RDFBeans is based on Java Annotations mechanism. 
No special interfaces and superclasses is required, that guarantees minimum 
modifications of existing codebase and compatibility with other POJO-oriented 
frameworks.   
  
Other features:

  * Built-in support of basic Java literal types mapped to standard XML-Schema literals; with ability to extend
    it with custom algorithms to represent domain-specific data structures  
  * Cascade databinding to reduce development time and to ensure referential integrity of complex object models  
  * Support of basic Java Collections, optionally represented with RDF containers  
  * Inheritance of RDFBeans annotations from superclasses and/or interfaces  
  * Transaction-safe model updates  
  * Virtual inversed properties to model [owl:inverseOf](http://www.w3.org/TR/2004/REC-owl-semantics-20040210/#owl_inverseOf)-like behavior (experimental)  
  * Support of RDF namespaces

Read more
---------
 
 * [Quick Start Guide](https://rdfbeans.github.io/quickstart.html)
 
 * [RDFBean annotations](https://rdfbeans.github.io/rdfbean.html)
 
 * [RDFBeans API JavaDocs](https://rdfbeans.github.io/apidocs/index.html)

Usage
-----

Include a Maven dependency into your project POM:

```
<dependencies>
        <dependency>
            <groupId>org.cyberborean</groupId>
            <artifactId>rdfbeans</artifactId>
            <version>2.2</version>
        </dependency>
</dependencies>
```
