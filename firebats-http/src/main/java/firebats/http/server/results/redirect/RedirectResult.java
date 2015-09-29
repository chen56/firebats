package firebats.http.server.results.redirect;

import com.google.common.base.Preconditions;

import io.netty.handler.codec.http.HttpResponseStatus;
import firebats.http.server.Context;
import firebats.http.server.exts.result.HttpResult;

public class RedirectResult extends HttpResult<RedirectResult>{
  	private String location;
	private HttpResponseStatus status;

	RedirectResult(Context ctx,HttpResponseStatus status,String location) {
		super(ctx);
		Preconditions.checkNotNull(status);
		Preconditions.checkNotNull(location);
		this.status=status;
		this.location=location;
		ctx.getResponse().setStatus(status);
 		ctx.getResponse().getHeaders().setHeader("Location",location);
  	}
	
	@Override
	public String toString() {
 		return "RedirectResult";
	}
 
	public static RedirectResultAware factory(Context ctx) {
		return new RedirectResultAware(){
			@Override
			public Context getContext() {
				return ctx;
			}};
	}

	@Override
	public Object getDebugableResult() {
 		return "redirect "+ status +" "+location;
	}
}