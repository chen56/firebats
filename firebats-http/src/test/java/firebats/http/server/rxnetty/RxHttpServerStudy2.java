package firebats.http.server.rxnetty;

import firebats.json.Jackson;
import io.netty.buffer.ByteBuf;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.server.ErrorHandler;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;
import rx.subjects.AsyncSubject;

public class RxHttpServerStudy2 {
	public static void main(String[] args) {
		HttpServer<ByteBuf, ByteBuf> server = RxNetty
				.newHttpServerBuilder(8888,
						new RequestHandler<ByteBuf, ByteBuf>() {
							@Override
							public Observable<Void> handle(
									HttpServerRequest<ByteBuf> request,
									final HttpServerResponse<ByteBuf> response) {
								debug("handle ");
								
			 			   		AsyncSubject<Void> subject=AsyncSubject.create();
 								Schedulers.newThread().createWorker().schedule(()->{
									response.writeString("x");
									debug("writeString");
									Schedulers.newThread().createWorker().schedule(()->{
										debug("close complete1");
										response.close().doOnTerminate(()->{
											subject.onCompleted();
											debug("close complete2");
										});
 									});
 								});
 								return subject;
							}
						})
//				.enableWireLogging(LogLevel.ERROR)
				.pipelineConfigurator(
						PipelineConfigurators
								.<ByteBuf, ByteBuf> httpServerConfigurator())
				.build().withErrorHandler(new ErrorHandler() {
					@Override
					public Observable<Void> handleError(Throwable throwable) {
						debug("server error"+throwable);
						return Observable.empty();
					}
				});
		server.startAndWait();

	}

	private static void debug(String string) {
		System.out.println("study " + string+" "+Thread.currentThread().getName());
	}

}
