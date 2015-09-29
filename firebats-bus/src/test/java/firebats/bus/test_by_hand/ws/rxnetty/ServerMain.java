package firebats.bus.test_by_hand.ws.rxnetty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observer;
import firebats.bus.RxBus.IRawRequest;
import firebats.internal.bus.websocket.rxnetty.RxNettyWsServer;

public class ServerMain {
	static Logger log = LoggerFactory.getLogger(ServerMain.class);
	public static void main(String[] args) {
		RxNettyWsServer server = RxNettyWsServer.create(25000);
		server.getInput().subscribe(new Observer<IRawRequest>() {
			@Override public void onCompleted() {
				log.debug("onCompleted");
			}
			@Override public void onError(Throwable e) {
				log.debug("onError",e);
			}
			@Override public void onNext(IRawRequest	 t) {
				log.debug("onNext "+t.getMessage());
 				t.reply("Reply: "+t.getMessage());
			}});
		server.startAndWait();
	}
}