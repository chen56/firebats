package firebats.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.Cookie;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;

public class Cookies {

	private HttpServerRequest<ByteBuf> request;
	private HttpServerResponse<ByteBuf> response;

	/*internal*/ Cookies(HttpServerRequest<ByteBuf> request,
			HttpServerResponse<ByteBuf> response) {
		this.request=request;
		this.response=response;
	}
	public Optional<Cookie> getFirst(String cookieName) {
		Set<Cookie> c =cookies().get(cookieName);
		if(c==null||c.size()==0)return Optional.absent();
		return Optional.of(c.iterator().next());
	}
	public Optional<String> getFirstValue(String cookieName) {
		Optional<Cookie> c = getFirst(cookieName);
		if(!c.isPresent()){
			return Optional.<String>absent();
		}
 		return Optional.fromNullable(c.get().getValue());
	}
    public void addCookie(Cookie cookie) {
        response.addCookie(cookie);
    }
    
	private Map<String, Set<Cookie>> cookies(){
		return  request.getCookies();
	}
	@Override
    public String toString(){
    	return request.getCookies().toString();
    }
}
