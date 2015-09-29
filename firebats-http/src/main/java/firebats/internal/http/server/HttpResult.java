package firebats.internal.http.server;

import firebats.check.Check;
import firebats.check.Check.GeneralException;
import firebats.check.CheckError;
import firebats.check.CheckException;
import firebats.http.server.old.FormContext;
import firebats.http.server.old.RequestContext;
import firebats.json.Jackson;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Action0;

import com.google.common.base.Throwables;
/**
 * 
 *  @Deprecated 暂时还没确定此类如何处理
 * */
@Deprecated

public class HttpResult{
	private static Logger log=LoggerFactory.getLogger(HttpResult.class);

	/** message == body */
	private Object data=new Object();
	private CheckError error;
	
	private transient HttpResponseStatus status;
	/*use for json mapper*/
	private HttpResult(){}
	public static HttpResult ok(){
		HttpResult result=new HttpResult();
		result.status=HttpResponseStatus.OK;
		return result;
	}
	public static HttpResult error(Throwable e){
		HttpResult result=new HttpResult();
 		if( e instanceof CheckException){
			result.error=((CheckException)e).getError();
		}else{
			result.error=Check.GeneralException.toCheckError(new GeneralException(){{info="Service temporarily unavailable";}}).withDetail(Throwables.getStackTraceAsString(e));
		}
		result.status=(HttpResponseStatus.valueOf(400));
		return result;
	}
	public static HttpResult error(CheckError e){
		HttpResult result=new HttpResult();
		result.error=e;
		if(result.error.getCode()>=400){
			result.status=(HttpResponseStatus.valueOf(result.error.getCode()));
		}else{
			result.status=(HttpResponseStatus.valueOf(400));
		}
		return result;
	}
	public HttpResult body(Object body){
		this.data=body;
		return this;
	}
	public HttpResponseStatus getStatus() {
		return status;
	}
	public Observable<Void> render(final RequestContext requestContext) {
		HttpServerResponse<ByteBuf> response = requestContext.getResponse();
		if(requestContext.getSession().isChanged()){
			response.getHeaders().addHeader(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(requestContext.getSession().toCookie()));
		}
		response.getHeaders().setHeader("Access-Control-Allow-Origin", "*");
		response.getHeaders().setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		response.getHeaders().setHeader("Access-Control-Allow-Credentials", "true");

 		debug(requestContext);
  		if(!response.isCloseIssued()){
			response.writeString(Jackson.normal().encode(this));
			return response.close(true).finallyDo(new Action0() {
				@Override public void call() {
					requestContext.safeComplete();
				}
			});
		}else{
			requestContext.safeComplete();
			return Observable.empty();
		}
	}
	public void debug(RequestContext requestContext) {
		if(!log.isDebugEnabled()){
			return;
		}
		StringBuilder sb = getDebugInfo(requestContext);
        log.debug(sb.toString());
    }
	public StringBuilder getDebugInfo(RequestContext requestContext) {
		HttpServerRequest<ByteBuf> request=requestContext.getRequest();
		HttpServerResponse<ByteBuf> response=requestContext.getResponse();

		StringBuilder sb=new StringBuilder();
		sb.append("\r\n");
    	sb.append("-> "+request.getHttpMethod() + " " + request.getUri() +"\r\n");
        for (Map.Entry<String, String> header : request.getHeaders().entries()) {
        	sb.append("  "+header.getKey() + ": [" + header.getValue()+"]\r\n");
        }
        if(requestContext instanceof FormContext){
        	FormContext f=(FormContext)requestContext;
    		sb.append("------form\r\n");
        	for (Entry<String, Object> item : f.getForm().getAll().entrySet()) {
            	sb.append("  "+item.getKey() + ": [" + item.getValue()+"]\r\n");
			}
        }
        sb.append("<- status="+response.getStatus().code() +" isCloseIssued="+response.isCloseIssued()+"\r\n");
        for (Map.Entry<String, String> header : response.getHeaders().entries()) {
        	sb.append("  "+header.getKey() + ": [" + header.getValue()+"]\r\n");
        }
  		if(!response.isCloseIssued()){
         	sb.append(Jackson.pretty().encode(this)+"\r\n");
  		}
		return sb;
	}
	public Object getData() {
		return data;
	}

}