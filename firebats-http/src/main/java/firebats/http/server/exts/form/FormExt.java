package firebats.http.server.exts.form;

import io.netty.buffer.ByteBuf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Func1;

import com.google.common.base.Preconditions;

import firebats.http.server.Chain;
import firebats.http.server.Context;
import firebats.http.server.Ext;
//TODO uploadfile资源释放，从Context#complete移动到链中
public class FormExt implements Ext<Context,Context> {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(FormExt.class);

	protected static final String ATTR = "ext.form";
	@Override
	public Observable<?> call(Chain<Context,Context>chain,final Context ctx) {
 		if(contains(ctx)){
	    	return chain.next(ctx);
		}
       	return ctx.getRequest().getContent().flatMap(new Func1<ByteBuf, Observable<?>>() {
		    @Override 
		    public Observable<?> call(ByteBuf content) {
 		    	Form form=Form.decode(ctx, content);
			    ctx.getAttrs().put(ATTR, form);
				return chain.next(ctx);
		    }
		});
	}
	
	/**@reutrn get 已处理好的Form
	 * 
	 * @throws NullPointerException 若没有经过FormExt处理，则抛出异常
	 */
	public static Form get(Context ctx){
 		return Preconditions.checkNotNull((Form)ctx.getAttrs().get(ATTR),"dependences FormExt,are you chain it?");
	}
	/**
	 * @return get 已处理好的Form,当没有经过FormExt处理时，可能为null
	 * */
	public static Form getOrNull(Context ctx){
		return (Form)ctx.getAttrs().get(ATTR);
	}
	
	public static boolean contains(Context ctx){
		return ctx.getAttrs().containsKey(ATTR);
	}
	
	@Override
	public String toString(){
		return ATTR;
	}
}
