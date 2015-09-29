package firebats.facade;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rx.Observable;
import rx.functions.Func1;
import firebats.bus.Message;
import firebats.bus.Message.MessageSpec;
import firebats.facade.Firebat;
import firebats.net.ConnectionString;

public class FirebatTest {
	private static final ConnectionString zk = ConnectionString.parse("zk://localhost:2181/test");
	TestingServer zkServer;
 	@Before 
    public void before() throws Exception{
		zkServer = new TestingServer(2181);
		zkServer.start();
	}
	
	@After
    public void after() throws IOException{
		if(zkServer!=null)zkServer.close();
	}

    @Test
    public void test1(){
    	Firebat service=Firebat.builder(zk).host("127.0.0.1").build();
    	service.start();
    	
    	Firebat client=Firebat.builder(zk).host("127.0.0.1").build();
    	client.start();

		final MessageSpec<String, String> Hello= Message.newRequestSpec("hello",String.class,String.class);
        service.getBus().receive(Hello,new Func1<String, Observable<String>>(){
			@Override
			public Observable<String> call(String message) {
				return Observable.just("hello:"+message); 
			}
		});
        
		assertEquals("hello:chen",client.getBus().request(Hello, "chen").toBlocking().first());
    	
		client.stop();
		service.stop();
    }
}