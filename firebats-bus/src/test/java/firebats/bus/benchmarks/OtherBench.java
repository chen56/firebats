package firebats.bus.benchmarks;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import firebats.test.BenchMonitor;
import firebats.test.BenchMonitor.Task;

public class OtherBench {
	public static void main(String[] args) {
		BenchMonitor.run(1000 * 1000, "System.currentTimeMillis",
				new Task<Long>() {
					@Override
					public void accept(Long t) throws Throwable {
						System.currentTimeMillis();
					}
				});
		BenchMonitor.run(1000 * 1000, "System.nanoTime", new Task<Long>() {
			@Override
			public void accept(Long t) throws Throwable {
				System.nanoTime();
			}
		});
		BenchMonitor.run(1000 * 1000, "new Date", new Task<Long>() {
			@Override
			public void accept(Long t) throws Throwable {
				new Date();
			}
		});
		BenchMonitor.run(1000 * 1000, "TimeUnit.MILLISECONDS.convert",
				new Task<Long>() {
					@Override
					public void accept(Long t) throws Throwable {
						TimeUnit.MILLISECONDS.convert(1000,
								TimeUnit.NANOSECONDS);
					}
				});
		{
			BenchMonitor.run(1000 * 1000, "UUID.randomUUID().toString",
					new Task<Long>() {
						@Override
						public void accept(Long t) throws Throwable {
							UUID.randomUUID().toString();
						}
					});

		}
 	}
}