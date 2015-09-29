package firebats.bus.test_by_hand.cluster;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action1;
import firebats.bus.Message;
import firebats.bus.Message.MessageSpec;
import firebats.facade.Firebat;
import firebats.net.ConnectionString;

public class Client {
	private Firebat client;

	public void before() throws Exception {
		client = connectCluster();
 	}

	public void after() throws Exception {
		client.stop();
	}

	public static void main(String[] args) throws Exception {
		new Client().run();
	}
	final MessageSpec<String, String> Hello = Message.newRequestSpec("hello", String.class, String.class);
	
	public void run() throws Exception {
		before();
 		Observable.interval(3, TimeUnit.SECONDS).subscribe(new Action1<Long>() {
			@Override
			public void call(Long t1) {
				String msg="chen "+t1;
				try {
					client.getBus().request(Hello,msg).toBlocking().first();
					System.out.println("send "+msg);
				} catch (Exception e) {
					System.out.println("send "+msg +" error!"+e.getMessage());
				}
			}
		});
	
		Thread.sleep(5000000);
		after();
	}

	private Firebat connectCluster() {
		return Firebat.builder(ConnectionString.parse("zk://localhost:2181/testCluster")).buildAndStart();
	}

}
