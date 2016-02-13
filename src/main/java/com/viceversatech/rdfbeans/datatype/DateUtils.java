/*
 * DateUtils.java         
 * -----------------------------------------------------------------------------
 * Created           Jan 15, 2009 4:31:20 PM by alex
 * Latest revision   $Revision: 44 $
 *                   $Date: 2014-07-01 18:33:00 +0500 (Вт, 01 июл 2014) $
 *                   $Author: alexeya $
 *
 * @VERSION@ 
 *
 * @COPYRIGHT@
 * 
 * @LICENSE@ 
 *
 * -----------------------------------------------------------------------------
 */


package com.viceversatech.rdfbeans.datatype;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <h1>DateUtils</h1>
 * 
 * <p></p>
 * 
 * @version $Id: DateUtils.java 44 2014-07-01 13:33:00Z alexeya $
 * @author Alex Alishevskikh, alexeya(at)gmail.com
 * 
 */
public class DateUtils {
	
	static final SimpleDateFormat ISO8601DateFormat;
    static final SimpleDateFormat ISO8601DateFormat2; 
    static final SimpleDateFormat ISO8601DateFormat3; 
    
    static final DateFormat[] dateformats;
    
    static {
    	synchronized (DateUtils.class) {
    		ISO8601DateFormat = (SimpleDateFormat) DateFormat
                    .getDateTimeInstance();
        	ISO8601DateFormat.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        	
        	ISO8601DateFormat2 = (SimpleDateFormat) DateFormat
                    .getDateTimeInstance();
        	ISO8601DateFormat2.applyPattern("yyyy-MM-dd'T'HH:mm:ssX");
        	
        	ISO8601DateFormat3 = (SimpleDateFormat) DateFormat
                    .getDateTimeInstance();
        	ISO8601DateFormat3.applyPattern("yyyy-MM-dd'T'HH:mmX");
        	
        	dateformats = new DateFormat[] {
        			ISO8601DateFormat, 
        			ISO8601DateFormat2, 
        			ISO8601DateFormat3, 
        			DateFormat.getInstance(), 
        			DateFormat.getDateInstance()
        	};
		}		
	}
    
    
	/**
	 * @param s
	 * @return
	 * @throws ParseException 
	 */
	public static synchronized Date parseDate(String s) throws ParseException {
		for (DateFormat df: dateformats) {
			try {
				return df.parse(s);
			} catch (ParseException e1) {
				// no-op
			}
		}
		throw new ParseException("Unknown datetime format: " + s, 0);
	}


	public static synchronized DateFormat getDefaultDateFormat() {		
		return ISO8601DateFormat;
	}
		
    
}
