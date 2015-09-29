package firebats.bus.test_by_hand.remote;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import rx.functions.Action1;
import firebats.bus.Message;
import firebats.bus.Message.MessageSpec;
import firebats.bus.RxBus;
import firebats.discovery.memory.MemoryDiscovery;
import firebats.net.Uri;

public class BusClient {
	static MemoryDiscovery discovery=new MemoryDiscovery();  

	public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
		// Bus和应用基本是1:1关系，目前没必要搞复杂，同步初始化即可
		final MessageSpec<String, String> stringInt = Message.newRequestSpec("hello",String.class,String.class);
		discovery.register(discovery.newService(stringInt.getChannel(),Uri.parse("ws://localhost:25000/bus") ));
 		System.out.println("go1");
 		//192.168.1.112
 		//192.168.1.51
 		URI thisBus = new URI("ws://localhost:25001/bus");
		RxBus bus = listen(8081);
		for (int i = 0; i < 1000; i++) {
			bus.request(stringInt, "a").subscribe(new Action1<String>() {
				@Override
				public void call(String t1) {
					System.out.println(t1);
				}
			},new Action1<Throwable>() {
				@Override
				public void call(Throwable t1) {
//					System.out.println(t1);
//				    t1.printStackTrace();
				}
			});
		    Thread.sleep(2000);
		}
//		System.in.read();
	}
	private static RxBus listen(int port) {
		return RxBus.builder().address("localhost", port).discovery(discovery)
				.buildAndStart();
	}

}
