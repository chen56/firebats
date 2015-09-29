package firebats.http.server.exts.notfound;

import rx.Observable;
import firebats.http.server.Chain;
import firebats.http.server.Context;
import firebats.http.server.Ext;
/**打印request和response情况*/
public class NotFoundExt implements Ext<Context,Context>{
	@Override
	public Observable<?> call(Chain<Context,Context> chain,Context ctx) {
		throw new RuntimeException("NotFoundExt not impl ");
	}
}