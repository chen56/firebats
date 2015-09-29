package firebats.bus.queue;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import firebats.internal.bus.queue.RequestQueue;
import rx.functions.Action1;

public class TestGuava {
	String log = "";

	@Before
	public void before(){
		log="";
	}
	
    @Test
    public void 驱逐() throws InterruptedException{
    	RequestQueue<String,String> x= RequestQueue.createGuavaCache(10000, TimeUnit.SECONDS, 1);
    	x.evictions()
    	.subscribe(new Action1<String>() {
			@Override
			public void call(String t1) {
				log(t1);
 			}
		},new Action1<Throwable>() {
			@Override
			public void call(Throwable t1) {
				log(t1.getMessage());
			}
		});
    	x.put("a","aa");
    	x.put("b","bb");
    	x.put("c","cc");
    	x.put("d","dd");
      	assertEquals("aa;bb;cc;",log);
    }
    

	private synchronized void log(Object x) {
		log+=x + ";";
	}

}
