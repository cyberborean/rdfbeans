
package org.cyberborean.rdfbeans.datatype;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utilities for parsing dates.
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
