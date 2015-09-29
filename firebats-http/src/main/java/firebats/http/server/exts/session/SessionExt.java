package firebats.http.server.exts.session;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;

import java.util.Map;
import java.util.Set;

import rx.Observable;
import rx.functions.Action0;

import com.google.common.base.Preconditions;

import firebats.http.server.Chain;
import firebats.http.server.Context;
import firebats.http.server.Ext;

public class SessionExt implements Ext<Context,Context> {
	private static final String ATTR = "ext.session";
	private String secretKey;
	private String cookieNameOfSession;
	private String cookieDomain;

    public SessionExt(String secretKey,String cookieNameOfSession,String cookieDomain){
    	this.secretKey=secretKey;
    	this.cookieNameOfSession=cookieNameOfSession;
    	this.cookieDomain=cookieDomain;
    }
	
	@Override
	public Observable<?> call(Chain<Context,Context>chain,final Context ctx) {
		//没有取过内容则取之
		if(!contains(ctx)) {
	    	HttpSession session=parse(ctx.getRequest());
		    ctx.getAttrs().put(ATTR, session);
		}
    	return chain.next(ctx).doOnTerminate(new Action0(){
			@Override
			public void call() {
				HttpSession session=SessionExt.get(ctx);
				if(session!=null&&session.isChanged()){
					ctx.getResponse().getHeaders().addHeader(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(session.toCookie()));
				}
			}
    	});
	}
	
	public static boolean contains(Context ctx){
		return ctx.getAttrs().containsKey(ATTR);
	}
	
 	/**@reutrn get 已处理好的Content
	 * 
	 * @throws NullPointerException 若没有经过{@link firebats.http.server.ext.content.PickupContentExt} 处理，则抛出异常
	 */
	public static HttpSession get(Context ctx){
		return Preconditions.checkNotNull(getOrNull(ctx),"dependences SessionExt,are you chain it?");
	}
 	/**
	 * @return get 已处理好的Content,当没有经过 {@link firebats.http.server.ext.content.PickupContentExt} 处理时，返回null
	 * */
	public static HttpSession getOrNull(Context ctx){
		return (HttpSession)ctx.getAttrs().get(ATTR);
	}

	private HttpSession parse(HttpServerRequest<ByteBuf> request) {
		Map<String, Set<Cookie>> cookies = request.getCookies();
		Set<Cookie> result = cookies.get(cookieNameOfSession);
		Cookie sessionCookie=result==null||result.size()==0?null:result.iterator().next();
		Preconditions.checkNotNull(secretKey,"secretKey should be config");
		Preconditions.checkNotNull(cookieNameOfSession,"cookieNameOfSession should be config");
		return HttpSession.builder(secretKey, cookieNameOfSession).setDomain(cookieDomain).buildFromCookie(sessionCookie);
	}
}