package org.cyberborean.rdfbeans.test.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Date;

import org.cyberborean.rdfbeans.annotations.RDF;
import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.annotations.RDFSubject;
import org.cyberborean.rdfbeans.exceptions.RDFBeanException;
import org.cyberborean.rdfbeans.test.RDFBeansTestBase;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.Test;

public class DefaultValuesTest extends RDFBeansTestBase {

	@Test
	public void test() throws RepositoryException, RDFBeanException {		
		DefaultValuesTestIface o1 = manager.create("urn:test:object", DefaultValuesTestIface.class);
		
		assertEquals('\u0000', o1.getCharValue());
		assertFalse(o1.isBooleanValue());
		assertEquals(0, o1.getIntValue());
		assertEquals(0.0f, o1.getFloatValue(), 0);
		assertEquals(0.0d, o1.getDoubleValue(), 0);
		assertEquals(0, o1.getByteValue());
		assertEquals(0L, o1.getLongValue());
		assertEquals(0, o1.getShortValue());
		
		assertNull(o1.getStringValue());
		assertNull(o1.getDateValue());
		assertNull(o1.getUriValue());		
	}
	
    
    @RDFBean("http://cyberborean.org/rdfbeans/2.0/test/proxy/DefaultValuesTestIface")
	public interface DefaultValuesTestIface {
    	@RDFSubject
    	String getUri();
    	
    	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/string")
    	public String getStringValue();
    	
    	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/char")
    	public char getCharValue();
    	
    	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/boolean")
    	public boolean isBooleanValue();
    	
    	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/int")
    	public int getIntValue();
    	
    	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/float")
    	public float getFloatValue();
    	
    	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/double")
    	public double getDoubleValue();
    	
    	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/byte")
    	public byte getByteValue();
    	
    	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/long")
    	public long getLongValue();
    	
    	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/short")
    	public short getShortValue();
    	
    	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/date")
    	public Date getDateValue();
    	
    	@RDF("http://cyberborean.org/rdfbeans/2.0/test/datatype/uri")
    	public java.net.URI getUriValue();
	}

}
