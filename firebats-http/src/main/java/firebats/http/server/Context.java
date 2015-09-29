package firebats.http.server;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.functions.Action0;

public class Context implements ContextAware{
    private static Logger log=LoggerFactory.getLogger(Context.class);
 	private HttpServerRequest<ByteBuf> request;
	private HttpServerResponse<ByteBuf> response;
	private boolean completed;
	private List<Action0> onCloses=new ArrayList<>();
	private Cookies cookies;
    private Map<String,Object> attrs=new LinkedHashMap<>();

	/*internal TODO to internal*/public Context(HttpServerRequest<ByteBuf> request,HttpServerResponse<ByteBuf> response) {
		this.request = request;
		this.response = response;
		this.cookies=new Cookies(this.getRequest(),this.getResponse());
	}
	protected Context(Context context) {
		this.request = context.request;
		this.response = context.response;
		this.cookies=context.cookies;
		this.attrs=context.attrs;
		this.onCloses=context.onCloses;
		this.completed=context.completed;
	}
	public Cookies getCookies(){
		return cookies;
	}
	public HttpServerRequest<ByteBuf> getRequest(){
		return this.request;
	}
	public HttpServerResponse<ByteBuf> getResponse(){
		return this.response;
	}
	/**在调用链完成后，需要调用complete,以使所有需要在完成一次请求后的回调程序执行*/
	/*TODO internal*/public void safeComplete() {
		if(completed)return;
		completed=true;
	    log.debug("Context complete ,close callback");

		for (Action0 action0 : onCloses) {
 			try {
				action0.call();
			} catch (Throwable e) {
			    log.error("Context complete ,close callback error",e);
			}
		}
	}
	public boolean isCompleted(){
		return completed;
	}
	public void onComplete(Action0 action0) {
		onCloses.add(action0);
	}
	/**
	 * 扩展属性,可以在请求的处理流程中的各处理器间传递和本次请求相关的数据，
	 * 主要供http模块的链式调用机制的各个ext使用
	 */
	public Map<String,Object> getAttrs() {
		return attrs;
	}
	@Override
	public Context getContext() {
 		return this;
	}
}