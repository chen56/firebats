package firebats.http.server;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.server.ErrorHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observer;
import rx.subjects.ReplaySubject;
import firebats.component.Component;
import firebats.component.ComponentContainer;
import firebats.component.IComponent;
import firebats.component.IComponentProvider;
import firebats.component.funcs.IStart;
import firebats.component.funcs.IStop;

public class FireHttpServer implements IComponentProvider {
	private static Logger log = LoggerFactory.getLogger(FireHttpServer.class);

    private ComponentContainer container=Component.newContainer(this);
	private int port;
	private HttpServer<ByteBuf, ByteBuf> server;
	Chain<Context,Context> head=new Chain<Context,Context>(new Head());

	private FireHttpServer(int port) {
		this.port=port;
		server=RxNetty.newHttpServerBuilder(this.port, new RequestHandler<ByteBuf, ByteBuf>() {
			    @Override
			    public Observable<Void> handle(HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
					//chain head 处理是最后的防护，作为和其他Chain同样的扩展实现，公平竞争
		    		Context ctx=new Context(request,response);
					log.debug("handle  "+request.getPath());

 			    	try {
 			    		ReplaySubject<Void> result = ReplaySubject.create();
   			    		head.call(ctx).subscribe(new Observer<Object>() {
							@Override
							public void onCompleted() {
								if(log.isDebugEnabled())  log.debug("onCompleted and close");
								//response.writeString(" onCompleted and close");
 								ctx.safeComplete();
								response.close();
								result.onCompleted();
							}
							@Override
							public void onError(Throwable e) {
								log.error("onError and close catch a unexpected error, your chain must care for all error,because framework dont know how to response it  :",e);
 								ctx.safeComplete();
								response.close();
								result.onError(e);
							}
							@Override
							public void onNext(Object t) {
								if(log.isDebugEnabled()) log.debug("onNext " + t);
							}
 			    		});
 			    		return result;
					} catch (Exception e) {
						log.error("error:",e);
						ctx.safeComplete();
				    	return Observable.error(e);
  					}
			    }
			})
//			.enableWireLogging(LogLevel.ERROR)
			.pipelineConfigurator(PipelineConfigurators.<ByteBuf, ByteBuf>httpServerConfigurator())
			.build()
			.withErrorHandler(new ErrorHandler(){
			@Override public Observable<Void> handleError(Throwable throwable) {
				log.error("server error",throwable);
				return Observable.empty();
			}
			})
			;
		
        container.add(
            	Component.newComponent("")
            	.on(IStart.class,new IStart(){
    				@Override public void start() throws Exception {
    					server.start();
     				}
    			})
            	.on(IStop.class,new IStop(){
    				@Override public void stop() throws Exception {
    					server.shutdown();
     				}
    			})
        );
	}
    public void start(){
    	container.start();
    }
    public void stop(){
    	container.stop();
    }
	public static FireHttpServer create(int port) {
 		return new FireHttpServer(port);
	}
	@Override public IComponent getComponent() {
 		return container;
	}
	private static class Head implements Ext<Context, Context> {
 		@Override
		public Observable<?> call(Chain<Context,Context>chain,Context ctx) {
			return chain.next(ctx);
		}
	}
    
	public Chain<Context, Context> chain() {
		return head;
	}
}
