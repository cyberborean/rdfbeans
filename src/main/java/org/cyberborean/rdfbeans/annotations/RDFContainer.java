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
 * Value: RDFContainer.ContainerType (optional)
 * Default value: RDFContainer.ContainerType.NONE
 *
 *
 * `@RDFContainer` annotation extends RDFBean property declaration ({@link RDF}) for the
 * properties of Java array or Collection types. The annotation
 * takes a constant from {@link ContainerType} enumeration as
 * an argument to specify how the multiple values must be
 * represented in RDF.
 *
 *
 * If `@RDFContainer` annotation is undefined or takes the default
 * {@link ContainerType#NONE} argument, the property is
 * represented as a set of individual RDF statements created for
 * each value. The order of elements is not guaranteed in this
 * case.
 *
 *
 * Otherwise, multiple values are represented as a RDF Container
 * of a type specified by {@link ContainerType} constant.
 *
 *
 * Examples:
 * ---------
 * ```
 * {@literal @}RDF("foaf:nick")
 * {@literal @}RDFContainer(ContainerType.ALT)
 *  public String[] getNick() { 
 * ...
 * ```
 *
 * ```
 * {@literal @}RDF("foaf:knows")
 * {@literal @}RDFContainer(ContainerType.NONE) // -- this is unnecessary public
 *  Set<Person> getKnows() {
 * ...
 * ```
 *
 * @author alex
 *
 */

@Target({ElementType.METHOD, ElementType.FIELD})
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
	 * <li>{@link #NONE} - No container</li>
	 * <li>{@link #BAG} - RDF Bag container</li>
	 * <li>{@link #SEQ} - RDF Seq container</li>
	 * <li>{@link #ALT} - RDF Alt container</li>
	 * <li>{@link #LIST} - RDF List container</li>
	 * </ul>
	 * 
	 */
	enum ContainerType {
		/** No container */
		NONE,
		/** RDF Bag container */
		BAG,
		/** RDF Seq container */
		SEQ,
		/** RDF Alt container */
		ALT,
		/** RDF List container */
		LIST;
	}
}
