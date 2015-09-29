package firebats.http.server.exts.result;

import rx.Observable;


public interface IHttpResult {
	public Observable<Void> render();
	public Object getDebugableResult(); 
}
