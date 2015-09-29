package firebats.http.server.exts.session;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultCookie;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

/**
 * TODO 陈鹏  凌乱，重构
 * 移植出playframework 1的play.mvc.Session，机理是，session生成一串文本放到一个特殊名称的cookie里。
 * <pre>
 * //从配置获取
 * String secretKey="asdfasdfasdfasdfasdf";
 * String cookieNameOfSession="XXX_SESSION";
 * 
 * //从http头中装载session对应的cookie值
 * HttpSession session=HttpSession.Empty(secretKey,cookieNameOfSession);
 * Cookie cookie=request.getCookie(cookieNameOfSession);//netty
 * HttpSession session = session.readFromCookie(cookie.getValue());
 * 
 * //像map一样使用session
 * session.put("uid","1234");
 * 
 * //如果session值有变化，重新把session写入cookie
 * if(session.isChanged()){
 *    Cookie newCookie=session.toCookie();
 *    response.setCookie(cookieNameOfSession,newCookie);
 * }
 * ...
 * </pre>
 * 
 * @Deprecated
 * 每个项目制定自己的session机制，firebats将不再内置session
 */
@Deprecated
public class HttpSession implements Map<String,String> {
	private static Pattern sessionParser = Pattern
			.compile("\u0000([^:]*):([^\u0000]*)\u0000");
//	private static final String AT_KEY = "___AT";
//	private static final String ID_KEY = "___ID";
	private static final String TS_KEY = "___TS";

	private static Logger log = LoggerFactory.getLogger(HttpSession.class);

	private final String secretKey;
	private final Map<String, String> data;
	private boolean changed = false;
	private final String cookieNameOfSession;
	
    private ExpireMode expireMode=ExpireMode.OnlyCookie;
    //默认永不过期
	private long maxAgeMillis=Long.MAX_VALUE;
	private String domain;
	
	private HttpSession(String secretKey, String cookieNameOfSession,String domain,Map<String,String> data, ExpireMode expireMode, long maxAgeMillis, boolean changed) {
		this.secretKey = secretKey;
		this.cookieNameOfSession = cookieNameOfSession;
		this.domain=domain;
		this.data = data;
		this.expireMode = expireMode;
		this.maxAgeMillis = maxAgeMillis;
		this.changed=changed;
	}

	/**
	 * 
	 * @param secretKey
	 *            应传入Play.secretKey参数
	 * @param cookieNameOfSession
	 *            session對應的cookie名
	 */
	public static HttpSessionBuilder builder(String secretKey, String cookieNameOfSession) {
		Preconditions.checkNotNull(secretKey,"secretKey should be not null");
		Preconditions.checkNotNull(cookieNameOfSession,"cookieNameOfSession should be not null");
		return new HttpSessionBuilder(secretKey, cookieNameOfSession);
	}
	public long getMaxAgeMillis() {
		return maxAgeMillis;
	}

	/**
	 * 测试环境的header: _wns-request-timespan:10 Cache-Control:no-cache
	 * Content-Length:0 Content-Type:text/plain; charset=utf-8
	 * Location:http://192.168.1.200:9992/hfers/admin/index Server:Play!
	 * Framework;1.2.5;dev Set-Cookie:PLAY_FLASH=;Expires=Thu, 12-Dec-2013
	 * 10:54:40 GMT;Path=/ Set-Cookie:PLAY_ERRORS=;Expires=Thu, 12-Dec-2013
	 * 10:54:40 GMT;Path=/
	 * Set-Cookie:PLAY_SESSION=1d93ceb48e0a590cc74bdc15ab414f172908ee6f
	 * -%00aid%3A50c592bc6a4340abce4cc858%00;Path=/
	 * 
	 * 使用方式：
	 * httpResponse.addHeader(io.netty.handler.codec.http.HttpHeaders.Names
	 * .SET_COOKIE, session.encode());
	 * 
	 * @return
	 */
	public String toCookieString() {
		return toCookie().toString();
	}
	public Cookie toCookie() {
        StringBuilder session = new StringBuilder();
        for (String key : data.keySet()) {
            session.append("\u0000");
            session.append(key);
            session.append(":");
            session.append(data.get(key));
            session.append("\u0000");
        }
        String sessionData = Codec.encodeURL(session.toString());
        String sign = Crypto.sign(sessionData, secretKey.getBytes());
        String value = sign + "-" + sessionData;
        DefaultCookie cookie=new DefaultCookie(cookieNameOfSession,value);
    	cookie.setPath("/");
        cookie.setDomain(domain);
        if (expireMode.isInCookie()) {
        	cookie.setMaxAge(TimeUnit.MILLISECONDS.toSeconds(maxAgeMillis));
        }
        return cookie;
	}
	
	public String getCookieNameOfSession() {
		return cookieNameOfSession;
	}

	public Map<String, String> copyWithoutInternalData() {
		HashMap<String, String> result = Maps.newHashMap(data);
		result.remove(TS_KEY);
		return result;
	}
//目前没用
//	public String getId() {
//		if (!data.containsKey(ID_KEY)) {
//			data.put(ID_KEY, UUID.randomUUID().toString());
//		}
//		return data.get(ID_KEY);
//	}
//
//
//	public String getAuthenticityToken() {
//		if (!data.containsKey(AT_KEY)) {
//			data.put(AT_KEY,Crypto.sign(UUID.randomUUID().toString(), secretKey));
//		}
//		return data.get(AT_KEY);
//	}

	private void change() {
		changed = true;
	}
	
	public boolean isChanged() {
		return changed;
	}
	
	@Override public void clear() {
		change();
		data.clear();
	}

	/**
	 * Returns true if the session is empty, e.g. does not contain anything else
	 * than the timestamp
	 */
	@Override public boolean isEmpty() {
		return copyWithoutInternalData().isEmpty();
	}

	@Override public int size() {
		return copyWithoutInternalData().size();
	}

	@Override public boolean containsKey(Object key) {
		return data.containsKey(key);
	}

	@Override public boolean containsValue(Object value) {
		return data.containsValue(value);
	}

	@Override public String get(Object key) {
		return data.get(key);
	}

	@Override public String put(String key, String value) {
		if (key.contains(":")) { throw new IllegalArgumentException(
				"Character ':' is invalid in a session key."); }
		change();
		if (value == null) {
			return data.remove(key);
		} else {
			return data.put(key, value);
		}
	}
	public void put(String key, Object value) {
		change();
		if (value == null) {
			put(key, (String) null);
		}
		put(key, value + "");
	}


	@Override public String remove(Object key) {
		change();
		return data.remove(key);
	}
 	public void remove(String... keys) {
		for (String key : keys) {
			remove(key);
		}
	}

	@Override public void putAll(Map<? extends String, ? extends String> m) {
		for (Map.Entry<? extends String, ? extends String> e : m.entrySet()) {
			put(e.getKey(),e.getValue());
		}
	}

	@Override public Set<String> keySet() {
		return copyWithoutInternalData().keySet();
	}

	@Override public Collection<String> values() {
		return copyWithoutInternalData().values();
	}

	@Override public Set<java.util.Map.Entry<String, String>> entrySet() {
		return copyWithoutInternalData().entrySet();
	}
	
	@Override public String toString() {
		return copyWithoutInternalData().toString();
	}
	
	/**过期模式，影响cookie的设置*/
    public static enum ExpireMode{
    	/**只設置http cookie過期時間，session里並不檢查*/
    	OnlyCookie,
    	/**設置http cookie過期時間，并設置session內部的過期時間
    	 * 需注意，這種模式將導致每個請求都在Header里重新設置session cookie, 即添加SET-COOKIE頭，原因是每次調用后都導致過期時間的推遲
    	 * */
    	CookieAndSession;
		public boolean isInCookie() {
			return Objects.equal(ExpireMode.this, OnlyCookie)||Objects.equal(ExpireMode.this, CookieAndSession);
		}
		public boolean isInSession() {
			return Objects.equal(ExpireMode.this, CookieAndSession);
		}
    }

    public static class HttpSessionBuilder{
		private String secretKey;
		private String cookieNameOfSession;
		private long currentTimeMillis = System.currentTimeMillis();
	    private ExpireMode expireMode=ExpireMode.OnlyCookie;
		private Long maxAgeMillis=TimeUnit.DAYS.toMillis(365);
        private String domain;
		private HttpSessionBuilder(String secretKey, String cookieNameOfSession) {
			this.secretKey=secretKey;
			this.cookieNameOfSession=cookieNameOfSession;
		}
		public HttpSessionBuilder setExpireMode(ExpireMode expireMode,Long maxAge,TimeUnit maxAgeUnit){
			if(expireMode!=null){
				this.expireMode=expireMode;
			}
			if(maxAgeUnit!=null&&maxAge!=null){
				this.maxAgeMillis=maxAgeUnit.toMillis(maxAge);
			}
			return this;
		}
		public HttpSessionBuilder setDomain(String domain){
			this.domain=domain;
			return this;
		}
		public HttpSessionBuilder testUseCurrentTimeMillis(int currentTimeMillis) {
			this.currentTimeMillis=currentTimeMillis;
			return this;
		}

		/**
		 * @param cookieHeader
		 *            应传入整个cookie头
		 */
		public HttpSession buildFromCookiesHeader(String cookieHeader) {
 			if(cookieHeader==null){
				return buildNewEmptySession();
			}
			Set<Cookie> cookies = CookieDecoder.decode(cookieHeader);
			for (Cookie cookie : cookies) {
				if (Objects.equal(cookie.getName(), cookieNameOfSession)) { 
					return buildFromCookie(cookie); 
				}
			}
			return buildNewEmptySession();
		}

		public HttpSession buildNewEmptySession() {
			return buildFromMap(Maps.<String,String>newLinkedHashMap(),/*changed*/false);
		}
		
		private HttpSession buildFromMap(Map<String,String> loaded,boolean changed) {
 	        if (expireMode.isInSession()) {	
	        	loaded.put(TS_KEY, ""+(currentTimeMillis + maxAgeMillis));
            }
			HttpSession result=new HttpSession(secretKey,cookieNameOfSession,domain,loaded,expireMode,maxAgeMillis,changed);
			return result;
		}
		/**
		 * @param cookie
		 *            session对应的cookie
		 */
		public HttpSession buildFromCookie(Cookie cookie) {
			if(cookie==null){
				return buildNewEmptySession();
			}
			return buildFromCookieValue(cookie.getValue());
		}

		/**
		 * @param Cookie
		 *            session对应的cookie
		 */
		public HttpSession buildFromCookieValue(String cookie) {
            if (cookie == null || cookie.trim().equals("")) {
				return buildNewEmptySession();
            }
            Map<String,String> loaded=parse(cookie);
            
            //如果要在session中指定过期
	        if (expireMode.isInSession()) {	
                // Verify that the session contains a timestamp, and that it's not expired
			    if (!loaded.containsKey(TS_KEY)) {
			    	return buildNewEmptySession();
                }
			    
                try {
                	long ts = Long.parseLong(loaded.get(TS_KEY));
    		        if (ts<currentTimeMillis) {
                        // Session expired
    			    	return buildNewEmptySession();
                    }
        			return buildFromMap(loaded,/*changed*/true);
				} catch (NumberFormatException e) {
					log.warn("session expired TS_KEY 非法格式 {}" ,cookie);
			    	return buildNewEmptySession();
				}
            }
            // Just restored. Nothing changed. No cookie-expire.
			return buildFromMap(loaded,/*changed*/false);
		}
		
		private Map<String, String> parse(String cookie) {
			Map<String,String> loaded=new LinkedHashMap<String,String>();
		 	int firstDashIndex = cookie.indexOf("-");
		    if(firstDashIndex > -1) {
            	String sign = cookie.substring(0, firstDashIndex);
            	String data = cookie.substring(firstDashIndex + 1);
            	if (sign.equals(Crypto.sign(data, secretKey.getBytes()))) {
                	String sessionData = Codec.decodeURL(data);
                	Matcher matcher = sessionParser.matcher(sessionData);
                	while (matcher.find()) {
                		loaded.put(matcher.group(1), matcher.group(2));
                	}
            	}
			}			
		    return loaded;
		}
    }
	/**
	 * 完全替代play.libs.Crypto
	 * 
	 * 但不应该轻易修改此类的任何加密算法！！！
	 * 
	 * 
	 * 可使用configuration.getProperty("application.secret")获取应用密钥作为secret参数
	 */
	private static class Crypto {
		// chen56:copy from playframework
		static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6',
				'7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

		/**
		 * chen56:copy from playframework
		 * 
		 * Sign a message with a key
		 * 
		 * @param message
		 *            The message to sign
		 * @param key
		 *            The key to use
		 * @return The signed message (in hexadecimal)
		 */
		public static String sign(String message, byte[] secretKey) {

			if (secretKey.length == 0) { return message; }

			try {
				Mac mac = Mac.getInstance("HmacSHA1");
				SecretKeySpec signingKey = new SecretKeySpec(secretKey,
						"HmacSHA1");
				mac.init(signingKey);
				byte[] messageBytes = message.getBytes("utf-8");
				byte[] result = mac.doFinal(messageBytes);
				int len = result.length;
				char[] hexChars = new char[len * 2];

				for (int charIndex = 0, startIndex = 0; charIndex < hexChars.length;) {
					int bite = result[startIndex++] & 0xff;
					hexChars[charIndex++] = HEX_CHARS[bite >> 4];
					hexChars[charIndex++] = HEX_CHARS[bite & 0xf];
				}
				return new String(hexChars);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

	}

	public static class Codec {
		public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

		public static String encodeURL(String data) {
			if (Strings.isNullOrEmpty(data)) { return data; }

			try {
				return URLEncoder.encode(data, DEFAULT_CHARSET.displayName());
			} catch (UnsupportedEncodingException wontHappen) {
				throw new IllegalStateException(wontHappen);
			}
		}

		public static String decodeURL(String data) {
			if (Strings.isNullOrEmpty(data)) { return data; }
			try {
				return URLDecoder.decode(data, DEFAULT_CHARSET.displayName());
			} catch (UnsupportedEncodingException wontHappen) {
				throw new IllegalStateException(wontHappen);
			}
		}

	}


}