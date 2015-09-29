package firebats.http;

import io.reactivex.netty.server.RxServer;
import firebats.component.Component;
import firebats.component.IComponent;
import firebats.component.funcs.IStart;
import firebats.component.funcs.IStop;

public class RxNettys {

	public static <TIn,TOut> IComponent warpComponent(final RxServer<TIn,TOut> httpServer) {
		return Component.newComponent(httpServer).onStart(new IStart() {
			@Override public void start() throws Exception {
				httpServer.start();
			}
		})
		.onStop(new IStop() {
			@Override public void stop() throws Exception {
				httpServer.shutdown();
			}
		})
		;
	}

}
