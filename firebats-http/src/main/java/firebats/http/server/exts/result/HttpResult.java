package firebats.http.server.exts.result;

import io.netty.handler.codec.http.HttpResponseStatus;
import rx.Observable;

import com.google.common.base.Preconditions;

import firebats.http.server.Context;


public abstract class HttpResult<TType extends HttpResult<TType>> implements IHttpResult{
 	protected final Context context;

	public HttpResult(Context ctx) {
		Preconditions.checkNotNull(ctx);
		this.context=ctx;
 	}

	@SuppressWarnings("unchecked")
	protected TType self() {
 		return (TType)this;
	}
	
	/**渲染响应,专用于继承*/ 	
	public Observable<Void> render(){
		return Observable.empty();
	}
	
	public Context getContext() {
		return context;
	}

	//----------------------------------------------------------
	// status
	//----------------------------------------------------------
	public TType status(HttpResponseStatus status){
		this.context.getResponse().setStatus(status);
		return self();
	}
	public TType status(int statusCode){
 		return status(HttpResponseStatus.valueOf(statusCode));
	}
	
	public TType status200Ok(){
 		return status(HttpResponseStatus.OK);
	}
	
	public TType status400BadRequest(){
 		return status(HttpResponseStatus.BAD_REQUEST);
	}
	
    public TType status302Found() {
       return status(HttpResponseStatus.FOUND);
    }

    public TType status301MovedPermanently() {
        return status(HttpResponseStatus.MOVED_PERMANENTLY);
    }

    public TType status303SeeOther() {
        return status(HttpResponseStatus.SEE_OTHER);
    }

    public TType status307TemporaryRedirect() {
        return status(HttpResponseStatus.TEMPORARY_REDIRECT);
    }

}