/*
 * RDFSubject.java         
 * -----------------------------------------------------------------------------
 * Created           Jan 14, 2009 12:15:37 PM by alex
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
 * Applied to: Method declaration <br>
 * Parameter: prefix (String, optional)
 * 
 * <p>
 * &#64;RDFSubject annotation indicates that the annotated getter method returns
 * a String value of RDFBean identifier.
 * 
 * <p>
 * The prefix parameter defines the optional prefix part of RDFBean identifier
 * and must contain either a namespace URI or a reference to namespace defined
 * by {@link RDFNamespaces} annotation.
 * <p>
 * If prefix parameter is set, it is expected that the method returns a local
 * part of RDFBean identifier. Otherwise, the method must return a value of
 * RDFBean identifier as a fully qualified name.
 * 
 * <p>
 * Examples:
 * 
 * <pre>
 * &#64;RDFSubject(prefix="http://rdfbeans.example.com/persons/") 
 * public String  getPersonId() {
 * ...
 * </pre>
 * 
 * <pre>
 * &#64;RDFSubject(prefix="persons:") 
 * public String getPersonId() { 
 * ...
 * </pre>
 * 
 * <pre>
 * &#64;RDFSubject 
 * public String getPersonId() { 
 * ... // A fully qualified name must be returned
 * </pre>
 * 
 * 
 * 
 * @version $Id: RDFSubject.java 21 2011-04-02 09:15:34Z alexeya $
 * @author Alex Alishevskikh, alexeya(at)gmail.com
 * 
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RDFSubject {

	/**
	 * An optional namespace prefix of RDFBean identifiers
	 */
	String prefix() default "";

}
