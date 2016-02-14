/**
 * DefaultDatatypeMapper.java
 * 
 * RDFBeans Feb 3, 2011 5:27:31 PM alex
 *
 * $Id: DefaultDatatypeMapper.java 44 2014-07-01 13:33:00Z alexeya $
 *  
 */
package org.cyberborean.rdfbeans.datatype;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.node.DatatypeLiteral;
import org.ontoware.rdf2go.model.node.Literal;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.vocabulary.XSD;


/**
 * Default DatatypeMapper implementation based on XML-Schema data types for 
 * representation of Java primitive wrapper classes and dates as typed RDF 
 * literals. 
 * 
 * <ul>
 * <li>Instances of {@link String} are represented as plain (untyped) RDF
 * literals.</li>
 * <li>Instances of {@link Boolean}, {@link Integer}, {@link Float},
 * {@link Double}, {@link Byte}, {@link Long} and {@link Short} are represented
 * as RDF literals with corresponding XML-Schema datatypes.</li>
 * <li>Instances of {@link Date} are represented as RDF literals of <code>xsd:dateTime</code>
 * type, serialized into ISO8601 date/time format.</li>
 * </ul>
 * 
 * @author alex
 * 
 */
public class DefaultDatatypeMapper implements DatatypeMapper {

	private static final Map<Class, URI> DATATYPE_MAP = new HashMap<Class, URI>();
	static {
		DATATYPE_MAP.put(String.class, XSD._string);
		DATATYPE_MAP.put(Integer.class, XSD._integer);
		DATATYPE_MAP.put(Date.class, XSD._dateTime);
		DATATYPE_MAP.put(Boolean.class, XSD._boolean);
		DATATYPE_MAP.put(Float.class, XSD._float);
		DATATYPE_MAP.put(Double.class, XSD._double);
		DATATYPE_MAP.put(Byte.class, XSD._byte);
		DATATYPE_MAP.put(Long.class, XSD._long);
		DATATYPE_MAP.put(Short.class, XSD._short);
	}
	
	public static URI getDatatypeURI(Class c) {
		// Check for direct mapping
		URI uri = DATATYPE_MAP.get(c);
		if (uri == null) {
			// Check for first assignable type mapping 
			for (Map.Entry<Class, URI> me : DATATYPE_MAP.entrySet()) {
				if (me.getKey().isAssignableFrom(c)) {
					return me.getValue();
				}
			}
		}
		return uri;
	}

	public Object getJavaObject(Literal l) {
		String s = l.getValue();
		if (l instanceof DatatypeLiteral) {
			URI dt = l.asDatatypeLiteral().getDatatype();
			if (dt.equals(XSD._string)) {
				return s;
			} else if (dt.equals(XSD._boolean)) {
				return Boolean.valueOf(s);
			} else if (dt.equals(XSD._integer)) {
				return Integer.valueOf(s);
			} else if (dt.equals(XSD._byte)) {
				return Byte.valueOf(s);
			} else if (dt.equals(XSD._long)) {
				return Long.valueOf(s);
			} else if (dt.equals(XSD._short)) {
				return Short.valueOf(s);
			} else if (dt.equals(XSD._float)) {
				return Float.valueOf(s);
			} else if (dt.equals(XSD._double)) {
				return Double.valueOf(s);
			} else if (dt.equals(XSD._dateTime)) {
				try {
					return DateUtils.parseDate(s);
				} catch (ParseException e) {
					
				}
			} 
		}
		return s;
	}
	
	public Literal getRDFValue(Object value, Model model) {
		URI dtUri = getDatatypeURI(value.getClass());
		if (dtUri != null) {
			String s = value.toString();
			if (value instanceof Date) {
				s = DateUtils.getDefaultDateFormat().format((Date) value);
			}
			if (dtUri.equals(XSD._string)) {
				return model.createPlainLiteral(s);
			}
			return model.createDatatypeLiteral(s, dtUri);
		}
		return null;
	}
	
}
