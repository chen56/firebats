package firebats.http.server.exts.movenext;

import rx.Observable;
import firebats.http.server.Chain;
import firebats.http.server.Context;
import firebats.http.server.Ext;
/**仅仅移动到next*/
public class MoveNextExt implements Ext<Context,Context>{
	@Override
	public Observable<?> call(Chain<Context,Context> chain,Context ctx) {
		return chain.next(ctx);
	}
}