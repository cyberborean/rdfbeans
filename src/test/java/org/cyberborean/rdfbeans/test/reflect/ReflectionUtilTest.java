package org.cyberborean.rdfbeans.test.reflect;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.List;

import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.reflect.ReflectionUtil;
import org.junit.Test;

public class ReflectionUtilTest {

	@Test
	public void testGetClassAnnotation() {
		RDFBean a = ReflectionUtil.getClassAnnotation(X.class, RDFBean.class);
		assertEquals(RDF_TYPE_C, a.value());
	}
	
	@Test
	public void testGetClassAnnotationIface() {
		RDFBean a = ReflectionUtil.getClassAnnotation(IX.class, RDFBean.class);
		assertEquals(RDF_TYPE_C, a.value());
	}
	
	@Test
	public void testGetAllInterfacesTest() {
		List<Class<?>> ifaces = ReflectionUtil.getAllInterfaces(IX.class);
		assertEquals(ID.class, ifaces.get(0));
		assertEquals(Comparable.class, ifaces.get(1));
		assertEquals(Serializable.class, ifaces.get(2));
		assertEquals(IC.class, ifaces.get(3));
		assertEquals(IA.class, ifaces.get(4));		
	}
	
	private static final String RDF_TYPE_A = "http://cyberborean.org/rdfbeans/test/A";
	private static final String RDF_TYPE_B = "http://cyberborean.org/rdfbeans/test/B";
	private static final String RDF_TYPE_C = "http://cyberborean.org/rdfbeans/test/C";


	@RDFBean(RDF_TYPE_A)
	static class A {}
	
	@RDFBean(RDF_TYPE_B)
	static class B extends A {}
	
	@RDFBean(RDF_TYPE_C)
	static class C extends A {}
	
	static class D extends C {}
	
	static class X extends D {}
	
	@RDFBean(RDF_TYPE_A)
	static interface IA {}
	
	@RDFBean(RDF_TYPE_B)
	static interface IB extends IA {}
	
	@RDFBean(RDF_TYPE_C)
	static interface IC extends IA {}
	
	static interface ID extends IC {}
		
	static interface IX extends ID, Comparable, Serializable {}

}
