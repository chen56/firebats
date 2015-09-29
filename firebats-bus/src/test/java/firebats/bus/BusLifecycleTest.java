package firebats.bus;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import firebats.discovery.memory.MemoryDiscovery;
import firebats.net.Uri;

public class BusLifecycleTest {
	MemoryDiscovery discovery=new MemoryDiscovery();
 	@Before
    public void before(){
	}
	
	@After
    public void after(){
	}
	@Test
    public void bind多次无错误() throws InterruptedException{
		for (int i = 0; i < 2; i++) {
			RxBus bus=bind(3333);
			bus.stop();
			System.out.println(i);
		}
	}
	
	@Test
    public void busSend() throws InterruptedException, IOException{
		RxBus service=bind(3333);
		service.rawSend("a","abody");
		service.stop();
	}

	@Test
    public void bus启动和关闭都是同步的() throws InterruptedException{
	    //Bus和应用基本是1:1关系，目前没必要搞复杂，同步初始化即可
		RxBus bus=bind(3333);
		bus.stop();
	}
	
	@Test
    public void bus启动时同步初始化所有connection() throws InterruptedException{
	    //Bus和应用基本是1:1关系，目前没必要搞复杂，同步初始化即可
		MemoryDiscovery router=new MemoryDiscovery();
		router.register(router.newService("service1",Uri.parse("ws://localhost:8001/bus")));
		RxBus bus= RxBus
				.builder()
				.address("localhost",8002)
				.discovery(router)
				.buildAndStart();
 		bus.stop();
	}

	private RxBus bind(int port) {
		return RxBus
				.builder()
				.discovery(discovery)
				.address("localhost",port).buildAndStart();
	}
}