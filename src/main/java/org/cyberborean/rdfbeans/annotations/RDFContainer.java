/**
 * RDFContainer.java
 * 
 * RDFBeans Feb 16, 2011 3:54:18 PM alex
 *
 * $Id: RDFContainer.java 36 2012-12-09 05:58:20Z alexeya $
 *  
 */
package org.cyberborean.rdfbeans.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to: Method declaration 
 * <br>Value: RDFContainer.ContainerType (optional)
 * <br>Default value: RDFContainer.ContainerType.NONE
 * 
 * <p>
 * &#64;RDFContainer annotation extends RDFBean property declaration ({@link RDF}) for the
 *               properties of Java array or Collection types. The annotation
 *               takes a constant from {@link RDFContainer.ContainerType} enumeration as
 *               an argument to specify how the multiple values must be
 *               represented in RDF.
 * 
 * <p>
 *               If &#64;RDFContainer annotation is undefined or takes the default
 *               {@link RDFContainer.ContainerType.NONE} argument, the property is
 *               represented as a set of individual RDF statements created for
 *               each value. The order of elements is not guaranteed in this
 *               case.
 * 
 * <p>
 *               Otherwise, multiple values are represented as a RDF Container
 *               of a type specified by {@link RDFContainer.ContainerType} constant.
 *               
 * <p>
 *               Examples:
 * <pre>
 * &#64;RDF("foaf:nick")
 * &#64;RDFContainer(ContainerType.ALT) 
 * public String[] getNick() { 
 * ...
 * </pre>
 * 
 * <pre>
 * &#64;RDF("foaf:knows")
 * &#64;RDFContainer(ContainerType.NONE) // -- this is unnecessary public
 * Set<Person> getKnows() { 
 * ...
 * </pre>
 * 
 * 
 * @author alex
 * 
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RDFContainer {

	/**
	 * Specifies a type of RDF container for Collection properties.
	 */
	ContainerType value() default ContainerType.NONE;

	/**
	 * RDF Container types:
	 * 
	 * <ul>
	 * <li>{@link ContainerType.NONE} - No container</li>
	 * <li>{@link ContainerType.BAG} - RDF Bag container</li>
	 * <li>{@link ContainerType.SEQ} - RDF Seq container</li>
	 * <li>{@link ContainerType.ALT} - RDF Alt container</li>
	 * </ul>
	 * 
	 */
	public enum ContainerType {
		NONE, BAG, SEQ, ALT;
	}
}
