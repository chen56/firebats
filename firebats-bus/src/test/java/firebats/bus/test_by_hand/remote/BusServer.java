package firebats.bus.test_by_hand.remote;

import java.io.IOException;
import java.util.Date;

import rx.Observable;
import rx.functions.Func1;

import com.google.common.base.Objects;

import firebats.bus.Message;
import firebats.bus.Message.MessageSpec;
import firebats.bus.RxBus;
import firebats.discovery.memory.MemoryDiscovery;

public class BusServer {
	public static void main(String[] args) throws IOException {
		// Bus和应用基本是1:1关系，目前没必要搞复杂，同步初始化即可
		RxBus service = listen(25000);
		final MessageSpec<String, String> stringInt = Message.newRequestSpec("hello",String.class,String.class);

		service.receive(stringInt,new Func1<String, Observable<String>>() {
					@Override
					public Observable<String> call(String message) {
						if(Objects.equal(message, "exception")){
							throw new RuntimeException("exception "+message);
						}
						System.out.println("xx:"+message);
						return Observable.just(message+":"+new Date());
					}
				});
		System.in.read();
	}

	static MemoryDiscovery discovery = new MemoryDiscovery();

	private static RxBus listen(int port) {
		return RxBus.builder().address("localhost", port).discovery(discovery)
				.buildAndStart();
	}
}
