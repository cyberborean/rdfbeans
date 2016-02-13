/**
 * RDFBeansTests.java
 * 
 * RDFBeans Feb 7, 2011 12:08:33 PM alex
 *
 * $Id: RDFBeansTestSuite.java 40 2014-04-05 04:07:09Z alexeya $
 *  
 */
package com.viceversatech.rdfbeans.test;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.viceversatech.rdfbeans.datatype.DatatypeTest;
import com.viceversatech.rdfbeans.test.examples.ExampleClassTest;
import com.viceversatech.rdfbeans.test.examples.ExampleIFaceTest;
import com.viceversatech.rdfbeans.test.foafexample.FOAFExampleProxyTest;
import com.viceversatech.rdfbeans.test.foafexample.FOAFExampleTest;
import com.viceversatech.rdfbeans.test.proxy.RDFBeanProxyTest;

/**
 * RDFBeansTests.
 *
 * @author alex
 *
 */
public class RDFBeansTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite(RDFBeansTestSuite.class.getName());
		//$JUnit-BEGIN$
		suite.addTestSuite(DatatypeTest.class);
		suite.addTestSuite(FOAFExampleTest.class);
		suite.addTestSuite(FOAFExampleProxyTest.class);
		suite.addTestSuite(ExampleClassTest.class);
		suite.addTestSuite(ExampleIFaceTest.class);
		suite.addTestSuite(RDFBeanProxyTest.class);
		//$JUnit-END$
		return suite;
	}

}
