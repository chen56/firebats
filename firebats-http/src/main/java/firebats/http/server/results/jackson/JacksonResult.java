package firebats.http.server.results.jackson;

import rx.Observable;
import firebats.http.server.Context;
import firebats.http.server.exts.result.HttpResult;
import firebats.json.Jackson;

public class JacksonResult extends HttpResult<JacksonResult>{
	private Object body;

	/**internal*/ JacksonResult(Context ctx,Object body) {
		super(ctx);
 		this.body=body;
 	}
	@Override
	public Observable<Void> render() {
		String bodyWrite=Jackson.normal().encode(body);
 		context.getResponse().writeString(bodyWrite);
 		return Observable.empty();
	}
	
	@Override
	public String toString() {
 		return "JacksonResult:"+body;
	}
	public static JacksonResultAware factory(Context ctx) {
		return new JacksonResultAware(){
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
