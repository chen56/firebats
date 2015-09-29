package firebats.http.server.exts.debug;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Action0;

import com.google.common.base.Supplier;

import firebats.http.server.Chain;
import firebats.http.server.Context;
import firebats.http.server.Ext;
import firebats.http.server.exts.form.Form;
import firebats.http.server.exts.form.FormExt;
import firebats.json.Jackson;
/**打印request和response情况*/
/**打印request和response情况*/
public class DebugExt implements Ext<Context,Context>{
	protected static final String ATTR = "ext.debug.result";
	private Supplier<Boolean> debugEnabled;
    static Logger log = LoggerFactory.getLogger(DebugExt.class);

	private final static Supplier<Boolean> Enable=new Supplier<Boolean>() {
		@Override
		public Boolean get() {
			return true;
		}
	};
	private DebugExt(Supplier<Boolean> debugEnabled) {
		this.debugEnabled=debugEnabled;
	}
    public static DebugExt newEnableDebug(){
    	return new DebugExt(Enable);
    }
    public static DebugExt newDebug(Supplier<Boolean> debugEnabled){
    	return new DebugExt(Enable);
    }
    public static DebugExt newEnableDebugSameAsLog(org.slf4j.Logger debugEnabled){
    	return new DebugExt(new Supplier<Boolean>() {
    		@Override
    		public Boolean get() {
    			return debugEnabled.isDebugEnabled();
    		}
    	});
    }
    
	@Override
	public Observable<?> call(Chain<Context,Context> chain,Context ctx) {
		return chain.next(ctx).doOnTerminate(new Action0(){
 			@Override
			public void call() {
 				debug(ctx);
			}
 		});
	}
	
	public void debug(Context ctx) {
		if(!debugEnabled.get()){
			return;
		}
		StringBuilder sb = getDebugInfo(ctx);
        System.out.println(sb.toString());
    }
	public StringBuilder getDebugInfo(Context ctx) {
		HttpServerRequest<ByteBuf> request=ctx.getRequest();
		HttpServerResponse<ByteBuf> response=ctx.getResponse();

		StringBuilder sb=new StringBuilder();
		sb.append("\r\n");
		sb.append("DebugExt>>>>>>>>>>>>>>>>>>>>>\r\n");
    	sb.append("request : "+request.getHttpMethod() + " " + request.getUri() +"\r\n");
        for (Map.Entry<String, String> header : request.getHeaders().entries()) {
        	sb.append("  "+header.getKey() + ": [" + header.getValue()+"]\r\n");
        }
        Form form=FormExt.getOrNull(ctx);
 		sb.append("request form:\r\n");
        if(form!=null&&!form.getAll().isEmpty()){
        	for (Entry<String, Object> item : form.getAll().entrySet()) {
        		Class<?> valueType=item.getValue()==null?null:item.getValue().getClass();
            	sb.append("  "+item.getKey() + ": [" + item.getValue()+"] isnull="+(item.getValue()==null)+" type="+valueType+"\r\n");
			}
        }
        sb.append("response : status="+response.getStatus().code() +" isCloseIssued="+response.isCloseIssued()+"\r\n");
        for (Map.Entry<String, String> header : response.getHeaders().entries()) {
        	sb.append("  "+header.getKey() + ": [" + header.getValue()+"]\r\n");
        }
        Object result=get(ctx);
        sb.append("response body: \r\n");
        sb.append(result==null?"":Jackson.pretty().encode(result)+"\r\n");
		sb.append("DebugExt<<<<<<<<<<<<<<<<<<<<"+request.getHttpMethod() + " " + request.getUri() +"\r\n");

  		if(!response.isCloseIssued()){
//         	sb.append(Jackson.pretty().encode(this)+"\r\n");
  		}
		return sb;
	}
	/**@reutrn get 已处理好的Form
	 * 
	 * @throws NullPointerException 若没有经过FormExt处理，则抛出异常
	 */
	public static Object get(Context ctx){
 		return ctx.getAttrs().get(ATTR);
	}
	/**@reutrn get 已处理好的Form
	 * 
	 * @throws NullPointerException 若没有经过FormExt处理，则抛出异常
	 */
	public static void set(Context ctx,Object result){
		ctx.getAttrs().put(ATTR,result);
	}

}