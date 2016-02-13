/**
 * 
 */
package com.viceversatech.rdfbeans.datatype;

import java.text.ParseException;

import junit.framework.TestCase;

/**
 * @author alex
 *
 */
public class DateUtilsTest extends TestCase {

	public void test()  {
		try {
			DateUtils.parseDate("2014-04-10T23:00:00.000Z");
			DateUtils.parseDate("2014-04-10T23:00:00Z");
			DateUtils.parseDate("2014-04-10T23:00Z");
			DateUtils.parseDate("2014-04-10T23:00:00-04:00");
		}
		catch (ParseException e) {
			fail(e.getMessage());
		}
	}
}
