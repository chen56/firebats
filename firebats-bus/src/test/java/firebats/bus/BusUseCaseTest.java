package firebats.bus;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import firebats.bus.Message;
import firebats.bus.RxBus;
import firebats.bus.Message.MessageSpec;
import firebats.discovery.memory.MemoryDiscovery;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

public class BusUseCaseTest {

  	@Test
	public void request_login() throws InterruptedException {
 		RxBus service = listen(8000);
        service.receive(LoginMessage.login,new Func1<LoginMessage, Observable<LoginMessage.Result>>(){
			@Override
			public Observable<LoginMessage.Result> call(LoginMessage message) {
				LoginMessage.Result x=	new LoginMessage.Result();
 				x.result=message.username.equals("a");
				return Observable.just(x); 
			}
		});
        
		// if it is client, it
 		RxBus client = listen(8001);
 		LoginMessage.Result result = client.request(LoginMessage.login,new LoginMessage(){{username="a";}}).toBlocking().singleOrDefault(null);
 		
  		assertEquals(true,result.result);
 		
  		service.stop();
		client.stop();
	}
    public static class LoginMessage{
  		final static MessageSpec<LoginMessage,LoginMessage.Result> login=
  				Message.newRequestSpec("login",LoginMessage.class,LoginMessage.Result.class) ;
 		public String username;
		public static class Result{
			public boolean result;
		}
	}
    
  	@Test
	public void noReplyMessage() throws InterruptedException {
 		RxBus service = listen(8000);
        service.receive(LogMessage.Info,new Func1<LogMessage, Observable<Message.NoReply>>(){
			@Override
			public Observable<Message.NoReply> call(LogMessage message) {
				System.out.println("noReplyMessage: "+message);
				return Observable.just(Message.NoReply); 
			}
		});

		// if it is client, it
 		RxBus client = listen(8001);
		assertEquals((Void)null,client.send(LogMessage.Info,new LogMessage(){{log="noReplyMessage;";}}).toBlocking().singleOrDefault(null));
		service.stop();
		client.stop();
	}
  	
  	@Test
	public void primitivesMessage() throws InterruptedException {
 		RxBus service = listen(8000);
    		final MessageSpec<String, Integer> stringInt= Message.newRequestSpec("stringInt",String.class,Integer.class);
    		
        service.receive(stringInt,new Func1<String, Observable<Integer>>(){
			@Override
			public Observable<Integer> call(String message) {
				return Observable.just(Integer.parseInt(message)+10); 
			}
		});
        
 		RxBus client = listen(8001);
 
// 		assertEquals(Integer.valueOf(1+10),client.request(stringInt,"1").toBlocking().first());
		assertEquals((Void)null,client.send(stringInt,"2").toBlocking().singleOrDefault(null));
		service.stop();
		client.stop();
	}
  	@Test
	public void bigText() throws InterruptedException {
 		RxBus service = listen(8000);
    		final MessageSpec<String, String> stringInt= Message.newRequestSpec("stringToString",String.class,String.class);
    		
        service.receive(stringInt,new Func1<String, Observable<String>>(){
			@Override
			public Observable<String> call(String message) {
				return Observable.just(message+message); 
			}
		});
        
 		RxBus client = listen(8001);
        
 		StringBuffer sb=new StringBuffer();
 		for (int i = 0; i < 10*100; i++) {
			sb.append("1234567890");
		}
 		
// 		assertEquals(Integer.valueOf(1+10),client.request(stringInt,"1").toBlocking().first());
		assertEquals((sb.toString()+sb.toString()).length(),client.request(stringInt,sb.toString()).toBlocking().singleOrDefault(null).length());
		service.stop();
		client.stop();
	}

 	/**
	 * client -> registerService -> logServcie
 	 * @throws InterruptedException 
	 */
	@Test
	public void selfToSelf() throws InterruptedException {
  		final CountDownLatch latch=new CountDownLatch(1);
  		final MessageSpec<String,String> REGISTER= Message.newRequestSpec("registerService",String.class,String.class);
  		
		final RxBus registerServer = listen(8000);
		
		registerServer.receive(REGISTER,new Func1<String, Observable<String>>(){
			@Override
			public Observable<String> call(String message) {
				return Observable.just("register ok "+message); 
			}
		});
		 
		registerServer.request(REGISTER,"chen").subscribe(new Action1<String>() {
			@Override
			public void call(String t1) {
				latch.countDown();
			}
		});
		latch.await();
		registerServer.stop();
	}

 	public static class LogMessage{
  		final static MessageSpec<LogMessage,Message.NoReply> Info= Message.newMessageSpec("assets.log",LogMessage.class);
 		public String log;
	}

 	MemoryDiscovery discovery = new MemoryDiscovery();
	private RxBus listen(int port) {
		return RxBus.builder().address("localhost", port).discovery(discovery)
				.buildAndStart();
	}

}