package firebats.http.server.results.string;

import rx.Observable;
import firebats.http.server.Context;
import firebats.http.server.exts.result.HttpResult;

public class StringResult extends HttpResult<StringResult>{
	private String body;
	/**internal*/ StringResult(Context ctx,String body) {
		super(ctx);
 		this.body=body;
 	}
	@Override
	public Observable<Void> render() {
 		context.getResponse().writeString(body);
 		return Observable.empty();
	}
	@Override
	public String toString() {
 		return "StringResult:"+body;
	}
	public static StringResultAware factory(Context ctx) {
		return new StringResultAware(){
			@Override
			public Context getContext() {
				return ctx;
			}};
	}
	@Override
	public Object getDebugableResult() {
 		return body;
	}
}