/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package route250.unixdomainsocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketOption;
import java.net.SocketOptions;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.net.UnixDomainSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.net.SocketFactory;

/**
 *
 * @author maeda
 */
public final class UnixDomainSocketFactory extends SocketFactory {
    
    private static Set<String> sLocalHosts = new HashSet<>();
    private static List<InetAddress> sLocalAddress = new ArrayList<>();
    private static long sLocalAddressReloadTime;
    private final Function<Integer, Path> mToPath;
    private Map<Integer, UnixDomainSocketAddress> mPortToSocketAddress = new LinkedHashMap<>();
    private static final UnixDomainSocketAddress SOCKET_FILE_NOT_EXISTS = UnixDomainSocketAddress.of(Paths.get("/"));
    private long mSocketAddressReloadTime;

    public UnixDomainSocketFactory(Function<Integer, Path> aToPath) {
        mToPath = aToPath;
    }

    public static boolean isLocal(String aHost) {
        reloadLocalAddress(System.currentTimeMillis());
        if (sLocalHosts.contains(aHost)) {
            return true;
        }
        try {
            return isLocal(InetAddress.getByName(aHost));
        } catch (UnknownHostException ex) {
        }
        return false;
    }

    public static boolean isLocal(InetAddress aHost) {
        for (InetAddress u : sLocalAddress) {
            if (u.equals(aHost)) {
                return true;
            }
        }
        return false;
    }

    public static void reloadLocalAddress(long zNow) {
        if (zNow >= (sLocalAddressReloadTime + 5000)) {
            sLocalAddressReloadTime = zNow;
            Set<String> zHosts = new HashSet<>();
            List<InetAddress> zList = new ArrayList<>();
            try {
                NetworkInterface.networkInterfaces().forEach(eth -> eth.inetAddresses().forEach(x -> zList.add(x)));
                for (InetAddress a : zList) {
                    zHosts.add(a.getCanonicalHostName());
                    zHosts.add(a.getHostName());
                    zHosts.add(a.getHostAddress());
                }
                sLocalHosts = zHosts;
                sLocalAddress = zList;
            } catch (SocketException ex) {
            }
        }
    }

    private void reloadSocketAddress() {
        long zNow = System.currentTimeMillis();
        reloadLocalAddress(zNow);
        if (zNow >= (mSocketAddressReloadTime + 1000)) {
            mSocketAddressReloadTime = zNow;
            mPortToSocketAddress = new LinkedHashMap<>();
        }
    }

    private UnixDomainSocketAddress toSocketPath(String aHost, Integer aPort) throws UnknownHostException, NoRouteToHostException, PortUnreachableException {
        reloadSocketAddress();
        if (!sLocalHosts.contains(aHost)) {
            return toSocketPath(InetAddress.getByName(aHost), aPort);
        }
        return toSocketPath(aPort);
    }

    private UnixDomainSocketAddress toSocketPath(InetAddress aHost, Integer aPort) throws NoRouteToHostException, PortUnreachableException {
        reloadSocketAddress();
        boolean exists = false;
        for (InetAddress u : sLocalAddress) {
            if (u.equals(aHost)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            throw new NoRouteToHostException(aHost.getHostAddress());
        }
        return toSocketPath(aPort);
    }

    private UnixDomainSocketAddress toSocketPath(Integer aPort) throws PortUnreachableException {
        UnixDomainSocketAddress zAddress = mPortToSocketAddress.get(aPort);
        if (zAddress == null) {
            Path zPath = toPath(aPort);
            if (!Files.exists(zPath) || Files.isDirectory(zPath) || Files.isRegularFile(zPath)) {
                zAddress = SOCKET_FILE_NOT_EXISTS;
            } else {
                zAddress = UnixDomainSocketAddress.of(zPath);
            }
            mPortToSocketAddress.put(aPort, zAddress);
        }
        if (zAddress == SOCKET_FILE_NOT_EXISTS) {
            throw new PortUnreachableException("" + aPort);
        }
        return zAddress;
    }

    private Path toPath(Integer aPort) throws PortUnreachableException {
        try {
            return mToPath.apply(aPort);
        } catch (Error | Exception ex) {
            String zMesg = ex.getMessage() != null ? " " + ex.getMessage() : "";
            throw new PortUnreachableException("port " + aPort + zMesg);
        }
    }

    @Override
    public Socket createSocket(String aHost, int aPort) throws IOException, UnknownHostException {
        return new UnixDomainSocket(aHost, aPort);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Socket createSocket(InetAddress aHost, int aPort) throws IOException {
        return new UnixDomainSocket(aHost, aPort);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public class UnixDomainSocket extends Socket {

        final UnixDomainSocketImpl mImpl;

        public UnixDomainSocket(String aHost, Integer aPort) throws SocketException, IOException {
            this(new UnixDomainSocketImpl(aHost, aPort));
        }

        public UnixDomainSocket(InetAddress aHost, Integer aPort) throws SocketException, IOException {
            this(new UnixDomainSocketImpl(aHost, aPort));
        }

        private UnixDomainSocket(UnixDomainSocketImpl impl) throws SocketException {
            super(impl);
            mImpl = impl;
        }
    }

    public class UnixDomainSocketImpl extends SocketImpl {

        SocketChannel mSocketChannel;

        public UnixDomainSocketImpl(String aHost, Integer aPort) throws IOException, UnknownHostException {
            toSocketPath(aHost, aPort);
        }

        public UnixDomainSocketImpl(InetAddress aHost, Integer aPort) throws IOException, UnknownHostException {
            toSocketPath(aHost, aPort);
        }

        @Override
        protected void create(boolean stream) throws IOException {
            if (stream) {
                mSocketChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
            } else {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        @Override
        protected void connect(String aHost, int aPort) throws IOException {
            UnixDomainSocketAddress zPath = toSocketPath(aHost, aPort);
            port = aPort;
            mSocketChannel.connect(zPath);
        }

        @Override
        protected void connect(InetAddress aHost, int aPort) throws IOException {
            UnixDomainSocketAddress zPath = toSocketPath(aHost, aPort);
            port = aPort;
            mSocketChannel.connect(zPath);
        }

        @Override
        protected void connect(SocketAddress address, int timeout) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected void bind(InetAddress host, int port) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected void listen(int backlog) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected void accept(SocketImpl s) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected InputStream getInputStream() throws IOException {
            return Channels.newInputStream(mSocketChannel);
        }

        @Override
        protected void shutdownInput() throws IOException {
            mSocketChannel.shutdownInput();
        }

        @Override
        protected OutputStream getOutputStream() throws IOException {
            return Channels.newOutputStream(mSocketChannel);
        }

        @Override
        protected void shutdownOutput() throws IOException {
            mSocketChannel.shutdownOutput();
        }

        @Override
        protected int available() throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected void close() throws IOException {
            mSocketChannel.close();
        }

        @Override
        protected void sendUrgentData(int data) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        private static SocketOption opt(int optID) throws SocketException {
            switch (optID) {
                case SocketOptions.SO_RCVBUF:
                    return StandardSocketOptions.SO_RCVBUF;
                case SocketOptions.SO_SNDBUF:
                    return StandardSocketOptions.SO_SNDBUF;
                case SocketOptions.SO_LINGER:
                    return StandardSocketOptions.SO_LINGER;
                case SocketOptions.SO_REUSEADDR:
                    return StandardSocketOptions.SO_REUSEADDR;
                case SocketOptions.SO_REUSEPORT:
                    return StandardSocketOptions.SO_REUSEPORT;
                case SocketOptions.SO_KEEPALIVE:
                    return StandardSocketOptions.SO_KEEPALIVE;
                case SocketOptions.TCP_NODELAY:
                    return StandardSocketOptions.TCP_NODELAY;
                case SocketOptions.IP_TOS:
                    return StandardSocketOptions.IP_TOS;
                case SocketOptions.SO_OOBINLINE:
                    throw new UnsupportedOperationException("'SO_OOBINLINE' not supported");
                case SocketOptions.SO_TIMEOUT:
                    throw new UnsupportedOperationException("'SO_TIMEOUT' not supported");
                case SocketOptions.SO_BINDADDR:
                    throw new UnsupportedOperationException("'SO_BINDADDR' not supported");
                    //                        return ExtendedSocketOptions.SO_PEERCRED;
            }
            throw new UnsupportedOperationException("#" + optID + " not supported");
        }

        @Override
        public void setOption(int optID, Object value) throws SocketException {
            try {
                mSocketChannel.setOption(opt(optID), value);
            } catch (IOException | UnsupportedOperationException ex) {
                throw new SocketException(ex.getMessage());
            }
        }

        @Override
        public Object getOption(int optID) throws SocketException {
            try {
                return mSocketChannel.getOption(opt(optID));
            } catch (IOException | UnsupportedOperationException ex) {
                throw new SocketException(ex.getMessage());
            }
        }

        @Override
        protected Set<SocketOption<?>> supportedOptions() {
            return mSocketChannel.supportedOptions();
        }
    }
    
}
