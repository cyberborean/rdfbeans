/**
 * 
 */
package org.cyberborean.rdfbeans.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cyberborean.rdfbeans.annotations.RDFBean;
import org.cyberborean.rdfbeans.generator.rdfs.RDFSClass;
import org.cyberborean.rdfbeans.generator.rdfs.RDFSModel;
import org.cyberborean.rdfbeans.generator.rdfs.RDFSProperty;
import org.cyberborean.rdfbeans.generator.rdfs.RDFSResource;
import org.ontoware.rdf2go.ModelFactory;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Syntax;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.util.RDFTool;
import org.ontoware.rdf2go.vocabulary.RDFS;
import org.ontoware.rdf2go.vocabulary.XSD;

/**
 * @author alex
 *
 */
public class RDFBeanGenerator {
	
	RDFBeanGeneratorConfig config;
	
	public RDFBeanGenerator(RDFBeanGeneratorConfig config) {
		this.config = config;
	}
	
	public void generate() throws ModelRuntimeException, FileNotFoundException, IOException {
		generate(new BufferedReader(new FileReader(config.getFile())));
	}
	
	public void generate(Reader inputReader) throws ModelRuntimeException, FileNotFoundException, IOException {
		
		// Create and parse RDF-Schema
		ModelFactory modelFactory = RDF2Go.getModelFactory();
        Model model = modelFactory.createModel();
        model.open(); 
        
        Syntax syntax = config.getSyntax();
        if (syntax == null) {
        	syntax = RDFTool.guessSyntax(config.getFile().getName());
        }
        Reader reader = inputReader;
        model.readFrom(reader, config.getSyntax());
		reader.close();
        
        RDFSModel rdfs = new RDFSModel(model);
        
        // Prepare output directory
        File outDir;        
        String pkg = config.getPackageName();
        if (pkg != null) {
        	pkg = pkg.replace('.', File.separatorChar);
        	outDir = new File(config.getOutputDirectory().getAbsolutePath() + File.separatorChar + pkg);
        }
        else {
        	outDir = new File(config.getOutputDirectory().getAbsolutePath());
        }
        if (!outDir.isDirectory()) {
        	outDir.mkdirs();
        }
        
        // Iterate over RDFS classes ang generate Java files
        Set<RDFSClass> classes = rdfs.getClasses();
        for (RDFSClass cls: classes) {
        	generateFile(cls, outDir);
        }
	}

	/**
	 * @param cls
	 * @param outDir
	 * @throws IOException 
	 */
	private void generateFile(RDFSClass cls, File outDir) throws IOException {
		String text = "";
		if (config.isInterfaces()) {
			text = generateInterface(cls);
		}
		File f = new File(outDir, createJavaClassName(cls) + ".java");
		FileWriter fw = new FileWriter(f);
		fw.write(text);
		fw.close();
	}

	private String createJavaClassName(RDFSResource r) {		
		return createJavaName(r, true);
	}
	
	private String createJavaName(RDFSResource r, boolean cap) {
		String name = getLocalName((URI) r.getUri());
		return asLegalJavaID(name, cap);
	}
	
	
	private String getLocalName(URI vx) {
		String fullUri = vx.toString();
		int splitIdx = fullUri.indexOf('#');

		if (splitIdx < 0) {
			splitIdx = fullUri.lastIndexOf('/');
		}

		if (splitIdx < 0) {
			splitIdx = fullUri.lastIndexOf(':');
		}

		if (splitIdx < 0) {
			throw new RuntimeException("Not a legal (absolute) URI: " + fullUri);
		}
		return fullUri.substring(splitIdx + 1);
	}
	
	/**
	 * Convert s to a legal Java identifier; capitalise first char if cap is
	 * true 
	 */
	protected String asLegalJavaID(String s, boolean cap) {
		StringBuilder buf = new StringBuilder();
		int i = 0;

		// treat the first character specially - must be able to start a Java
		// ID, may have to upcase
		try {
			for (; !Character.isJavaIdentifierStart(s.charAt(i)); i++) {
				// skip all characters which are illegal at the start
			}
		} catch (StringIndexOutOfBoundsException e) {
			System.err
					.println("Could not identify legal Java identifier start character in '"
							+ s + "', replacing with __");
			return "__";
		}
		buf.append(cap ? Character.toUpperCase(s.charAt(i)) : s.charAt(i));

		// copy the remaining characters - replace non-legal chars with '_'
		for (++i; i < s.length(); i++) {
			char c = s.charAt(i);
			buf.append(Character.isJavaIdentifierPart(c) ? c : '_');
		}

		// check standard name
		String result = buf.toString();
		if (result.equals("class") || result.equals("abstract"))
			result = result + "_";
		return result;
	}

	/**
	 * @param cls
	 * @return
	 */
	private String generateInterface(RDFSClass cls) {
		String name = createJavaClassName(cls);
		Set<String> imports = getDefaultImports();
		StringBuffer body = new StringBuffer();
		body.append("@RDFBean(\"").append(cls.getUri()).append("\")\n");
		body.append("public interface ").append(name);
		Set<RDFSClass> superclasses = cls.getSuperClasses();
		if (!superclasses.isEmpty()) {
			body.append(" extends");
			for (RDFSClass c: superclasses) {
				body.append(' ').append(createJavaClassName(c)).append(',');				
			}
			body.deleteCharAt(body.length()-1);
		}
		body.append(" {\n\n");
		
		body.append("\t@RDFSubject\n");
		body.append("\tpublic String get").append(config.getSubjectProperty()).append("();\n\n");
		
		Set<RDFSProperty> props = cls.getPropertiesInDomain();
		for (RDFSProperty p: props) {
			String pName = createJavaName(p, true);
			String pClass = getPropertyClass(p, imports);
			body.append(createJavadocGetterComment(p));
			body.append("\t@RDF(\"").append(p.getUri()).append("\")\n");
			body.append("\tpublic ").append(pClass).append(" get").append(pName).append("();\n\n");
			body.append("\tpublic void set").append(pName).append("(").append(pClass).append(' ').append(createJavaName(p, false))
					.append(");\n\n");
		}
		
		body.append("}");
		
		StringBuffer out = new StringBuffer();
		if (config.getHeadingComment() != null) {
			out.append("/*\n * ").append(config.getHeadingComment()).append("\n */\n");
		}
		if (config.getPackageName() != null) {
			out.append("package ").append(config.getPackageName()).append(";\n\n");
		}
		for (String imp: imports) {
			out.append("import ").append(imp).append(";\n");
		}
		
		out.append(createJavadocComment(cls));
		
		out.append(body);
		
		return out.toString();
	}

	private static Map<URI, String> XSD_TO_JAVA = new HashMap<URI, String>();
	static {
		
		XSD_TO_JAVA.put(RDFS.Literal, String.class.getName());
		XSD_TO_JAVA.put(XSD._string, String.class.getName());
		XSD_TO_JAVA.put(XSD._normalizedString, String.class.getName());
		XSD_TO_JAVA.put(XSD._token, String.class.getName());
		XSD_TO_JAVA.put(XSD._language, String.class.getName());
		XSD_TO_JAVA.put(XSD._Name, String.class.getName());
		XSD_TO_JAVA.put(XSD._NCName, String.class.getName());
		XSD_TO_JAVA.put(XSD._ID, String.class.getName());
		XSD_TO_JAVA.put(XSD._IDREF, String.class.getName());
		XSD_TO_JAVA.put(XSD._ENTITY, String.class.getName());
		XSD_TO_JAVA.put(XSD._NMTOKEN, String.class.getName());
		XSD_TO_JAVA.put(XSD._IDREFS, String[].class.getName());
		XSD_TO_JAVA.put(XSD._ENTITIES, String[].class.getName());
		XSD_TO_JAVA.put(XSD._NMTOKENS, String[].class.getName());
		XSD_TO_JAVA.put(XSD._anyURI, String.class.getName());
		XSD_TO_JAVA.put(XSD._QName, String.class.getName());
		XSD_TO_JAVA.put(XSD._hexBinary, String.class.getName());
		XSD_TO_JAVA.put(XSD._base64Binary, String.class.getName());
		
		XSD_TO_JAVA.put(XSD._decimal, long.class.getName());
		XSD_TO_JAVA.put(XSD._integer, long.class.getName());
		XSD_TO_JAVA.put(XSD._nonNegativeInteger, long.class.getName());
		XSD_TO_JAVA.put(XSD._positiveInteger, long.class.getName());
		XSD_TO_JAVA.put(XSD._unsignedLong, long.class.getName());
		XSD_TO_JAVA.put(XSD._nonPositiveInteger, long.class.getName());
		XSD_TO_JAVA.put(XSD._negativeInteger, long.class.getName());
		XSD_TO_JAVA.put(XSD._duration, java.util.Date.class.getName());
		
		XSD_TO_JAVA.put(XSD._int, int.class.getName());
		XSD_TO_JAVA.put(XSD._unsignedInt, int.class.getName());
		
		XSD_TO_JAVA.put(XSD._short, short.class.getName());
		XSD_TO_JAVA.put(XSD._unsignedShort, short.class.getName());
		
		XSD_TO_JAVA.put(XSD._byte, byte.class.getName());
		XSD_TO_JAVA.put(XSD._unsignedByte, byte.class.getName());
		
		XSD_TO_JAVA.put(XSD._float, float.class.getName());
		
		XSD_TO_JAVA.put(XSD._double, double.class.getName());
		
		XSD_TO_JAVA.put(XSD._boolean, boolean.class.getName());
		
		XSD_TO_JAVA.put(XSD._dateTime, java.util.Date.class.getName());
		XSD_TO_JAVA.put(XSD._date, java.util.Date.class.getName());
		XSD_TO_JAVA.put(XSD._time, java.util.Date.class.getName());
		XSD_TO_JAVA.put(XSD._gDay, java.util.Date.class.getName());
		XSD_TO_JAVA.put(XSD._gYear, java.util.Date.class.getName());
		XSD_TO_JAVA.put(XSD._gMonth, java.util.Date.class.getName());
		XSD_TO_JAVA.put(XSD._gMonthDay, java.util.Date.class.getName());
		XSD_TO_JAVA.put(XSD._gYearMonth, java.util.Date.class.getName());
		
	}
	
	private String getPropertyClass(RDFSProperty p, Set<String> imports) {
		RDFSClass range = p.getRangeClass();
		if (range == null) {
			System.err.println("Warning: rdfs:range is not defined for property " + p.getUri() );
			return String.class.getName();
		}
		Resource uri = range.getUri();
		String javaType = XSD_TO_JAVA.get(uri);
		if (javaType != null) {
			return javaType;
		}
		return createJavaClassName(range);
	}

	/**
	 * @return
	 */
	private Set<String> getDefaultImports() {
		Set<String> imports = new HashSet<String>();
		imports.add("org.cyberborean.rdfbeans.annotations.*");
		return imports;
	}

	/**
	 * @param cls
	 * @return
	 */
	private StringBuffer createJavadocComment(RDFSResource r) {
		StringBuffer sb = new StringBuffer();
		sb.append("\t/**\n");
		String label = r.getLabel();
		if (label != null) {
			sb.append("\t * ").append(label).append("\n");
		}
		sb.append("\t *\n");
		String comment = r.getComment();
		if (comment != null) {
			sb.append("\t * ").append(comment).append("\n");
		}
		sb.append("\t *\n");
		sb.append("\t */\n");
		return sb;
	}
	
	private StringBuffer createJavadocGetterComment(RDFSResource r) {
		StringBuffer sb = new StringBuffer();
		sb.append("\t/**\n");		
		String comment = r.getComment();
		if (comment != null) {
			sb.append("\t * ").append(comment).append("\n");
		}
		sb.append("\t *\n");
		String label = r.getLabel();
		if (label == null) {
			label = "value";
		}
		sb.append("\t * @return ").append(label).append("\n");
		sb.append("\t */\n");
		return sb;
	}

}
