package firebats.http.server.exts.tree;

import java.util.Iterator;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import firebats.http.server.Chain;
import firebats.http.server.Context;
import firebats.http.server.Ext;
import firebats.http.server.exts.route.RouteExt;
import firebats.http.server.exts.route.Routes.Route;

public class TreeNode<TIn extends Context,TOut extends Context> {
	/*internal*/TreeNode<?,? extends TIn> parent;
	private Ext<TIn, TOut> ext;
	private TreeExt tree;
	public TreeNode(TreeExt tree,Ext<TIn,TOut> ext){
		this.tree=tree;
    	this.ext=ext;
     }
	public TreeNode<TOut,TOut> route(RouteExt<TOut> routeExt) {
		TreeNode<TOut,TOut> result=child(routeExt);
		for(Route route : routeExt.getRoutes().getAll()){
			tree.route_chain.put(route, chain(result));
			tree.routes.register(route);
		}
		return result;
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Chain chain(TreeNode<TOut, TOut> node) {
		Iterator<TreeNode> rootToMeChain= node.getListFromMeToRoot().iterator();
		Chain root=new Chain(rootToMeChain.next().getExt());
		Chain current=root;
		while(rootToMeChain.hasNext()){
			current=current.link(rootToMeChain.next().getExt());
		}
 		return root;
	}
	@SuppressWarnings("rawtypes")
	private List<TreeNode> getListFromMeToRoot() {
		if(getParent()==null)return Lists.newArrayList(this);
		List<TreeNode> result=this.getParent().getListFromMeToRoot();
		result.add(this);
 		return result;
	}
	public TreeNode<?,? extends TIn> getParent() {
 		return parent;
	}
	public <TNextOut extends Context,TNext extends Ext<TOut,TNextOut>> TreeNode<TOut,TNextOut> child(TNext nextExt) {
		Preconditions.checkNotNull(nextExt,"nextExt should not be null");
		TreeNode<TOut,TNextOut> next=new TreeNode<TOut,TNextOut>(tree,nextExt);
		next.parent=this;
		return next;
	}
    public Ext<TIn, TOut> getExt() {
		return ext;
	}
}