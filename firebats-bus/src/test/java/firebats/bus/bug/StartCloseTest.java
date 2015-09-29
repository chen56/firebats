package firebats.bus.bug;

import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.protocol.http.websocket.WebSocketServer;

import org.junit.Test;

import rx.Observable;
import rx.functions.Func1;
import firebats.bus.RxBus;
import firebats.discovery.memory.MemoryDiscovery;
import firebats.internal.bus.websocket.rxnetty.RxNettyWsServer;
//非要sleep一点时间才能成功运行
public class StartCloseTest {
	MemoryDiscovery discovery = new MemoryDiscovery();

	@Test
	public void stub() throws InterruptedException {
	}
	@Test
	public void bind多次无错误_RxNettyWsServer() throws InterruptedException {
		System.out.println("start bind多次无错误");
		for (int i = 0; i < 100; i++) {
			System.out.println(i);
			RxNettyWsServer bindServer=RxNettyWsServer.create(4445);
			bindServer.start();
			bindServer.stop();
			Thread.sleep(10);

		}
		System.out.println("end bind多次无错误");
	}
	@Test
	//很不幸，rxnetty自己也不能做到
	public void bind多次无错误2_rxnetty_WebSocketServer() throws InterruptedException {
		System.out.println("start bind多次无错误");
		for (int i = 0; i < 100; i++) {
			System.out.println(i);
			 WebSocketServer<WebSocketFrame, WebSocketFrame> server = RxNetty
						.newWebSocketServerBuilder(4445,new ConnectionHandler<WebSocketFrame, WebSocketFrame>() {
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
																	return Observable.empty();
														        }
																return Observable.empty();
															}
														});
									}
								}).enableWireLogging(LogLevel.DEBUG).build();

			 server.start();
			 server.shutdown();
			Thread.sleep(10);
		}
		System.out.println("end bind多次无错误");
	}
  	//很不幸，rxnetty自己也不能做到，暂时关闭此测试
	@Test
	public void bind多次无错误_bus() throws InterruptedException {
		System.out.println("start bind多次无错误");
		for (int i = 0; i < 100; i++) {
			System.out.println(i);
			RxBus bus = listen(4445);
			bus.stop();
			Thread.sleep(10);
		}
		System.out.println("end bind多次无错误");
	}

	private RxBus listen(int port) {
		return RxBus.builder().address("localhost", port).discovery(discovery)
				.buildAndStart();
	}

}