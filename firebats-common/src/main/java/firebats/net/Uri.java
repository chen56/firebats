package firebats.net;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.net.HostAndPort;

public class Uri {
	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private String scheme;
	private String host;
	private int port;
	private List<String> pathParts;
	private String fragment;
	private Multimap<String, String> parameters;
    
	//http://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xhtml?&page=136
	private static Map<String,Integer> DefaultPorts=new LinkedHashMap<>();
	static {
		DefaultPorts.put("http",80);
		DefaultPorts.put("https",443);
		DefaultPorts.put("ws",80);
		DefaultPorts.put("wss",443);
	}
	
	
	private Uri(){

	}
	
	public static UriBuilder buildFrom(String uri) {
		return UriBuilder.parse(uri);
	}
	public static Uri parse(String uri) {
		return UriBuilder.parse(uri).build();
	}

    public boolean containsParam(String key){
    	return parameters.containsKey(key);
    }
    
    public String getFirstParam(String key){
    	Iterator<String> result = parameters.get(key).iterator();
    	return result.hasNext()?result.next():null;
    }
    
    public Collection<String> getParams(String key){
     	return parameters.get(key);
    }

	public String getPath() {
		return toURI().getPath();
	}
	public String getHost() {
 		return host;
	}
	public int getPort() {
 		return port;
	}
	/**
	 * 若Port==-1,则此方法按照标准的端口注册表查找默认端口:
	 * http://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xhtml</p>
	 * Uri目前支持的协议参考{@link #getDefaultPorts()}}
	 */
	public int getPortResolveDefault(){
		if(port<0){
			if(scheme!=null){
				Integer result = DefaultPorts.get(scheme.toLowerCase());
				if(result!=null)return result;
			}
		}
		return port;
	}
	
	/**
	 * 默认端口注册表:
	 * http://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xhtml</p>
	 * @param 返回目前Uri类支持的缺省协议和端口表
	 */
    public static Map<String,Integer> getDefaultPorts(){
    	return ImmutableMap.copyOf(DefaultPorts);
    }

	public HostAndPort getHostPort() {
		return HostAndPort.fromParts(getHost(),getPort());
	}
	public String getScheme() {
		return scheme;
	}
	public String getFragment() {
		return fragment;
	}
    public List<String> getPathParts(){
    	return Collections.unmodifiableList(pathParts);
    }

	public String toHostAndPortString() {
		return port>0?host+":"+port:host;
	}
	
	public String toHostPortPathString() {
		return String.format("%s%s",toHostAndPortString(),getPath());
	}
	
    public static String toQueryString(
    		Multimap<String, String> parameters, String spliter,
            boolean encode) {
        StringBuilder builder = new StringBuilder();
        for (Iterator<String> keys = parameters.keySet().iterator(); keys
                .hasNext();) {
            String key = keys.next();
            for (Iterator<String> values = parameters.get(key).iterator(); values
                    .hasNext();) {
                String value = values.next();
                builder.append(encode ? encodeURL(key) : key).append("=");
                if (value != null) {
                    builder.append(encode ? encodeURL(value.toString())
                            : value.toString());
                }
                if (values.hasNext()) {
                    builder.append(spliter);
                }
            }
            if (keys.hasNext()) {
                builder.append(spliter);
            }
        }
        return builder.toString();
    }
    
    public URI toURI() {
        return getURI(getQuery(false));
	}

    public UriBuilder toUriBuilder(){
		return UriBuilder.from(this);
    }
    
    public URI toURIWithoutQuery() {
        return getURI(null);
    }
    
    /*
     * 参数集合拼为uri查询串 [{a:[1,2]},b:1] -> a=1&a=2&b=1
     */
    public String getQuery(boolean encode) {
        return toQueryString(parameters, "&", encode);
    }

    /**
     * get请求uri，参数将拼为uri query部分
     */
    private URI getURI(String query) {
        try {
            query = Strings.isNullOrEmpty(query) ? null : query;
            String path = "/"
                    + Joiner.on("/").join(pathParts);
            return new URI(scheme, null, host, port, path, query, fragment);
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
	public String getPathAndQuery(boolean encode) {
		StringBuilder result=new StringBuilder();
		result.append(getPath());
		if(!parameters.isEmpty()){
			result.append("?"+getQuery(encode));
		}
		if(!Strings.isNullOrEmpty(fragment)){
			result.append("#"+getQuery(encode));
		}
		return result.toString();
	}

    @Override public String toString() {
     	return toURI().toString();
    }
    @Override
    public final boolean equals(Object other) {
		if(this==other)return true;
		if(!(other instanceof Uri)) return false;
		if(!canEqual(other)) return false;
			
		Uri that = (Uri) other;
		return Objects.equal(this.fragment,that.fragment)
       		 &&Objects.equal(this.host,that.host)
       		 &&Objects.equal(this.parameters,that.parameters)
       		 &&Objects.equal(this.pathParts,that.pathParts)
       		 &&Objects.equal(this.port,that.port)
       		 &&Objects.equal(this.scheme,that.scheme)
       		 ;
    }
	
	public boolean canEqual(Object other) {
		return (other instanceof Uri);
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(this.fragment,this.host,this.parameters,this.pathParts,this.port,this.scheme);
	}

	public static String encodeURL(String data) {
		if(Strings.isNullOrEmpty(data)){
			return data;
		}

		try {
			return URLEncoder.encode(data, DEFAULT_CHARSET.displayName());
		} catch (UnsupportedEncodingException wontHappen) {
			throw new IllegalStateException(wontHappen);
		}
	}

	public static String decodeURL(String data) {
		if(Strings.isNullOrEmpty(data)){
			return data;
		}
		try {
			return URLDecoder.decode(data, DEFAULT_CHARSET.displayName());
		} catch (UnsupportedEncodingException wontHappen) {
			throw new IllegalStateException(wontHappen);
		}
	}
    /*
     * 分隔uri query字符串部分为数组： a=1&a=2&b=1 -> [{a:[1,2]},b:[1]]
     */
    public static Multimap<String, String> toParmeters(String uriQuery) {
    	Multimap<String, String> result = MultimapBuilder.linkedHashKeys().linkedListValues().build();

//        MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>();
        if (uriQuery == null) {
            return result;
        }
        String[] p = uriQuery.split("&");
        for (String nameValue : p) {
            String[] splittedElement = nameValue.split("=");
            if (splittedElement.length == 1) {
                result.put(splittedElement[0].trim(), null);
            }
            if (splittedElement.length == 2) {
                String key = splittedElement[0];
                String value = splittedElement[1];
                if (!Strings.isNullOrEmpty(key)
                        && !Strings.isNullOrEmpty(value)) {
                    result.put(key, value);
                }
            }
        }
        return result;
    }
//    private static boolean isHttps(String scheme){
//    	return Objects.equal(scheme,"https")||Objects.equal(scheme,"wss");
//    }
//    private static boolean isHttp(String scheme){
//    	return Objects.equal(scheme,"http")||Objects.equal(scheme,"ws");
//    }
//    private boolean isDefaultPort(){
//    	return (isHttps(scheme)&&port==443 )||(isHttp(scheme)&&port==80) ;
//    }
	public static class UriBuilder {
	    private String scheme;
		private String host;
		private int port;
		private List<String> pathParts;
		private String fragment;
		private Multimap<String, String> parameters;

		public static UriBuilder parse(String uri){
			try {
		    	 UriBuilder result = new UriBuilder();
		         URI u = new URI(uri);
		         result.scheme = u.getScheme();
		         if (result.scheme != null) {
		        	 result.scheme = result.scheme.toLowerCase();
		         }
		         result.host = u.getHost();
		         result.port = u.getPort();
		         result.pathParts = toPathParts(u.getPath());
		         result.fragment = u.getFragment();
		         result.parameters = toParmeters(u.getQuery());
		         return result;
			} catch (URISyntaxException e) {
				throw new RuntimeException("error parse uri:"+uri,e);
			}
	    }
 
		public static UriBuilder from(Uri uri) {
	    	UriBuilder result=new UriBuilder();
	        result.scheme = uri.scheme;
	        result.host = uri.host;
	        result.port = uri.port;
	        result.pathParts = uri.pathParts;
	        result.fragment = uri.fragment;
	        result.parameters = uri.parameters;
			return result;
		}
        
		public Uri build(){
			 Uri result=new Uri();
	         result.scheme = scheme;
	         result.host = host;
	         result.port = port;
	         result.pathParts = pathParts;
	         result.fragment = fragment;
	         result.parameters = parameters;
			return result;
		}
		
		@Override
		public UriBuilder clone()  {
			UriBuilder result = new UriBuilder();
			result.scheme = this.scheme;
			result.host = this.host;
			result.port = this.port;
			result.pathParts = new ArrayList<String>(this.pathParts);
			result.fragment = this.fragment;
			result.parameters = MultimapBuilder.linkedHashKeys().linkedHashSetValues().build(this.parameters);;
			return result;
		}
		
	    /**
	     * 给请求添加参数， 若是get请求，则参数将拼为uri query部分 若是post请求，则参数作为form内容体提交
	     */
	    public UriBuilder addParam(String key, Object value) {
	        Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "参数名不能为空");
	        parameters.put(key.trim(), value==null?null:value.toString());
	        return this;
	    }
		public UriBuilder port(Integer port) {
			this.port=port;
 	        return this;
		}
		public UriBuilder host(String host) {
			this.host=host;
 	        return this;
		}
		public UriBuilder fragment(String fragment) {
			this.fragment=fragment;
 	        return this;
		}
		public UriBuilder scheme(String scheme) {
			this.scheme=scheme;
 	        return this;
		}

	     /**
	     * 添加参数
	     */
	    public UriBuilder addParams(String key, Object ... values) {
	    	Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "参数名不能为空");
	        for (Object value : values) {
	        	addParam(key,value);
	        }
	        return this;
	    }

	    public UriBuilder removeParam(String key){
	    	parameters.removeAll(key);
	    	return this;
	    }
	    
	    /**
	     * 给uri上添加path部分，例如: a/b , a/b/c
	     */
	    public UriBuilder addPath(String pathPart) {
//	        checkArgument(!Strings.isNullOrEmpty(pathPart), "pathPart参数不能为空");
	    	if(Strings.isNullOrEmpty(pathPart)){
	    		return this;
	    	}
	        pathParts.addAll(toPathParts(pathPart));
	        return this;
	    }
		
		/*
	     * 分隔uri path字符串部分为数组： /a/b -> [a,b]
	     */
	    private static List<String> toPathParts(String uriPath) {
	        String[] paths = uriPath.split("/");
	        List<String> result = new ArrayList<String>();
	        for (String pathPart : paths) {
	            if (!Strings.isNullOrEmpty(pathPart)) {
	                result.add(pathPart.trim());
	            }
	        }
	        return result;
	    }
	}
}