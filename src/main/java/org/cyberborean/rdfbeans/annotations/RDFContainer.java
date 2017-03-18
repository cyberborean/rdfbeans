
package org.cyberborean.rdfbeans.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to: Method declaration <br> 
 * Value: {@link ContainerType} (optional) <br>
 * Default value: {@link ContainerType#NONE}
 *
 *
 * `@RDFContainer` annotation supplements RDFBean property declaration (@{@link RDF} annotation) for 
 * properties of array and Collection types. The value element is a constant from {@link ContainerType} enumeration 
 * that specifies a type of RDF container to hold values of this array or Collection in the RDF model. 
 *
 * If no `@RDFContainer` annotation is declared, each value of this array or Collection is
 * represented with an individual RDF statement. It is not possible to guarantee any order of elements in this
 * case.
 *
 * Otherwise, multiple values are represented with an [RDF Container](https://www.w3.org/TR/rdf-schema/#ch_containervocab)
 * as specified by {@link ContainerType} element:
 * 
 *  - {@link ContainerType#BAG} - rdf:Bag
 *  - {@link ContainerType#SEQ} - rdf:Seq
 *  - {@link ContainerType#ALT} - rdf:Alt
 *  - {@link ContainerType#LIST} - rdf:List collection
 *
 *
 * Example:
 * 
 * ```java
 * {@literal @}RDF("foaf:nick")
 * {@literal @}RDFContainer(ContainerType.ALT)
 *  public String[] getNick() { 
 * ...
 * ``` 
 *
 */

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RDFContainer {

	/**
	 * Specifies a type of RDF container for array or Collection properties.
	 */
	ContainerType value() default ContainerType.NONE;

	/**
	 * RDF Container types:
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
