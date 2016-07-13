package org.cyberborean.rdfbeans.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * Applied to: Class or interface declaration <br>
 * Value: String or String array (required)
 *
 *
 * `@RDFNamespaces` annotation specifies one or more RDF namespace prefixes in
 * the format `<prefix> = <uri>`
 *
 * Examples:
 * ---------
 * ```
 * {@literal @}RDFNamespaces("owl = http://www.w3.org/2002/07/owl#");
 * ```
 *
 * ```
 * {@literal @}RDFNamespaces({
 *    "foaf = http://xmlns.com/foaf/0.1/",
 *    "persons = http://rdfbeans.viceversatech.com/test-ontology/persons/"
 *  });
 * ```
 *
 * @author alex
 *
 */
@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RDFNamespaces {

	/**
	 * Namespace prefix specification (<code>&lt;prefix&gt; = &lt;uri&gt;</code>)
	 */
	String[] value();

}
