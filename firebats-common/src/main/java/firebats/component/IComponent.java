package firebats.component;

import firebats.funcs.throwable.EAction1;

public interface IComponent{
	public <TFunc extends IComponentFunc> void invoke(Class<TFunc> funcClass,EAction1<TFunc> callback);
	public Object getSource();
}