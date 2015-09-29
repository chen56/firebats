package firebats.bus.test_by_hand.ws.rxnetty;

import java.io.IOException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observer;
import rx.functions.Action0;
import rx.functions.Action1;
import firebats.internal.bus.websocket.rxnetty.RxNettyWsClient;

public class ClientMain {
	static Logger log=LoggerFactory.getLogger(ClientMain.class);
	public static void main(String[] args) throws IOException {
		log.debug(" start ");

		RxNettyWsClient client = RxNettyWsClient.create(URI.create("ws://localhost:25000"));
		client.getInput().subscribe(new Observer<String>() {
			@Override public void onCompleted() {
				info("onCompleted ");
			}

			@Override public void onError(Throwable e) {
				error("onError ",e);
			}

			@Override public void onNext(String t) {
 				info("onNext "+t);
			}});
		for (int i = 0; i < 1000; i++) {
			final String message="hi "+i;
			client.writeAndFlush(message).subscribe(new Action1<Void>() {
				@Override public void call(Void t1) {
					info("send success "+message);
				}},new Action1<Throwable>() {
					@Override public void call(Throwable t1) {
						info("send error "+message);
					}
				},new Action0() {
					@Override public void call() {
						info("send complate "+message);
					}
				});
			sleep(3000);
		}
		
		System.in.read();
	}
 	private static void sleep(int i) {
 		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
 			e.printStackTrace();
		}
	}
 	private static void info(Object o) {
 		System.out.println("ClientMain "+o);
	}

 	private static void error(Object o,Throwable e) {
 		System.out.println("ClientMain "+o);
 		e.printStackTrace();
	}

  
}
