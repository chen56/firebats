package firebats.bus;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import rx.Observable;
import rx.functions.Func1;
import firebats.bus.Message.MessageSpec;
import firebats.discovery.memory.MemoryDiscovery;

public class BusInterceptTest {

  	@Test
	public void request_login() throws InterruptedException {
 		RxBus service = listen(8000).build();
  		final MessageSpec<String,String> REGISTER= Message.newRequestSpec("registerService",String.class,String.class);

        service.receive(REGISTER,new Func1<String, Observable<String>>(){
			@Override
			public Observable<String> call(String message) {
 				return Observable.just(message); 
			}
		});
		// if it is client, it
// 		RxBus client = listen(8001).intercept(new Func1<Message<Object>>(){
// 			
// 		}).build();
// 		String result = client.request(REGISTER,"a").toBlocking().singleOrDefault(null);
//  		assertEquals(true,result);
// 		
//  		service.stop();
//		client.stop();
	}

 	MemoryDiscovery discovery = new MemoryDiscovery();
	private RxBus.Builder listen(int port) {
		return RxBus.builder().address("localhost", port).discovery(discovery);
	}

}