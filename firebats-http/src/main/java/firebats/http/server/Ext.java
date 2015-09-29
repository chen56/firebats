package firebats.http.server;

import java.util.List;

import rx.Observable;

import com.google.common.collect.Lists;


public interface Ext<TIn,TOut> {
	Observable<?> call(Chain<TIn,TOut>chain,TIn ctx);
	
	/**
	 * 某一个ext可能希望父链上已包含某种ext，可以通过此方法声明出来，做为错误检查的依据，
	 * TODO IExt目前还未做dependences检查*/
	@SuppressWarnings("rawtypes")
	default List<Class> dependences(){
		return Lists.newArrayList();
	}
}