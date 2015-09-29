package firebats.component;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Throwables;

import firebats.component.funcs.IStart;
import firebats.component.funcs.IStop;
import firebats.funcs.throwable.EAction1;

public class Component implements IComponent{
	private Map<Class<? extends IComponentFunc>,IComponentFunc> funcs=new LinkedHashMap<>();
	private Object source;
	private Component(Object source){
		this.source=source;
	}

    public static ComponentContainer newContainer(Object source) {
		return new ComponentContainer(source);
	}
    /**
     * 
     */
    public static Component newComponent(Object source) {
		return new Component(source);
	}
	@Override
	public Object getSource() {
		return source;
	}
	public <TFunc extends IComponentFunc> Component on(Class<TFunc> funcClass, TFunc func) {
		funcs.put(funcClass,func);
		return this;
	}
	
	public Component onStart(IStart func) {
		return on(IStart.class,func);
	}
	
	public Component onStop(IStop func) {
		return on(IStop.class,func);
	}

	
	public <TFunc extends IComponentFunc> void invoke(Class<TFunc> funcClass,EAction1<TFunc> callback) {
		IComponentFunc func = funcs.get(funcClass);
		if(func!=null){
			try {
				callback.call(funcClass.cast(func));
			} catch (Exception e) {
				Throwables.propagate(e);
 			}
		}
	}
	public String toString(){
		return "-"+getSource();
	}


}