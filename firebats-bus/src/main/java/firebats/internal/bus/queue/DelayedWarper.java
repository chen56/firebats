package firebats.internal.bus.queue;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
/**DelayedQueue 所需结构体*/
public class DelayedWarper<T> implements Delayed {
	private static final AtomicLong sequence = new AtomicLong(0);
	private final long sequenceNumber;
	private final long expiresAt;
	private final T data;

	public DelayedWarper(T data, long timeout, TimeUnit timeUnit) {
		this.expiresAt = System.currentTimeMillis()
				+ timeUnit.convert(timeout, TimeUnit.MILLISECONDS);
		this.data = data;
		this.sequenceNumber = sequence.getAndIncrement();
	}

	public T getData() {
		return this.data;
	}

	public long getDelay(TimeUnit unit) {
		long delay = expiresAt - System.currentTimeMillis();
		return unit.convert(delay, TimeUnit.MILLISECONDS);
	}

	public int compareTo(Delayed other) {
		if (other == this) return 0;

		if (other instanceof DelayedWarper) {
			@SuppressWarnings("unchecked")
			DelayedWarper<T> x = (DelayedWarper<T>) other;
			long diff = expiresAt - x.expiresAt;
			if (diff < 0) return -1; 
			else if (diff > 0) return 1; 
			else if (sequenceNumber < x.sequenceNumber) return -1; 
			else return 1;
		}
		long diff = getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
		return (diff == 0) ? 0 : ((diff < 0) ? -1 : 1);
	}
}

