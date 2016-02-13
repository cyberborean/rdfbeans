/**
 * 
 */
package com.viceversatech.rdfbeans.generator;

import java.io.File;

import org.ontoware.rdf2go.model.Syntax;

/**
 * @author alex
 *
 */
public class RDFBeanGeneratorConfig {

	// input options
	File file;
	Syntax syntax;
	
	// output options
	File outputDirectory = new File(".");
	String packageName = "";
	boolean interfaces = true;
	String headingComment;
	String subjectProperty = "Id";

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public Syntax getSyntax() {
		return syntax;
	}

	public void setSyntax(Syntax syntax) {
		this.syntax = syntax;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public boolean isInterfaces() {
		return interfaces;
	}

	public void setInterfaces(boolean interfaces) {
		this.interfaces = interfaces;
	}

	public String getHeadingComment() {
		return headingComment;
	}

	public void setHeadingComment(String headingComment) {
		this.headingComment = headingComment;
	}

	public String getSubjectProperty() {
		return subjectProperty;
	}

	public void setSubjectProperty(String subjectProperty) {
		this.subjectProperty = subjectProperty;
	}
	


}
