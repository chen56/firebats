package firebats.internal.bus.queue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.subjects.PublishSubject;

public class RequestReplyDelayQueue<KEY,DATA> extends RequestQueue<KEY, DATA> {
 	private final ConcurrentHashMap<KEY, Tuple2> replyWaiters = new ConcurrentHashMap<>();
 	private final DelayQueue<DelayedWarper<Tuple2>> queue = new DelayQueue<>();
	private Thread thread;
	private PublishSubject<DATA>  evictions=PublishSubject.create();
    private AtomicInteger sequence=new AtomicInteger();
    long timeout;
    TimeUnit timeoutTimeUnit;
    
	public static <KEY, DATA>  RequestQueue<KEY, DATA> createAndStart(long timeout,
			TimeUnit timeoutTimeUnit) {
 		RequestReplyDelayQueue<KEY,DATA> result=new RequestReplyDelayQueue<KEY,DATA>();
 		result.timeout=timeout;
 		result.timeoutTimeUnit=timeoutTimeUnit;
  		result._start();
		return result;
	}
	
    RequestReplyDelayQueue<KEY,DATA> _start() {
 		thread=new Thread(new Runnable() {
			@Override
			public void run() {
		        for (;;) {
		            try {
		            	DelayedWarper<Tuple2> delayItem = queue.take();
		                if (delayItem != null) {
		                	Tuple2 keyAndData = delayItem.getData();
		                	Tuple2 evicted = replyWaiters.remove(keyAndData.t1);
		                	if(evicted!=null){ 
	 		                	evictions.onNext(keyAndData.t2);
		                	}
		                }
		            } catch (InterruptedException e) {
 		                return;
		            }
		        }
			}
		});
 		thread.setDaemon(true);
 		thread.setName("RequestReplyDelayQueue "+sequence.getAndIncrement());
 		thread.start();
 		return this;
	}

	/* (non-Javadoc)
	 * @see firebats-bus.internal.queue.IRequestReplyQueue#close()
	 */
	@Override
	public void close(){
		thread.interrupt();
	}
	
	/* (non-Javadoc)
	 * @see firebats-bus.internal.queue.IRequestReplyQueue#put(KEY, DATA, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public void put(KEY key, DATA data) {
		Tuple2 x=new Tuple2(key,data);
 		replyWaiters.put(key,x);
		queue.offer(new DelayedWarper<Tuple2>(x, timeout, timeoutTimeUnit));

// 		e.execute(new Runnable() {
//			@Override
//			public void run() {
//				queue.offer(new DelayedWarper<Tuple2>(x, timeout, timeoutTimeUnit));
//			}
//		});
	}
	
	/* (non-Javadoc)
	 * @see firebats-bus.internal.queue.IRequestReplyQueue#poll(KEY)
	 */
	@Override
	public DATA poll(KEY key){
		Tuple2 result=replyWaiters.remove(key);
 		return result==null?null:result.t2;
	}
	
	/* (non-Javadoc)
	 * @see firebats-bus.internal.queue.IRequestReplyQueue#evictions()
	 */
	@Override
	public Observable<DATA> evictions(){
		return evictions.asObservable();
	}
	
    private final class Tuple2 {
        private final KEY t1;
        private final DATA t2;
         private Tuple2(KEY key, DATA data) {
            this.t1 = key;
            this.t2 = data;
        }
     }

}