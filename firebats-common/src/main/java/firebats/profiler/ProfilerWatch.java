package firebats.profiler;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

public class ProfilerWatch implements AutoCloseable{

	private Profiler node;
	private Stopwatch watch=Stopwatch.createStarted();

	/*internal*/ ProfilerWatch(Profiler node) {
		this.node=node;
 	}

	public void stop() {
		watch.stop();
		node._stop(this);
	}

	public long elapsed(TimeUnit desiredUnit){
		return watch.elapsed(desiredUnit);
	}

	@Override
	public void close(){
		stop();
	}

	public static ProfilerWatch empty() {
 		return new ProfilerWatch(null){
 			@Override
 			public void stop() {
 			}
 			@Override
 			public long elapsed(TimeUnit desiredUnit) {
  				return 0;
 			}
 			@Override
 			public void close() {
  			}
 		};
	}
}
