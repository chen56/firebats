package debugCookies;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.HttpObjectAggregationConfigurator;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerPipelineConfigurator;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.server.ErrorHandler;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import firebats.http.server.IRequestHandler;
import firebats.http.server.old.AbstractRoute;
import firebats.http.server.old.FormContext;

public class Server {
	static Logger log=LoggerFactory.getLogger(Server.class);
	public static void main(String[] args) {
		AbstractRoute.cookieNameOfSession="";
		AbstractRoute.secretKey="";
		final XXX xxx=new XXX();
		xxx.configure();
		 HttpServer<ByteBuf, ByteBuf> httpServer = RxNetty
				.newHttpServerBuilder(20000,
						new RequestHandler<ByteBuf, ByteBuf>() {
							@Override public Observable<Void> handle(
									HttpServerRequest<ByteBuf> request,
									final HttpServerResponse<ByteBuf> response) {
								return xxx.handle(request, response);
							}
						}).pipelineConfigurator(Server.<ByteBuf, ByteBuf>httpServerConfigurator())
				.build().withErrorHandler(new ErrorHandler() {
					@Override public Observable<Void> handleError(
							Throwable throwable) {
						log.error("server error", throwable);
						return Observable.empty();
					}
				}); 
		 httpServer.startAndWait();

	}
	public static class XXX extends AbstractRoute<FormContext>{
	    
	    public XXX(){
	    }
	    @Override
	    public void configure(){
			post("/app/x",new IRequestHandler<FormContext>() {
				@Override public Observable<String> handle(FormContext requestContext) {
//					Check.ServerMaintenance.fail();
					System.out.println("["+requestContext.getForm().getAttr("a")+"]");
					return Observable.just("pong : "+new Date() +requestContext.getCookies());
				}
			});
	    }
	}
	public static <I, O> PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> httpServerConfigurator1() {
		return PipelineConfigurators.httpServerConfigurator();
	}
    public static <I, O> PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> httpServerConfigurator2() {
        return new PipelineConfiguratorComposite<HttpServerRequest<I>,HttpServerResponse<O>>(
        		new HttpServerPipelineConfigurator<I, O>(),new HttpCompressorConfigurator(),
        		new HttpObjectAggregationConfigurator(2000_000_000));
    }
    public static <I, O> PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> httpServerConfigurator() {
        return new PipelineConfiguratorComposite<HttpServerRequest<I>,HttpServerResponse<O>>(
        		new HttpServerPipelineConfigurator<I, O>(),new HttpCompressorConfigurator());
    }


}
