package firebats.internal.bus;

import rx.Observable;
import firebats.bus.RxBus.IRawRequest;
import firebats.component.IComponentProvider;
import firebats.component.funcs.IStartStop;

public interface IBusServer extends IStartStop,IComponentProvider{
	public abstract void startAndWait();
	/**@return 返回的Observable 绝对不会调用onError*/
	public abstract Observable<IRawRequest> getInput();
}