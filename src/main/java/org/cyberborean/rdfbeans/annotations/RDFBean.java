package org.cyberborean.rdfbeans.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to: Class or interface declaration<br>
 * Value: String (required)
 * 
 * `@RDFBean` annotation declares that the annotated class or interface defines a
 * RDFBean object. 
 * 
 * The mandatory value element of this annotation specifies a qualified name or an absolute URI of an RDF type of
 * resources representing instances of this class (interface) in RDF model.
 * 
 * This annotation type is defined with `@Inherited` meta-annotation, so if not declared, it is automatically inherited from a nearest superclass.
 * 
 * Examples:
 * 
 * ```java
 * {@literal @}RDFBean("foaf:Person") 
 *  public class Person { 
 *      ...
 *  }
 * ```
 * 
 * ```java
 * {@literal @}RDFBean("http://xmlns.com/foaf/0.1/Person") 
 *  public interface Person { 
 *      ...
 *  }
 * ``` 
 * 
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RDFBean {

	/**
	 * A qualified name or absolute URI of an RDFBean type
	 */
	String value();

}
