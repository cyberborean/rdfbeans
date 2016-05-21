# RDFBeans framework

RDFBeans is an object-RDF mapping framework for Java. It provides ORM-like databinding functionality for RDF databases ("triplestores") with two basic techniques:

  * Classic object persistence: storing and retrieving the state of POJO objects
    to/from a RDF model

  * Dynamic proxy mechanism to access RDF data with Java interfaces mapped directly 
    to the underlying model

RDFBeans is built upon [Eclipse RDF4J](http://rdf4j.org/) (Sesame) API
to provide object persistence for a number of state-of-the-art 
RDF triplestore implementations.

RDFBeans is based on Java Annotations mechanism. 
No special interfaces and superclasses is required, that guarantees minimum 
modifications of existing codebase and compatibility with other POJO-oriented 
frameworks.   
  
##Features

  * Cascade databinding to reduce development time and ensure referential integrity of complex object models

  * Inheritance of RDFBeans annotations from superclasses and/or interfaces
    
  * No external specifications (RDF-schemas) are required: everything is declared by the annotations

  * Extensible mechanism of mapping Java types to RDF literals: you can define your own algorithms to represent your data structures with RDF

  * Support of basic Java Collections, optionally represented with RDF containers
  
  * Transactions support (triplestore-specific)  
  
  * Support of indexed JavaBean properties

  * Support of RDF namespaces


