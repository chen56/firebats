package firebats.internal.bus.websocket.rxnetty;

import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.server.RxServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import firebats.bus.RxBus.IRawRequest;
import firebats.component.Component;
import firebats.component.ComponentContainer;
import firebats.component.IComponent;
import firebats.component.IComponentProvider;
import firebats.component.funcs.IStart;
import firebats.component.funcs.IStop;
import firebats.internal.bus.IBusServer;

public class RxNettyWsServer implements IBusServer, IComponentProvider{
	private RxServer<WebSocketFrame, WebSocketFrame> server;
	private static Logger log=LoggerFactory.getLogger(RxNettyWsServer.class);
	private PublishSubject<IRawRequest> receiveSubject = PublishSubject.create();
    private ComponentContainer container=Component.newContainer(this);

	private RxNettyWsServer(final int port) {
		this.server= RxNetty
				.newWebSocketServerBuilder(port,new ConnectionHandler<WebSocketFrame, WebSocketFrame>() {
							@Override public Observable<Void> handle(final ObservableConnection<WebSocketFrame, WebSocketFrame> connection) {
								return connection.getInput().flatMap(new Func1<WebSocketFrame, Observable<Void>>() {
													@Override public Observable<Void> call(
															WebSocketFrame wsFrame) {
														
												        if (wsFrame instanceof CloseWebSocketFrame) {
												            return connection.close();
												        }
												        if (wsFrame instanceof PingWebSocketFrame) {
												        	return connection.writeAndFlush(new PongWebSocketFrame());
												        }
												        if (wsFrame instanceof TextWebSocketFrame) {
												        	String data=((TextWebSocketFrame)wsFrame).text();
												        	log.debug("handleWebSocketFrame "+data);
												        	receiveSubject.onNext(new ReplyableWebSocketFrame(data, connection));
															return Observable.empty();
												        }
														return Observable.empty();
													}
												});
							}
						}).enableWireLogging(LogLevel.DEBUG).build();
        container.add(
            	Component.newComponent(server)
            	.on(IStart.class,new IStart(){
    				@Override public void start() throws Exception {
    					log.info("start RxNettyWsServer port="+port);
    					server.start();
    				}
    			})
            	.on(IStop.class,new IStop(){
    				@Override public void stop() throws Exception {
    					log.info("stopping RxNettyWsServer port="+port);
    					server.shutdown();
    					log.info("stoped RxNettyWsServer port="+port);

    				}
    			})
            );
	}
	
    public static RxNettyWsServer create(int port){
     	return new RxNettyWsServer(port);
    }

	@Override public IComponent getComponent() {
 		return container;
	}

    public void start(){
    	container.start();
    }
    
    public void stop(){
    	container.stop();
    }
	
	public Observable<IRawRequest> getInput(){
		return receiveSubject.asObservable();
	}
	public void startAndWait(){
		log.debug("startAndWait");
		server.startAndWait();
	}

	public static class ReplyableWebSocketFrame implements IRawRequest{
		private final String message;
		private final ObservableConnection<WebSocketFrame, WebSocketFrame> connection;
		public ReplyableWebSocketFrame(String message,ObservableConnection<WebSocketFrame, WebSocketFrame> connection){
			this.message=message;
			this.connection=connection;
		}
		public Observable<Void> reply(WebSocketFrame frame) {
			log.debug("reply :"+frame); 
			return connection.writeAndFlush(frame);
		}
		@Override public Observable<Void> reply(String message) {
			return connection.writeAndFlush(new TextWebSocketFrame(message));
		}
		@Override public String getMessage() {
			return message;
		}
	}
}