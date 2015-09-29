package firebats.bus.test_by_hand.cluster;

import rx.Observable;
import rx.functions.Func1;
import firebats.bus.Message;
import firebats.bus.Message.MessageSpec;
import firebats.facade.Firebat;
import firebats.net.ConnectionString;

public class Service {
	private Firebat service;

	public void before() throws Exception {
		service = createService();
 	}

	public void after() throws Exception {
		service.stop();
	}

	public static void main(String[] args) throws Exception {
		new Service().run();
	}
	final MessageSpec<String, String> Hello = Message.newRequestSpec("hello", String.class, String.class);
	
	public void run() throws Exception {
		before();
		Thread.sleep(5000000);
		after();
	}

	private Firebat createService() {
		service=connectCluster();
		service.getBus().receive(Hello, new Func1<String, Observable<String>>() {
			@Override
			public Observable<String> call(String message) {
				return Observable.just("hello:" + message);
			}
		});
		return service;
	}
	private Firebat connectCluster() {
		return Firebat.builder(ConnectionString.parse("zk://localhost:2181/testCluster")).buildAndStart();
	}

}
