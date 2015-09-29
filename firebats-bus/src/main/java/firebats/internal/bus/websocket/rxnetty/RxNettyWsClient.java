package firebats.internal.bus.websocket.rxnetty;

import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.protocol.http.websocket.WebSocketClient;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.subjects.AsyncSubject;
import rx.subjects.PublishSubject;

import com.google.common.base.Objects;

import firebats.bus.exceptions.BusException;
import firebats.internal.bus.IBusClient;

public class RxNettyWsClient implements IBusClient{

    static final int DEFAULT_NO_OF_EVENTS = 1000;
    static final int DEFAULT_INTERVAL = 2000;
    private AtomicReference<ClientState> state=new AtomicReference<ClientState>(ClientState.Disconnected);
	private URI uri;
	private AtomicReference<Observable<ObservableConnection<WebSocketFrame, WebSocketFrame>>> connectioning=new AtomicReference<>();
	private WebSocketClient<WebSocketFrame, WebSocketFrame> rxClient;
	private static Logger log=LoggerFactory.getLogger(RxNettyWsClient.class);
	private PublishSubject<String> receiveSubject = PublishSubject.create();

	public RxNettyWsClient(URI uri) {
        this.uri = uri;
    	this.rxClient=createRxWebSocketClient(uri);
    }
    public static RxNettyWsClient create(URI uri) {
        return new RxNettyWsClient(uri);
    }
	@Override public URI getRemoteURI() {
		return uri;
	}
	
 	@Override public void close() {
 		state.set(ClientState.Disconnecting);
 		try {
 	 		rxClient.shutdown();
		} catch (Exception e) {
			log.error("close error",e);
		}
 		rxClient=null;
 		state.set(ClientState.Disconnected);
 		connectioning.set(null);
	}
 	private void reset(){
 		close();
    	this.rxClient=createRxWebSocketClient(uri);
 	}
 	private static WebSocketClient<WebSocketFrame, WebSocketFrame> createRxWebSocketClient(URI uri){
 		return RxNetty.<WebSocketFrame, WebSocketFrame>newWebSocketClientBuilder(uri.getHost(), uri.getPort())
                        .withWebSocketURI(uri.getPath())
                        .withMaxFramePayloadLength(65536*8)//default
                        .withWebSocketVersion(WebSocketVersion.V13)
                        .build();
 	}
 	
 	private static Func1<ObservableConnection<WebSocketFrame, WebSocketFrame>,Void> Conn2void=new Func1<ObservableConnection<WebSocketFrame, WebSocketFrame>,Void>(){
		@Override
		public Void call(
				ObservableConnection<WebSocketFrame, WebSocketFrame> t1) {
			return null;
		}
    };
    
	public Observable<Void> startAsync()  {
		return ensureConnection().map(Conn2void);
    }
	
	private Observable<ObservableConnection<WebSocketFrame, WebSocketFrame>> ensureConnection()  {
		ClientState s=state.get();
		switch (s) {
		case Disconnected:
			return connect();
			
		case Connecting:
			return connectioning.get();
			
		case Connected:
			return connectioning.get();

		case Disconnecting:
			return connectioning.get();

		case Failed:
			return connect();

		default:
			return Observable.error(new BusException(String.format("bug:state (%s) not process",s)));
		}
	}
	
 	private Observable<ObservableConnection<WebSocketFrame, WebSocketFrame>> connect() {
 		final AsyncSubject<ObservableConnection<WebSocketFrame, WebSocketFrame>> result=AsyncSubject.create();
 		synchronized (state) {
 			state.set(ClientState.Connecting);
 			connectioning.set(result);
		}
		
        rxClient.connect().subscribe(new Observer<ObservableConnection<WebSocketFrame, WebSocketFrame>>() {
			@Override public void onCompleted() {
				result.onCompleted();
			}
			@Override public void onError(Throwable e) {
 				log.debug("connect.onError "+getRemoteURI(),e);
				state.set(ClientState.Failed);
				result.onError(e);
			}

			@Override public void onNext(final ObservableConnection<WebSocketFrame, WebSocketFrame> conn) {
				log.info("connected {}",getRemoteURI());
				
				state.set(ClientState.Connected);
				result.onNext(conn);

				conn.getInput().subscribe(new Observer<WebSocketFrame>() {
					@Override public void onCompleted() {
                		log.debug("input.onCompleted");
					}

					@Override public void onError(Throwable e) {
 						log.info("input.onError[{}]",e.getMessage());
 						state.set(ClientState.Failed);
					}

					@Override public void onNext(WebSocketFrame frame) {
                        if (frame instanceof CloseWebSocketFrame) {
                        	log.debug("received CloseWebSocketFrame");
                            close();
                        }else if (frame instanceof PingWebSocketFrame) {
                        	log.debug("received PingWebSocketFrame");
                        	conn.writeAndFlush(new PongWebSocketFrame());
                        }else if(frame instanceof TextWebSocketFrame){
                        	TextWebSocketFrame f=(TextWebSocketFrame)frame;
                        	String text=f.text();
                            log.debug("received TextWebSocketFrame: "+text);
                        	receiveSubject.onNext(text);
                        }else{
                            log.debug("received unknow frame: "+frame);
                        }
					}});
			}
		});
        
        return result;
	}
	public Observable<Void> writeAndFlush(final String message) {
		log.debug("client({}) writeAndFlush {}",state.get(),message);
		return ensureConnection().flatMap(new Func1<ObservableConnection<WebSocketFrame, WebSocketFrame>, Observable<? extends Void>>() {
			@Override
			public Observable<? extends Void> call(
					ObservableConnection<WebSocketFrame, WebSocketFrame> conn) {
 				if(conn==null){
					log.error("why conn is null?");
					return Observable.error(new BusException(String.format("Send Error state(%s) : %s",state.get(),message)));
				}else{
					return conn.writeAndFlush(new TextWebSocketFrame(message));
				}
			}
		});
	}

    public URI getUri() {
		return uri;
	}
    
    public Observable<String> getInput() {
		return receiveSubject.asObservable();
	}

    @Override
    public final boolean equals(Object other) {
		if(this==other)return true;
		if(!(other instanceof RxNettyWsClient)) return false;
		if(!canEqual(other)) return false;

		RxNettyWsClient that = (RxNettyWsClient) other;
		return Objects.equal(this.uri,that.uri);
    }
    
	public boolean canEqual(Object other) {
		return (other instanceof RxNettyWsClient);
	}

    @Override
    public final int hashCode() {
        return uri.hashCode();
    }
    
	/** Connection state */
	public static enum ClientState {
		/**
		 * 轻量级的初始状态，还未执行任何资源消耗型任务
		 */
		Disconnected,

		/**
		 * 向Connected状态转换时的过度转换{@link #Running}.
		 */
		Connecting,

		/**
		 * 工作状态
		 */
		Connected,

		/**
		 * 向Disconnected状态转换时的过度转换{@link #Connected}.
		 */
		Disconnecting,

		/**
		 * 各种原因造成的故障，故障状态为临时状态，佷快就会进行重连操作,在没有转换为{@link #Running}前，发送消息都将失败
		 */
		Failed;
  	}

}