/**
 * DatatypeTestClass.java
 * 
 * RDFBeans Feb 4, 2011 4:25:19 PM alex
 *
 * $Id: DatatypeTestClass.java 21 2011-04-02 09:15:34Z alexeya $
 *  
 */
package org.cyberborean.rdfbeans.test.entities;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.cyberborean.rdfbeans.annotations.RDF;
import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.annotations.RDFContainer;
import org.cyberborean.rdfbeans.annotations.RDFSubject;
import org.cyberborean.rdfbeans.annotations.RDFContainer.ContainerType;

/**
 * DatatypeTestClass.
 *
 * @author alex
 *
 */
@RDFBean("http://cyberborean.org/rdfbeans/2.0/test/datatype/DatatypeTestClass")
public class DatatypeTestClass {
	
	String stringValue;
	boolean booleanValue;	
	int intValue;
	float floatValue;
	double doubleValue;
	byte byteValue;
	long longValue;
	short shortValue;
	Date dateValue;
	java.net.URI uriValue;
	
	int[] arrayValue;
	List listValue;
	Set setValue;
	SortedSet sortedSetValue;
	List headTailList;

	@RDFSubject
	public String getID() {
		return "http://cyberborean.org/rdfbeans/2.0/test/datatype/testInstance";
	}
	
	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/string")
	public String getStringValue() {
		return stringValue;
	}
	
	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/boolean")
	public boolean isBooleanValue() {
		return booleanValue;
	}
	
	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/int")
	public int getIntValue() {
		return intValue;
	}
	
	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/float")
	public float getFloatValue() {
		return floatValue;
	}
	
	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/double")
	public double getDoubleValue() {
		return doubleValue;
	}
	
	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/byte")
	public byte getByteValue() {
		return byteValue;
	}
	
	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/long")
	public long getLongValue() {
		return longValue;
	}
	
	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/short")
	public short getShortValue() {
		return shortValue;
	}
	
	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/date")
	public Date getDateValue() {
		return dateValue;
	}
	
	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/uri")
	public java.net.URI getUriValue() {
		return uriValue;
	}
	
	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/array")
	@RDFContainer(ContainerType.SEQ)
	public int[] getArrayValue() {
		return arrayValue;
	}
	
	public int getArrayValue(int n) {
		return arrayValue[n];
	}
	
	@RDF(value="http://cyberborean.org/rdfbeans/2.0/test/datatype/list")
	public List getListValue() {
		return listValue;
	}

	@RDF(value="http://cyberborean.org/rdfbeans/2.0/test/datatype/set")
	@RDFContainer(ContainerType.BAG)
	public Set getSetValue() {
		return setValue;
	}
	
	@RDF(value="http://cyberborean.org/rdfbeans/2.0/test/datatype/sortedSet")
	@RDFContainer(ContainerType.ALT)
	public SortedSet getSortedSetValue() {
		return sortedSetValue;
	}

	@RDF(value="http://cyberborean.org/rdfbeans/2.0/test/datatype/headTailList")
	@RDFContainer(ContainerType.LIST)
	public List getHeadTailListValue() {
		return headTailList;
	}

	// Setters
	
	public void setID(String id) {
		//no-op
	}	
	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}
	public void setBooleanValue(boolean booleanValue) {
		this.booleanValue = booleanValue;
	}
	public void setIntValue(int intValue) {
		this.intValue = intValue;
	}
	public void setFloatValue(float floatValue) {
		this.floatValue = floatValue;
	}
	public void setDoubleValue(double doubleValue) {
		this.doubleValue = doubleValue;
	}
	public void setByteValue(byte byteValue) {
		this.byteValue = byteValue;
	}
	public void setLongValue(long longValue) {
		this.longValue = longValue;
	}
	public void setShortValue(short shortValue) {
		this.shortValue = shortValue;
	}
	public void setDateValue(Date dateValue) {
		this.dateValue = dateValue;
	}
	public void setUriValue(java.net.URI uriValue) {
		this.uriValue = uriValue;
	}
	public void setArrayValue(int[] arrayValue) {
		this.arrayValue = arrayValue;
	}
	public void setArrayValue(int n, int value) {
		this.arrayValue[n] = value;
	}
	public void setListValue(List listValue) {
		this.listValue = listValue;
	}
	public void setSetValue(Set setValue) {
		this.setValue = setValue;
	}
	public void setSortedSetValue(SortedSet sortedSetValue) {
		this.sortedSetValue = sortedSetValue;
	}
	public void setHeadTailList(List headTailList) { this.headTailList = headTailList; }

}
