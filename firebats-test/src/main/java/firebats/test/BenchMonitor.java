package firebats.test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;

public class BenchMonitor {
	Optional<CountDownLatch> latch = Optional.absent();
	final AtomicLong successCounter = new AtomicLong();
	final AtomicLong errorCounter = new AtomicLong();
	public boolean print;

	public static Builder builder() {
		return new Builder();
	}
	
    public static interface Task<T> {
        void accept(T t) throws Throwable;
    }

	public static void run(long times,String message,Task<Long> task){
		Stopwatch s = Stopwatch.createStarted();
		for (long i = 0; i < times; i++) {
			try {
				task.accept(i);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
		s.stop();
		System.out.println("run "+times+" times "+message+" : "+s.elapsed(TimeUnit.MILLISECONDS)+" ms");
	}
	
	private static long delta(AtomicLong counter, AtomicLong lastCounter) {
		final long now = counter.get();
		long last = lastCounter.getAndSet(now);
		final long delta = now - last;
 		return delta;
	}

	public long tickSuccess() {
		if(latch.isPresent()){
			latch.get().countDown();
		}
		return successCounter.getAndIncrement();
	}

	public long tickError() {
		if(latch.isPresent()){
			latch.get().countDown();
		}
		return errorCounter.getAndIncrement();
	}

	public void awaitLatch() {
		if(latch.isPresent()){
			try {
				latch.get().await();
 				x.interrupt();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class Builder {
		Optional<CountDownLatch> latch = Optional.absent();
		private boolean print=true;

		public Builder latch(int count) {
			latch = Optional.of(new CountDownLatch(count));
			return this;
		}

		public BenchMonitor get() {
			BenchMonitor result = new BenchMonitor();
			result.latch = latch;
			result.print=print;
			result.start();
			return result;
		}

		public Builder noprint() {
			print=false;
 			return this;
		}
	}
	Thread x;
	public void start() {
		x=new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					AtomicLong successLastCounter = new AtomicLong();
					AtomicLong errorLastCounter = new AtomicLong();
					while (true) {
						final long deltaSuccess = delta(successCounter,
								successLastCounter);
						final long deltaError = delta(errorCounter,
								errorLastCounter);
						String info = String.format(
								"%s success/s, %s error/s , %s total ",
								deltaSuccess, deltaError,
								successCounter.get()
										+ errorCounter.get());
				 		if(print){
							log(info);
				 		}
						Thread.sleep(1000);
					}
				} catch (InterruptedException e) {
					log("stop monitor");
				} catch (Throwable e) {
					e.printStackTrace();
 				}
			}
		});
		x.start();
		;
	}
 	private static void log(String message){
		System.out.println(String.format("[BenchMonitor] %s",message));
 	}
	public static void printBenchmarkResult(int times, Stopwatch watch,
			final int success) {
		long elapsed = watch.elapsed(TimeUnit.MILLISECONDS);
		System.out.println(times
				+ " times,"
				+ elapsed
				+ "ms elapsed "
				+ ((int) (times * 1000 / (double) elapsed)) + " times/s");
		System.out.println(success
				+ " success,"
				+ elapsed
				+ "ms elapsed "
				+ ((int) (success * 1000 / (double) elapsed)) + " times/s");
	}
	
	public static void main(String[] args) throws IOException {
        int times=1000*1000;
		BenchMonitor monitor = BenchMonitor.builder().latch(times).get();
		for (int i = 0; i < times-1000; i++) {
			monitor.tickSuccess();
		}
		for (int i = 0; i < 1000; i++) {
			monitor.tickError();
		}
		monitor.awaitLatch();
	}


}