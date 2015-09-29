package firebats.net;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Collections;

import com.google.common.base.Optional;

public class InterfaceAddresses {

    public static Optional<InetAddress> guessIpAddress(){
        try {
			for (NetworkInterface netint : Collections.list(NetworkInterface.getNetworkInterfaces())){
				if(!netint.isUp())continue;
				if(netint.isLoopback())continue;
				if(netint.isVirtual())continue;
				if(netint.isPointToPoint())continue;
				
			    for (InetAddress inetAddress : Collections.list(netint.getInetAddresses())) {
					if(!(inetAddress instanceof Inet4Address)) continue;
					Inet4Address net4=(Inet4Address)inetAddress;
					if(net4.isLoopbackAddress())continue;
					if(net4.isLinkLocalAddress())continue;
					if(net4.isAnyLocalAddress())continue;
					if(net4.isMulticastAddress())continue;
					if(!net4.isSiteLocalAddress())continue;
					return Optional.<InetAddress>of(net4);
				}
			}
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
        return Optional.absent();
    }
    
	public static int getRandomPort() {
		ServerSocket server = null;
		try {
			server = new ServerSocket(0);
			return server.getLocalPort();
		} catch (IOException e) {
			throw new Error(e);
		} finally {
			if (server != null) {
				try {
					server.close();
				} catch (IOException ignore) {
					// ignore
				}
			}
		}
	}

}
