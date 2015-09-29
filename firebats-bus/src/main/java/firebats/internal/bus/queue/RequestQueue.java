package firebats.internal.bus.queue;

import java.util.concurrent.TimeUnit;

import rx.Observable;

public abstract class RequestQueue<KEY,DATA> {
	public abstract void close();

	public abstract void put(KEY key, DATA data);

	public abstract DATA poll(KEY key);

	public abstract Observable<DATA> evictions();
 	public static <KEY,DATA> RequestQueue<KEY, DATA> createAndStartDelayQueue(long timeout,TimeUnit timeoutTimeUnit){
 		return RequestReplyDelayQueue.createAndStart(timeout, timeoutTimeUnit);
 	}

 	public static <KEY,DATA> RequestQueue<KEY, DATA> createGuavaCache(long timeout,TimeUnit timeoutTimeUnit,long maxSizeOfReplyQueue){
 		return GuavaCacheDelayQueue.create(timeout,timeoutTimeUnit,maxSizeOfReplyQueue);
 	}
}
