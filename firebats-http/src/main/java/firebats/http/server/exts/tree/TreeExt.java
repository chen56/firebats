package firebats.http.server.exts.tree;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Action1;
import firebats.http.server.Chain;
import firebats.http.server.Context;
import firebats.http.server.Ext;
import firebats.http.server.exts.movenext.MoveNextExt;
import firebats.http.server.exts.route.Routes;
import firebats.http.server.exts.route.Routes.Route;
/**
 * 实验性模块，对http请求处理流进行多级扩展，目前还没有很多此类需求，暂时玩耍一下，看测试代码，可行的！
 */
public class TreeExt implements Ext<Context, Context> {
	private static Logger log=LoggerFactory.getLogger(TreeExt.class);
	/*internal*/Routes routes=new Routes();
	/*internal*/ 
	@SuppressWarnings("rawtypes") Map<Route,Chain> route_chain=new HashMap<>();

	private TreeNode<Context, Context> root=new TreeNode<>(this,new MoveNextExt());
	@SuppressWarnings("unchecked")
	@Override
	public Observable<?> call(Chain<Context,Context> chain,final Context ctx) {
		Route route=routes.select(ctx.getRequest());
		if(route==null) return chain.next(ctx);
		@SuppressWarnings("rawtypes")
		Chain newChain = route_chain.get(route);
		return newChain.call(ctx).doOnError(new Action1<Throwable>() {
			@Override
			public void call(Throwable e) {
 				log.error("error",e);
			}
		});
	}
	protected TreeNode<Context,Context> root() {
		return root;
	}
}