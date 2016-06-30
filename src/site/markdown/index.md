RDFBeans framework
==================

RDFBeans is an object-RDF mapping library for the Java language, providing
a framework for mapping an object-oriented domain model to RDF resource
descriptions.

RDFBeans is built upon [RDF2Go](http://rdf2go.semweb4j.org/) high-level
abstract model layer to provide object persistence with a number of existing
RDF triple stores.

RDFBeans framework supports two basic techniques for mapping the data from Java
applications to RDF:

* Persistence functionality to store and retrieve the state of class instances
  to/from a RDF model
* Dynamic proxy mechanism to access RDF data via Java interfaces mapped directly
  to the model

RDFBeans uses Java Annotations mechanism for all framework-related sourcecode markup.
No use of special interfaces and superclasses is required, that guarantees minimum
modifications of existing codebase and compatibility with other JavaBean-oriented
frameworks.


Features (version 2.0)
----------------------

 * Does not depend on specific triplestore implementation: any supported by RDF2Go API
   can be used
 * Cascade databinding to reduce development time and ensure referential integrity of complex object data structures
 * Modular RDFBeans annotations: can be inherited from superclasses and interfaces
 * No predefined ontologies and RDF-schemas are required for RDF data.
 * Transactions support (triplestore-specific)
 * Extensible mechanism of mapping Java data types to RDF literals
 * Support of basic Java Collections, optionally represented with RDF containers
 * Support of indexed JavaBean properties
 * Support of RDF namespaces


See more
--------
 * [RDFBeans usage](usage.html)
 * [RDFBean annotations](rdfbean.html)
 * [RDFBeans API JavaDocs](apidocs/index.html)
