package firebats.http.server.rxnetty;

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
import rx.schedulers.Schedulers;

public class RxHttpServerStudy {
	public static void main(String[] args) {
		HttpServer<ByteBuf, ByteBuf> server = RxNetty
				.newHttpServerBuilder(8888,
						new RequestHandler<ByteBuf, ByteBuf>() {
							@Override
							public Observable<Void> handle(
									HttpServerRequest<ByteBuf> request,
									final HttpServerResponse<ByteBuf> response) {
								debug("thread id1:"+ Thread.currentThread().getName());
								return Observable
										.just("a")
										.subscribeOn(Schedulers.io())
										.observeOn(Schedulers.io())
										.flatMap(
												x -> {
													debug("thread id2:"+ Thread.currentThread().getName());
													return response.writeStringAndFlush("xxx")
															.subscribeOn(Schedulers.io())
															.observeOn(Schedulers.io())
;
												});
							}
						})
				.enableWireLogging(LogLevel.TRACE)
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
		System.out.println("study " + string);
	}

}
