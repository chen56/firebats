package firebats.bus;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rx.Observable;
import rx.functions.Func1;
import firebats.bus.Message.MessageSpec;
import firebats.facade.Firebat;
import firebats.net.ConnectionString;

public class ClusterTest {
	Firebat c;
	TestingServer zkServer;
	private Firebat s;
 	@Before 
    public void before() throws Exception{
		zkServer = new TestingServer(9999);
		zkServer.start();
 		c= connectCluster();
 		s= connectCluster();
	}

	private Firebat connectCluster() {
		return Firebat.builder(ConnectionString.parse("zk://localhost:9999/testCluster"))
				.host("127.0.0.1").buildAndStart();
	}
	
	@After
    public void after() throws IOException{
		s.stop();
		c.stop();
		zkServer.close();
	}

	@Test
	public void test1() throws InterruptedException{
		final MessageSpec<String, String> Hello= Message.newRequestSpec("hello",String.class,String.class);
		s.getBus().receive(Hello,new Func1<String, Observable<String>>(){
			@Override
			public Observable<String> call(String message) {
				return Observable.just("hello:"+message); 
			}
		});
		RxBus client=c.getBus();
		assertEquals("hello:chen", client.request(Hello, "chen").toBlocking().first());
	}
}
