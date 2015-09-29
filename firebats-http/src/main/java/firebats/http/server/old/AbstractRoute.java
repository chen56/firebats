package firebats.http.server.old;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandlerWithErrorMapper;
import io.reactivex.netty.protocol.http.server.file.FileErrorResponseMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Func1;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import firebats.check.Check;
import firebats.check.Check.NotFound;
import firebats.check.CheckException;
import firebats.http.server.Context;
import firebats.http.server.IRequestHandler;
import firebats.http.server.exts.session.HttpSession;
import firebats.internal.http.server.HttpResult;
import firebats.internal.http.server.rxnetty.ext.DirectoryRequestHandler;
import firebats.net.Path;
/**
 * 
 *  @Deprecated 替代为：使用route ext
 * */
@Deprecated

public abstract class AbstractRoute<CONTEXT extends Context> {
	private static Logger log=LoggerFactory.getLogger(AbstractRoute.class);
    private Path path=Path.ROOT;
	private Map<Path,Route<?>> routes=new LinkedHashMap<>();
	/**TODO 临时配置方式：secretKey和cookieNameOfSession*/
	public static String secretKey;
	public static String cookieNameOfSession;
	public static String cookieDomain;
    
	public Route<CONTEXT> post(String path,IRequestHandler<CONTEXT> handler) {
		return service(path,HttpMethod.POST,handler);
	}
	
	public Route<CONTEXT> get(String path,IRequestHandler<CONTEXT> handler) {
		return service(path,HttpMethod.GET,handler);
	}
	/**
	 *  /app/user/static             resources/static
	 *  /app/user/static/chat.html   resources/static/a.html
	 *  
	 */
	public Route<CONTEXT> files(final String prefixPath,final String files){
		return service(prefixPath,HttpMethod.GET,new IRequestHandler<CONTEXT>(){
			final RequestHandlerWithErrorMapper<ByteBuf, ByteBuf> fileHandler = RequestHandlerWithErrorMapper.from(
	                new DirectoryRequestHandler(files,prefixPath),
	                new FileErrorResponseMapper());
			@Override
			public Observable<?> handle(CONTEXT ctx) {
				return fileHandler.handle(ctx.getRequest(), ctx.getResponse());
			}
		});
	}
	
	public  Route<CONTEXT> service(String path,HttpMethod httpMethod,IRequestHandler<CONTEXT> handler) {
		Path p = Path.fromPortableString(path);
		if(!p.isAbsolute()){
			p=this.path.append(p);
		}

		Route<CONTEXT> result=new Route<CONTEXT>(p,handler,httpMethod);
		this.routes.put(p, result);
		return result;
	}
	
	public abstract void configure();
	private HttpSession parseSession(HttpServerRequest<ByteBuf> request) {
		Map<String, Set<Cookie>> cookies = request.getCookies();
		Set<Cookie> result = cookies.get(cookieNameOfSession);
		Cookie sessionCookie=result==null||result.size()==0?null:result.iterator().next();
		Preconditions.checkNotNull(secretKey,"secretKey should be config");
		Preconditions.checkNotNull(cookieNameOfSession,"cookieNameOfSession should be config");
		return HttpSession.builder(secretKey, cookieNameOfSession).setDomain(cookieDomain).buildFromCookie(sessionCookie);
	}

	public Observable<Void> handle(final HttpServerRequest<ByteBuf> request,final HttpServerResponse<ByteBuf> response) {
		HttpSession session = parseSession(request);
		final Context context=new Context(request,response);
		final RequestContext requestContext=new RequestContext(session,context);
		HttpMethod shouldBeUseHttpMethod=null;
		for (Path path : getRoutes().keySet()) {
			if(!request.getPath().startsWith(path.toPortableString())){
				continue;
			}
			final Route restMethod = getRoutes().get(path);
			if(!Objects.equal(restMethod.getHttpMethod(),request.getHttpMethod())){
				shouldBeUseHttpMethod=restMethod.getHttpMethod();
				continue;
			}
			if(restMethod.getHandler()!=null){
				//TODO RELEASE ByteBuf https://github.com/ReactiveX/RxNetty/issues/203
				return request.getContent().flatMap(new Func1<ByteBuf, Observable<Void>>() {
				    @Override 
				    public Observable<Void> call(ByteBuf buf) {
				    	Object reply=null;
						final FormContext formContext=new FormContext(buf,requestContext);
						try {
							reply=restMethod.getHandler().handle(formContext);
 						} catch (Throwable e) {
 							//非check exception则看看怎么了
 							if(!(e instanceof CheckException)){
 								log.error("handle error ",e);
 							}
 							return HttpResult.error(e).render(formContext);
						}
						if(reply instanceof Observable){
	  						return ((Observable<?>)reply).flatMap(new Func1<Object,Observable<Void>>() {
						        @Override 
						        public Observable<Void> call(Object replyValue) {
						    	    return HttpResult.ok().body(replyValue).render(formContext);
					            }
					        }).onErrorResumeNext(new Func1<Throwable, Observable<Void>>() {
	                            @Override
	                            public Observable<Void> call(Throwable e) {
	 								return HttpResult.error(e).render(formContext);
	                            }
	                        });
						}else{
				    	    return HttpResult.ok().body(reply).render(formContext);
						}
						
				    }
				});
			}
		}
		if(shouldBeUseHttpMethod!=null){
			final String error=String.format("[%s] not find",request.getPath());
			final String detail=String.format("[%s] should use httpMethod[%s]",request.getPath(),shouldBeUseHttpMethod);
			return HttpResult.error(Check.NotFound.toCheckError(new NotFound(){{info=error;}}).withDetail(detail)).render(requestContext);
		}else{
			final String error=String.format("[%s] not find",request.getPath());
			return HttpResult.error(Check.NotFound.toCheckError(new NotFound(){{info=error;}}).withDetail(error)).render(requestContext);
		}
	}
	private Map<Path, Route<?>> getRoutes() {
		return routes;
	}
	

	public static class Route<CONTEXT> {
		private Path path;
		private IRequestHandler<CONTEXT>  handler;
		private HttpMethod httpMethod;
		private Route(Path path,IRequestHandler<CONTEXT>  handler,HttpMethod httpMethod) {
			this.path=path;
			this.handler=handler;
			this.httpMethod=httpMethod;
		}
		public Route<CONTEXT>  handler(IRequestHandler<CONTEXT>  handler) {
			Preconditions.checkNotNull(handler);
			this.handler = handler;
			return this;
		}
		public IRequestHandler<CONTEXT> getHandler() {
			return handler;
		}
		public HttpMethod getHttpMethod() {
			return httpMethod;
		}
		public Path getPath() {
			return path;
		}
	}
}