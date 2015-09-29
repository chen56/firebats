package firebats.bus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.fest.assertions.api.Assertions;
import org.junit.Test;

import rx.Observable;
import rx.functions.Func1;
import firebats.bus.Message.MessageSpec;
import firebats.check.Check;
import firebats.check.CheckErrorCauseException;
import firebats.check.CheckException;
import firebats.discovery.memory.MemoryDiscovery;

public class BusErrorTest {
 
  	@Test
  	public void 远程服务抛出异常_client应得到翻译后的异常() throws InterruptedException{
 		RxBus service = listen(8000);
   		final MessageSpec<String, Integer> stringInt= Message.newRequestSpec("stringInt",String.class,Integer.class);

        service.receive(stringInt,new Func1<String, Observable<Integer>>(){
			@Override
			public Observable<Integer> call(String message) {
				throw new RuntimeException("throw "+message);
			}
		});
        
 		RxBus client = listen(8001);
   		try {
   			//when
  			client.request(stringInt,"b").toBlocking().first();
  			fail();
		} catch (CheckException e) {
			System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXX");
			e.printStackTrace();
			System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXX");

			//then
 			assertEquals("Service temporarily unavailable", e.getMessage());
			assertEquals(Check.GeneralException.getErrorCode(), e.getCode());
			Assertions.assertThat(e.getCause()).isInstanceOf(CheckErrorCauseException.class);
			Assertions.assertThat(e.getCause()).hasMessageContaining("java.lang.RuntimeException: throw b");
		}
 
		service.stop();
		client.stop();
  	}

  	@Test
  	public void 多次失败不影响client运转() throws InterruptedException{
 		RxBus service = listen(8000);
   		final MessageSpec<String, Integer> stringInt= Message.newRequestSpec("stringInt",String.class,Integer.class);

        service.receive(stringInt,new Func1<String, Observable<Integer>>(){
			@Override
			public Observable<Integer> call(String message) {
				return Observable.just(Integer.parseInt(message));
			}
		});
        
        //第1次失败
 		RxBus client = listen(8001);
  		client.request(stringInt,"1").toBlocking().singleOrDefault(null);
  		try {
  			client.request(stringInt,"b").toBlocking().singleOrDefault(null);
			fail();
		} catch (CheckException e) {
  			assertEquals("Service temporarily unavailable", e.getMessage());
 		}
  		client.request(stringInt,"1").toBlocking().singleOrDefault(null);

        //第2次失败，多次失败保证不会影响bus状态
  		client.request(stringInt,"1").toBlocking().singleOrDefault(null);
  		try {
  			client.request(stringInt,"b").toBlocking().singleOrDefault(null);
  			fail();
		} catch (CheckException e) {
  			assertEquals("Service temporarily unavailable", e.getMessage());
 		}
  		client.request(stringInt,"2").toBlocking().singleOrDefault(null);

		service.stop();
		client.stop();
  	}
 
	@Test
	public void bindError(){
		try {
			 RxBus.builder().address("error ip", 222).discovery(discovery)
				.buildAndStart();
		} catch (Exception e) {
			Assertions.assertThat(e).hasMessage("error parse uri:bus://error ip:222/bus");
		}
	} 

	@Test
	public void channelNotFind(){
  		RxBus service = listen(4444);
   		final MessageSpec<String, Integer> stringInt= Message.newRequestSpec("stringInt",String.class,Integer.class);
		try {
			service.send(stringInt,"").toBlocking().first();
			fail();
		} catch (CheckException e) {
			Assertions.assertThat(e).hasMessage("Service Not Found");
			Assertions.assertThat(e.getError().getDetail()).contains("send failed,channel[stringInt] no subscriber");
		}
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