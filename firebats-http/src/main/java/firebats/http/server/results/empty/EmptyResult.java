package firebats.http.server.results.empty;

import firebats.http.server.Context;
import firebats.http.server.exts.result.HttpResult;

public class EmptyResult extends HttpResult<EmptyResult>{
 	public EmptyResult(Context ctx) {
		super(ctx);
 	}
	@Override
	public String toString() {
 		return "EmptyResult";
	}
	public static EmptyResultAware factory(Context ctx) {
		return new EmptyResultAware(){
			@Override
			public Context getContext() {
				return ctx;
			}};
	}
	@Override
	public Object getDebugableResult() {
 		return "empty result";
	}
}