package firebats.bus.benchmarks;

import java.util.Date;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import firebats.internal.bus.queue.DelayedWarper;
import firebats.internal.bus.queue.RequestQueue;
import firebats.test.BenchMonitor;
import firebats.test.BenchMonitor.Task;

public class RequestReplyDelayQueueBench {
    public static void main(String[] args) {
    	{
    		final RequestQueue<String,String> queue = RequestQueue.createGuavaCache(5000,TimeUnit.MILLISECONDS,10000);
    		BenchMonitor.run(1000*1000, "GuavaCacheDelayQueue put", new Task<Long>() {
    			@Override
    			public void accept(Long t) throws Throwable {
    				queue.put("key"+t,"data"+t);
    			}
    		});
    	}
    	{
    		final RequestQueue<String,String> queue = RequestQueue.createGuavaCache(5000,TimeUnit.MILLISECONDS,10000);
    		BenchMonitor.run(1000*1000, "GuavaCacheDelayQueue put poll", new Task<Long>() {
    			@Override
    			public void accept(Long t) throws Throwable {
    				queue.put("key"+t,"data"+t);
    				queue.poll("key"+t);
    			}
    		});

    	}

    	{
    		final RequestQueue<String, String> queue=RequestQueue.createAndStartDelayQueue(5,TimeUnit.SECONDS);
    		BenchMonitor.run(1000*1000, "RequestReplyDelayQueue put", new Task<Long>() {
    			@Override
    			public void accept(Long t) throws Throwable {
    				queue.put("key"+t,"data"+t);
    			}
    		});

    	}
    	{
    		final RequestQueue<String, String> queue=RequestQueue.createAndStartDelayQueue(5,TimeUnit.SECONDS);
    		BenchMonitor.run(1000*1000, "RequestReplyDelayQueue put poll", new Task<Long>() {
    			@Override
    			public void accept(Long t) throws Throwable {
    				queue.put("key"+t,"data"+t);
    				queue.poll("key"+t);
    			}
    		});

    	}
    	

    	
    	
		BenchMonitor.run(1000*100, "", new Task<Long>() {
			@Override
			public void accept(Long t) throws Throwable {
//				x.put("key"+t, "data"+t,1,TimeUnit.SECONDS);
				new DelayedWarper<String>("", 1, TimeUnit.SECONDS);
			}
		});
		BenchMonitor.run(1000*1000, "System.currentTimeMillis", new Task<Long>() {
			@Override
			public void accept(Long t) throws Throwable {
				 System.currentTimeMillis();
			}
		});
		BenchMonitor.run(1000*1000, "System.nanoTime", new Task<Long>() {
			@Override
			public void accept(Long t) throws Throwable {
				 System.nanoTime();
			}
		});
		BenchMonitor.run(1000*1000, "new Date", new Task<Long>() {
			@Override
			public void accept(Long t) throws Throwable {
				 new Date();
			}
		});
		BenchMonitor.run(1000*1000, "TimeUnit.MILLISECONDS.convert", new Task<Long>() {
			@Override
			public void accept(Long t) throws Throwable {
				 TimeUnit.MILLISECONDS.convert(1000, TimeUnit.NANOSECONDS);
			}
		});
		
		{
		 	final ConcurrentHashMap<String, String> replyWaiters = new ConcurrentHashMap<>();
			BenchMonitor.run(1000*1000, "ConcurrentHashMap put", new Task<Long>() {
				@Override
				public void accept(Long t) throws Throwable {
					replyWaiters.put("x"+t,"v"+t);
				}
			});

		}
		{
		 	final ConcurrentHashMap<String, String> replyWaiters = new ConcurrentHashMap<>();
	 		BenchMonitor.run(1000*1000, "ConcurrentHashMap put remove", new Task<Long>() {
				@Override
				public void accept(Long t) throws Throwable {
					replyWaiters.put("x"+t,"v"+t);
					replyWaiters.remove("x"+t);
				}
			});
		}

		{
			final PriorityQueue<String> queue = new PriorityQueue<String>();
			BenchMonitor.run(1000*1000, "PriorityQueue add", new Task<Long>() {
				@Override
				public void accept(Long t) throws Throwable {
					queue.add("key "+t);
				}
			});
		}

		{
			final PriorityQueue<String> queue = new PriorityQueue<String>();
			BenchMonitor.run(1000*1000, "PriorityQueue add take", new Task<Long>() {
				@Override
				public void accept(Long t) throws Throwable {
					queue.add("key "+t);
					queue.poll();
				}
			});

		}
 

	}
}
