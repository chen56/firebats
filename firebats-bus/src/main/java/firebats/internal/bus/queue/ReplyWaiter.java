package firebats.internal.bus.queue;

import rx.Observer;

public class ReplyWaiter{
	public final Observer<String> subject;
	public final String message;
	public ReplyWaiter(Observer<String> subject, String message){
		this.subject=subject;
		this.message=message;
	}
}
