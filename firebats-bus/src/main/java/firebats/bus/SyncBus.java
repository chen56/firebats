package firebats.bus;

import java.util.concurrent.TimeUnit;

import firebats.bus.Message.IMessageSpecProvider;
import firebats.bus.Message.MessageSpec;

public class SyncBus{
	private RxBus bus;
	public SyncBus(RxBus bus) {
		this.bus=bus;
	}
	public <TBody extends IMessageSpecProvider<TBody,TReply>,TReply> TReply request(TBody body) {
	    return request(body.getMessageSpec(), body);
    }
	public <TBody extends IMessageSpecProvider<TBody,TReply>,TReply> TReply request(TBody body,long timeout, TimeUnit timeUnit) {
	    return request(body.getMessageSpec(), body,timeout,timeUnit);
    }
	public <TBody,TReply> TReply request(final MessageSpec<TBody,TReply> spec, TBody body) {
	    return request(spec,body,bus.getTimeout(),bus.getTimeoutTimeUnit());
	}
	public <TBody,TReply> TReply request(final MessageSpec<TBody,TReply> spec, TBody body,long timeout, TimeUnit timeUnit) {
	    return bus.request(spec, body).timeout(timeout,timeUnit).toBlocking().first();
	}
}