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

import java.io.IOException;
import java.nio.charset.Charset;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.AsyncSubject;

public class RxnettyBug {
	private int port;
	private HttpServer<ByteBuf, ByteBuf> server;

	private RxnettyBug(int port) {
		this.port = port;
		server = RxNetty
				.newHttpServerBuilder(this.port,
						new RequestHandler<ByteBuf, ByteBuf>() {
							@Override
							public Observable<Void> handle(
									HttpServerRequest<ByteBuf> request,
									final HttpServerResponse<ByteBuf> response) {
								AsyncSubject<Void> result = AsyncSubject.create();
								debug("handle " + request.getPath());

								request.getContent()
										.flatMap(
												new Func1<ByteBuf, Observable<?>>() {
													@Override
													public Observable<?> call(ByteBuf content) {
														debug("read content");
														String body = content.toString(Charset.defaultCharset());
														return Observable.just("pong : "+ body)
																 //use io thread,response nothing! 
																 .subscribeOn(Schedulers.io())
																 .map(x -> {
																	debug("pong "+x);
																	response.writeString(x);
																	return x;
																});
													}
												})
										.subscribe(new Observer<Object>() {
											@Override
											public void onCompleted() {
												debug("onCompleted and close");
												response.close();
											}

											@Override
											public void onError(Throwable e) {
												debug("onError and close :",e);
												response.close();
											}

											@Override
											public void onNext(Object t) {
												debug("onNext " + t);

											}
										});
								return result;							
							}
						})
				.enableWireLogging(LogLevel.DEBUG)
				.pipelineConfigurator(PipelineConfigurators.<ByteBuf, ByteBuf> httpServerConfigurator())
				.build().withErrorHandler(new ErrorHandler() {
					@Override
					public Observable<Void> handleError(Throwable throwable) {
						debug("server error", throwable);
						return Observable.empty();
					}
				});
	}

	public void start() {
		server.start();
	}
	
	private void debug(String string, Throwable throwable) {
		debug(string+throwable);
	}
	
	private void debug(String string) {
 		System.out.println(Thread.currentThread().getName()+"    "+string);
 	}

	public void stop() throws InterruptedException {
		server.shutdown();
	}

	public static RxnettyBug create(int port) {
		return new RxnettyBug(port);
	}

	public static void main(String[] args) throws IOException {
		RxnettyBug.create(8888).start();
		System.in.read();
	}
}