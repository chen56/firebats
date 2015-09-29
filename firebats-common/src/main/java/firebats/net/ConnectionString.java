package firebats.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;

import firebats.converter.Converter;

public class ConnectionString {
	private static String SCHEME_END="://";
    private static String HostPortPlaceholder="TEMP-HOST:9999";
	private List<HostAndPort> hostAndPorts;
	private Uri tempUri;

	public ConnectionString(List<HostAndPort> hostPorts, Uri tempUri) {
		this.tempUri=tempUri;
		this.hostAndPorts=hostPorts;
	}

	public String toString(){
		return tempUri.toString().replace(HostPortPlaceholder, getHostPortsString());
	}

	public static ConnectionString parse(String string) {
		int schemeEnd=string.indexOf(SCHEME_END);
		boolean hasScheme=schemeEnd>0;
		int hostStart =hasScheme?schemeEnd+SCHEME_END.length():0;
 		int pathStart=string.indexOf("/", hostStart); 
		String hostsString=pathStart<0?string.substring(hostStart):string.substring(hostStart, pathStart);
		String[] strHostPorts=hostsString.split(",");
		String tempUri=string.replace(hostsString, HostPortPlaceholder);
		tempUri=hasScheme?tempUri:"temp://"+tempUri;
		List<HostAndPort> hostPorts=new ArrayList<>();
		for (String hostPortStr : strHostPorts) {
			hostPorts.add(HostAndPort.fromString(hostPortStr));
		}
 		return new ConnectionString(hostPorts,Uri.buildFrom(tempUri).build());
	}

	public List<HostAndPort> getHostPorts() {
		return Lists.newArrayList(hostAndPorts);
	}
	
	public String getHostPortsPathString() {
		return String.format("%s%s",getHostPortsString(),tempUri.getPath());
	}

	public String getHostPortsString() {
		return Joiner.on(",").join(hostAndPorts);
	}
	
	public String getPath() {
		return tempUri.getPath();
	}
	public String getPathWithoutSlash() {
		String result=tempUri.getPath();
 		return result.startsWith("/")?result.substring(1):result;
	}

	public String getScheme() {
		return tempUri.getScheme();
	}
	
 	public static Converter<String, ConnectionString> String2ConnectionString() {
		return Converter.of(new Function<String, ConnectionString>() {
			@Override
			public ConnectionString apply(String input) {
				return parse(input);
			}
		});
	}
 	
	public static int getRandomPort() {
		try {
			ServerSocket server = new ServerSocket(0);
			int result = server.getLocalPort();
			server.close();
			return result;
		} catch (IOException e) {
			throw new Error(e);
		}
	}


}
