package firebats.internal.bus.queue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.subjects.PublishSubject;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class GuavaCacheDelayQueue<KEY,DATA> extends RequestQueue<KEY, DATA> {
 	private PublishSubject<DATA>  evictions=PublishSubject.create();
 	private Cache<KEY,DATA> queue ;
 
 	/* (non-Javadoc)
	 * @see firebats-bus.internal.queue.IRequestReplyQueue#close()
	 */
	@Override
	public void close(){
 	}
	
	/* (non-Javadoc)
	 * @see firebats-bus.internal.queue.IRequestReplyQueue#put(KEY, DATA, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public void put(KEY key, DATA data) {
		queue.put(key, data);
 	}
	
	/* (non-Javadoc)
	 * @see firebats-bus.internal.queue.IRequestReplyQueue#poll(KEY)
	 */
	@Override
	public DATA poll(KEY key){
		DATA result=queue.getIfPresent(key);
		if(result!=null){
			queue.invalidate(key);
		}
 		return result;
	}
	
	/* (non-Javadoc)
	 * @see firebats-bus.internal.queue.IRequestReplyQueue#evictions()
	 */
	@Override
	public Observable<DATA> evictions(){
		return evictions
				.asObservable()
//				.observeOn(Schedulers.io())
//				.subscribeOn(Schedulers.trampoline())
				;
	}

	public static <KEY, DATA> RequestQueue<KEY, DATA> create(long timeout,
			TimeUnit timeoutTimeUnit, long maxSizeOfReplyQueue) {
 		final GuavaCacheDelayQueue<KEY,DATA> result=new GuavaCacheDelayQueue<KEY,DATA>();
   		result.queue = CacheBuilder.newBuilder()
	 			.expireAfterWrite(timeout, timeoutTimeUnit)
	  			.maximumSize(maxSizeOfReplyQueue)
	  			.removalListener(new RemovalListener<KEY,DATA>() {
	  				Set<RemovalCause> wasEvicted=new HashSet<>();
	  				{
	  					//只有这3种情况算是被清除
	  					wasEvicted.add(RemovalCause.COLLECTED);
	  					wasEvicted.add(RemovalCause.EXPIRED);
	  					wasEvicted.add(RemovalCause.SIZE);
	  				}
					@Override
					public void onRemoval(RemovalNotification<KEY,DATA> notification) {
 						RemovalCause cause = notification.getCause();
						if(wasEvicted.contains(cause)){
							//异常构造耗资巨大，此处频率很高，用shared异常优化之
							result.evictions.onNext(notification.getValue());
 						}
					}
				})
				.build();
		return result;
	}
}