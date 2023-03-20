/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package route250.unixdomainsocket;

import org.apache.logging.log4j.ThreadContext;

/**
 *
 * @author maeda
 */
public class Track {
    public static final String ID = "trackID";
    private static final String Prefix = " #";
    public static void set( String aValue ) {
        try {
            ThreadContext.put(ID, Prefix + aValue);
        } catch(Error|Exception ex ) {}
    }
    public static void append( String aValue ) {
        try {
            String a = strip( ThreadContext.get(ID) );
            if( a == null || a.length()==0 ) {
                ThreadContext.put(ID, Prefix + aValue);
            }
        } catch(Error|Exception ex ) {}
    }
    public static String get() {
        try {
            return ThreadContext.get(ID);
        } catch(Error|Exception ex ) {}
        return null;
    }
    public static void clear() {
        try {
            ThreadContext.clearAll();
        } catch(Error|Exception ex ) {}
    }

    private static boolean isEmpty( String aValue ) {
        return aValue == null || aValue.length()==0;
    }
    private static String strip( String aValue ) {
        int i=0,n = aValue != null ? aValue.length() : 0;
        while( i<n ) {
            char cc = aValue.charAt(i);
            if( cc == ' ' || cc == '#' ) {
                i++;
            } else {
                break;
            }
        }
        if( i>0 ) {
            return aValue.substring(i);
        } else {
            return aValue;
        }
    }
}
