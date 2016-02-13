/**
 * 
 */
package com.viceversatech.rdfbeans.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.ontoware.rdf2go.model.Syntax;

import junit.framework.TestCase;

/**
 * @author alex
 *
 */
public class GeneratorTest extends TestCase {

	public void testFoafXml() throws Exception {
		RDFBeanGeneratorConfig config = new RDFBeanGeneratorConfig();
		config.setHeadingComment("Generated with RDFBeanGenerator");
		config.setSyntax(Syntax.RdfXml);
		config.setOutputDirectory(new File(System.getProperty("java.io.tmpdir") + File.separator + "rdfbeangenerator-test-foaf"));
		config.setPackageName("rdfbeangenerator.test");
		
		RDFBeanGenerator g = new RDFBeanGenerator(config);
		
		InputStream in = this.getClass().getResourceAsStream("foaf.rdf.xml");
		g.generate(new BufferedReader(new InputStreamReader(in)));
	}
	
	public void testNfoTtl() throws Exception {
		RDFBeanGeneratorConfig config = new RDFBeanGeneratorConfig();
		config.setHeadingComment("Generated with RDFBeanGenerator");
		config.setSyntax(Syntax.Turtle);
		config.setOutputDirectory(new File(System.getProperty("java.io.tmpdir") + File.separator + "rdfbeangenerator-test-nfo"));
		config.setPackageName("rdfbeangenerator.test");
		
		RDFBeanGenerator g = new RDFBeanGenerator(config);
		
		InputStream in = this.getClass().getResourceAsStream("nfo.ttl");
		g.generate(new BufferedReader(new InputStreamReader(in)));
	}
}
