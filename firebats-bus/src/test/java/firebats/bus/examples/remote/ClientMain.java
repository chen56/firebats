package firebats.bus.examples.remote;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import firebats.bus.Message;
import firebats.bus.Message.MessageSpec;
import firebats.bus.RxBus;
import firebats.discovery.memory.MemoryDiscovery;
import firebats.net.Uri;

public class ClientMain {
 	private static final int PORT = 25001; 
 	static MemoryDiscovery discovery = new MemoryDiscovery();
	private static RxBus listen(int port) {
		return RxBus.builder().address("localhost", port).discovery(discovery)
				.buildAndStart();
	}
    
	public static void main(String[] args) throws IOException {
  		final MessageSpec<String,String> REGISTER= Message.newRequestSpec("register",String.class,String.class);
		//标出远端主机提供哪些服务
		discovery.register(discovery.newService(ServiceMain.REGISTER.getChannel(),Uri.parse(String.format("bus://%s:%s/bus",ServiceMain.HOST,ServiceMain.PORT))));

		final RxBus registerServer = listen(PORT);
		
		Observable.timer(3, 3, TimeUnit.SECONDS, Schedulers.io()).subscribe(new Action1<Long>() {
			@Override public void call(Long t1) {
				//request 
				try {
					System.out.println(registerServer.request(ServiceMain.REGISTER, "chen").toBlocking().first());;
				} catch (Exception e) {
				    System.err.println("error:"+e);
				}
			}
		},new Action1<Throwable>() {
 			@Override public void call(Throwable t1) {
               t1.printStackTrace();
			}
		});
		System.in.read();
	}}
