package firebats.http.server.exts.cors;

import rx.Observable;
import rx.functions.Action0;
import firebats.http.server.Chain;
import firebats.http.server.Context;
import firebats.http.server.Ext;
/**打印request和response情况*/
public class HttpAccessControlCorsExt implements Ext<Context,Context>{
	@Override
	public Observable<?> call(Chain<Context,Context> chain,Context ctx) {
		return chain.next(ctx).doOnTerminate(new Action0(){
 			@Override
			public void call() {
 				//陈鹏 使用nginx配置Access-Control-Allow-Origin是不是更好？
 				ctx.getResponse().getHeaders().setHeader("Access-Control-Allow-Origin", "*");
 				ctx.getResponse().getHeaders().setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
 				ctx.getResponse().getHeaders().setHeader("Access-Control-Allow-Credentials", "true");
  			}
 		});
	}
}