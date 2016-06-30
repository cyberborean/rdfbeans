/*
 * RDFBean.java         
 * -----------------------------------------------------------------------------
 * Created           Jan 14, 2009 12:13:37 PM by alex
 * Latest revision   $Revision: 21 $
 *                   $Date: 2011-04-02 15:15:34 +0600 (Sat, 02 Apr 2011) $
 *                   $Author: alexeya $
 *
 * @VERSION@ 
 *
 * @COPYRIGHT@
 * 
 * @LICENSE@ 
 *
 * -----------------------------------------------------------------------------
 */

package org.cyberborean.rdfbeans.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to: Class or interface declaration <br>
 * Value: String (required)
 * 
 * &#64;RDFBean annotation indicates that the annotated class (interface) is an
 * RDFBean and declares a qualified name or absolute URI of a RDF type (e.g. a
 * reference to RDF-Schema Class) of RDF resources representing the instances of
 * this class in the model.
 * 
 * Example:
 * 
 * ```
 * {@literal @}RDFBean("foaf:Person") 
 *  public class Person { ...
 * ```
 * 
 * @version $Id: RDFBean.java 21 2011-04-02 09:15:34Z alexeya $
 * @author Alex Alishevskikh, alexeya(at)gmail.com
 * 
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RDFBean {

	/**
	 * A qualified name or absolute URI of an RDFBean type
	 */
	String value();

}
