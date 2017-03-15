
package org.cyberborean.rdfbeans.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to: Method declaration<br>
 * Value: String (required)
 * 
 * `@RDF` annotation declares a RDFBean data property. The annotations must be
 * applied to getter methods of an RDFBean class or interface.
 * 
 * The mandatory value element defines a qualified name or absolute URI of
 * an RDF property (predicate) mapped to this property.
 * 
 * Example:
 *
 * ```java
 * {@literal @}RDF("foaf:name")
 * public String getName() { 
 *     return name;
 * }
 * ```
 * 
 */

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RDF {
	/**
	 * A qualified name or absolute URI of an RDF property
	 */
	String value() default "";
	
	String inverseOf() default "";
}
