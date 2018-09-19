package org.cyberborean.rdfbeans.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to: Method declaration <br>
 * Value: `prefix` (String, optional)
 * 
 * &#64;RDFSubject annotation indicates that the annotated getter method returns
 * a value of the RDFBean object identifier property. 
 * 
 * An RDFBean class or interface should declare only one identifier property.
 * If the property is declared, all identifier properties inherited from other classes (interfaces) are ignored. 
 * Otherwise, if no identifier property is declared on a given class or interface, it can be inherited from the nearest ancestors.
 * 
 * If no identifier property is found in the classes/interfaces hierarchy, the RDFBean object cannot be represented with 
 * an RDF resource in the model. However, it is still possible to represent it as a blank node (anonymous RDFBean).
 * 
 * The `prefix` parameter defines the optional prefix part of RDFBean identifier
 * and must contain either a namespace URI or a reference to namespace defined
 * by {@link RDFNamespaces} annotation.
 * 
 * If the prefix is specified, it is expected that the method returns a local
 * part of RDFBean identifier. Otherwise, the method must return a value of
 * RDFBean identifier as a fully qualified name.
 * 
 * Examples:
 * 
 * ```java
 * {@literal @}RDFSubject(prefix="http://rdfbeans.example.com/persons/") 
 *  public String  getPersonId() {
 * ...
 * ```
 * 
 * ```java
 * {@literal @}RDFNamespaces("persons=http://rdfbeans.example.com/persons/");
 * ...
 * {@literal @}RDFSubject(prefix="persons:") 
 *  public String getPersonId() { 
 *  ...
 * ```
 * 
 * ```
 * {@literal @}RDFSubject 
 *  public String getPersonId() { 
 *  ... // A fully qualified name must be returned
 * ```
 * 
 */

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RDFSubject {

	/**
	 * An optional namespace prefix of RDFBean identifiers
	 */
	String prefix() default "";

}
