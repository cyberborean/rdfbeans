
package org.cyberborean.rdfbeans.datatype;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;


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
 * 
 */
public class DefaultDatatypeMapper implements DatatypeMapper {

	private static final Map<Class<?>, IRI> DATATYPE_MAP = new HashMap<>();
	static {
		DATATYPE_MAP.put(String.class, XMLSchema.STRING);
		DATATYPE_MAP.put(Integer.class, XMLSchema.INT);
		DATATYPE_MAP.put(Date.class, XMLSchema.DATETIME);
		DATATYPE_MAP.put(Boolean.class, XMLSchema.BOOLEAN);
		DATATYPE_MAP.put(Float.class, XMLSchema.FLOAT);
		DATATYPE_MAP.put(Double.class, XMLSchema.DOUBLE);
		DATATYPE_MAP.put(Byte.class, XMLSchema.BYTE);
		DATATYPE_MAP.put(Long.class, XMLSchema.LONG);
		DATATYPE_MAP.put(Short.class, XMLSchema.SHORT);
		DATATYPE_MAP.put(BigDecimal.class, XMLSchema.DECIMAL);
		DATATYPE_MAP.put(java.net.URI.class, XMLSchema.ANYURI);
	}

	public static IRI getDatatypeURI(Class<?> c) {
		// Check for direct mapping
		IRI uri = DATATYPE_MAP.get(c);
		if (uri == null) {
			// Check for first assignable type mapping 
			for (Map.Entry<Class<?>, IRI> entry : DATATYPE_MAP.entrySet()) {
				if (entry.getKey().isAssignableFrom(c)) {
					return entry.getValue();
				}
			}
		}
		return uri;
	}

	public Object getJavaObject(Literal l) {
		IRI dt = l.getDatatype();
		if ((dt == null) || XMLSchema.STRING.equals(dt)) {
			return l.stringValue();
		} 
		else if (XMLSchema.BOOLEAN.equals(dt)) {
			return l.booleanValue();
		} 
		else if (XMLSchema.INT.equals(dt)) {
			return l.intValue(); //Integer.valueOf(l.intValue());
		} 
		else if (XMLSchema.BYTE.equals(dt)) {
			return l.byteValue(); //Byte.valueOf(l.byteValue());
		} 
		else if (XMLSchema.LONG.equals(dt)) {
			return l.longValue();//Long.valueOf(l.longValue());
		} 
		else if (XMLSchema.SHORT.equals(dt)) {
			return l.shortValue(); //Short.valueOf(l.shortValue());
		} 
		else if (XMLSchema.FLOAT.equals(dt)) {
			return l.floatValue(); //Float.valueOf(l.floatValue());
		} 
		else if (XMLSchema.DOUBLE.equals(dt)) {
			return l.doubleValue();//Double.valueOf(l.doubleValue());
		} 
		else if (XMLSchema.DECIMAL.equals(dt)) {
			return l.decimalValue();
		}
		else if (XMLSchema.ANYURI.equals(dt)) {
			return java.net.URI.create(l.stringValue());
		}
		else if (XMLSchema.DATETIME.equals(dt)) {
			return l.calendarValue().toGregorianCalendar().getTime();
			
			/*
			try {
				return DateUtils.parseDate(s);
			} catch (ParseException e) {
				
			}
			*/
		} 
		return l.stringValue();
	}

	@Override
	public Literal getRDFValue(Object value, ValueFactory vf) {
		if (value instanceof Date) {
			return vf.createLiteral((Date)value);
		}
		IRI dtUri = getDatatypeURI(value.getClass());
		if (dtUri != null) {
			if (dtUri.equals(XMLSchema.STRING)) {
				return vf.createLiteral(value.toString());
			}
			return vf.createLiteral(value.toString(), dtUri);
		}
		return null;
	}
}
