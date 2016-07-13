package org.cyberborean.rdfbeans.test.reflect;

import java.util.Map;
import org.cyberborean.rdfbeans.annotations.RDF;
import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.annotations.RDFNamespaces;
import org.cyberborean.rdfbeans.annotations.RDFSubject;
import org.cyberborean.rdfbeans.exceptions.RDFBeanValidationException;
import org.cyberborean.rdfbeans.reflect.RDFBeanInfo;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

public class RDFBeanInfoTest {
	@RDFNamespaces({"ex = http://example.org/",
		"	 ex2 	 =	http://example.com/withSpaces#	"})
	@RDFBean("ex:Bean")
	public static abstract class AnnotationTest {
		@RDF("ex2:test")
		public abstract Object getTest();
		public abstract void setTest(Object test);

		@RDF("pkg:field")
		private Object field;
		public abstract Object getField();
		public abstract void setField(Object field);
	}

	private static RDFBeanInfo info;
	
	@BeforeClass
	public static void parseAnnotations() throws RDFBeanValidationException {
		info = RDFBeanInfo.get(AnnotationTest.class);
	}

	@Test
	public void shouldReadNamespaces() {
		Map<String, String> namespaces = info.getRDFNamespaces();
		assertThat(namespaces, notNullValue());
		assertThat(namespaces.size(), is(3));
		assertThat(namespaces.containsKey("ex"), is(true));
		assertThat(namespaces.get("ex"), equalTo("http://example.org/"));
		assertThat(namespaces.containsKey("ex2"), is(true));
		assertThat(namespaces.get("ex2"), equalTo("http://example.com/withSpaces#"));
		assertThat(namespaces.containsKey("pkg"), is(true));
		assertThat(namespaces.get("pkg"), equalTo("http://example.com/package-ns#"));
	}

	@Test
	public void shouldApplyNamespaceToClass() {
		IRI expected = SimpleValueFactory.getInstance().createIRI("http://example.org/Bean");
		assertThat(info.getRDFType(), equalTo(expected));
	}

	@Test
	public void shouldApplyNamespaceToProperty() {
		IRI expected = SimpleValueFactory.getInstance().createIRI("http://example.com/withSpaces#test");
		assertThat(info.getProperty(expected), notNullValue());
	}

	@Test
	public void annotationMayBeOnField() {
		IRI field = SimpleValueFactory.getInstance().createIRI("http://example.com/package-ns#field");
		assertThat(info.getProperty(field), notNullValue());
	}
}
