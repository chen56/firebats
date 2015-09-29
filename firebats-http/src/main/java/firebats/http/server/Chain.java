package firebats.http.server;

import rx.Observable;

import com.google.common.base.Preconditions;

public class Chain<TIn,TOut> {
	private Ext<TIn, TOut> ext;
	private Chain<TOut,?> next;
	public Chain(Ext<TIn,TOut> ext) {
		Preconditions.checkNotNull(ext,"ext should not be null");
		this.ext=ext;
	}
 	/**
	 * 链接下一个责任链环节
	 */
	public <TNextOut,TNext extends Ext<TOut,TNextOut>> Chain<TOut,TNextOut> link(TNext nextExt) {
		Preconditions.checkNotNull(nextExt,"nextExt should not be null");
		Preconditions.checkState(this.next==null,"already has next:"+this.next);
		Chain<TOut,TNextOut> next=new Chain<TOut,TNextOut>(nextExt);
		this.next=next;
		return next;
	}
	public Chain<TOut,?> getNext(){
		return next;
	}
	public Observable<?> next(TOut ctx) {
  		return next==null?Observable.empty():next.call(ctx);
	}
	public Observable<?> call(TIn ctx) {
		try {
	  		return ext.call(this,ctx);
		} catch (Exception e) {
			return Observable.error(e);
		}
	}
	@Override
	public String toString(){
		return ext.toString();
	}
}