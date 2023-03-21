/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package route250.unixdomainsocket;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.ThreadContext;

/**
 *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
 * @author maeda
 */
public class skt {
    public static void main( String[] aArgs ) throws SocketException, UnknownHostException {
        try {
            ThreadContext.put("trackID", "x");
        } catch(Error|Exception ex ) {}
        try {
            ThreadContext.get("trackID");
        } catch(Error|Exception ex ) {}
        Path Path ;
        InetAddress local = InetAddress.getByName("t430");
        dump( "t430", local);
        
        Set<String> zHosts = new HashSet<>();
        List<InetAddress> zList = new ArrayList<>();
        NetworkInterface.networkInterfaces().forEach( (eth) -> eth.inetAddresses().forEach( (x)-> zList.add(x)));
        for( InetAddress a : zList ) {
            zHosts.add( a.getCanonicalHostName() );
            zHosts.add( a.getHostName() );
            zHosts.add( a.getHostAddress() );
        }

//        UnixDomainSocketImpl impl = new UnixDomainSocketImpl();
//        Socket skt = new UnixDomainSocket(impl);
        
    }
    
    public static void dump( String a, InetAddress addr ) {
        System.out.println("["+a+"]");
        System.out.println("getHostName:"+addr.getHostName());
        System.out.println("getCanonicalHostName:"+addr.getCanonicalHostName());
        System.out.println("getHostAddress:"+addr.getHostAddress());
        System.out.println("isAnyLocalAddress:"+addr.isAnyLocalAddress());
        System.out.println("isLinkLocalAddress:"+addr.isLinkLocalAddress());
        System.out.println("isLoopbackAddress:"+addr.isLoopbackAddress());
        System.out.println("isSiteLocalAddress:"+addr.isSiteLocalAddress());
        System.out.println("isLoopbackAddress:"+addr.isLoopbackAddress());
        System.out.println("isLoopbackAddress:"+addr.isLoopbackAddress());
        System.out.print("addr:");
        byte[] ip = addr.getAddress();
        for( int i=0;i<ip.length;i++ ) {
            if( i>0 ) System.out.print(".");
            System.out.print(""+(ip[i]&0xff));
        }
        System.out.println();
    }
    
}
