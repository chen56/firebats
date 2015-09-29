package firebats.bus.examples.remote;

import rx.Observable;
import rx.functions.Func1;
import firebats.bus.Message;
import firebats.bus.Message.MessageSpec;
import firebats.bus.RxBus;
import firebats.discovery.memory.MemoryDiscovery;

public class ServiceMain {
	public static final MessageSpec<String,String> REGISTER= Message.newRequestSpec("register",String.class,String.class);

	public static final int PORT = 25000; 
 	public static final String HOST = "localhost"; 
 	static MemoryDiscovery discovery = new MemoryDiscovery();
	public static void main(String[] args) {
		final RxBus registerServer = RxBus.builder().address(HOST, PORT).discovery(discovery).build();
		registerServer.receive(REGISTER,new Func1<String, Observable<String>>(){
			@Override
			public Observable<String> call(String message) {
				return Observable.just("register ok "+message); 
			}
		});
		registerServer.startAndWait();
	}
}
