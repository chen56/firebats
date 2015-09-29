package firebats.render;

import rx.functions.Action1;
/**
 * -->参数--经过(IRender)->结果
 */
public interface IRender<ARG,TResult>{
	public abstract TResult get(Action1<ARG> argSetter);
	public abstract TResult get(ARG args);
	public abstract TResult get();
}