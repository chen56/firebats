package firebats.component;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import firebats.component.funcs.IStart;
import firebats.component.funcs.IStop;
import firebats.funcs.throwable.EAction1;

public class ComponentContainer implements IComponent{
	private	static Logger log=LoggerFactory.getLogger(ComponentContainer.class);
	private List<IComponent> children=new ArrayList<>();
	private boolean ignoreException;
	private Object source;
	/*internal*/ ComponentContainer(Object source){
		this.source=source;
	}
	
	public void add(IComponent child) {
		this.children.add(child);
	}
	public void add(IComponentProvider child) {
		add(child.getComponent());
	}
	private ComponentContainer(Object source,List<IComponent> children,boolean ignoreException) {
		this(source);
 		this.children=children;
		this.ignoreException=ignoreException;
	}
	public void start(){
		invoke(IStart.class,new EAction1<IStart>() {
			@Override public void call(IStart t1) throws Exception {
			    t1.start();
			}
		});
	}
	public void stop(){
		ignoreException(true).reverse().invoke(IStop.class,new EAction1<IStop>() {
			@Override public void call(IStop t1) throws Exception {
			    t1.stop();
			}
		});
	}
	public ComponentContainer reverse(){
		List<IComponent> x=new ArrayList<>();
		for (IComponent component : Lists.reverse(children)) {
			if(component instanceof ComponentContainer){
				ComponentContainer c=(ComponentContainer)component;
				x.add(c.reverse());
			}else{
				x.add(component);
			}
		}
		return new ComponentContainer(getSource(),x,ignoreException);
	}
	@Override
	public Object getSource() {
		return source;
	}

	public <TFunc extends IComponentFunc> void invoke(Class<TFunc> funcClass,EAction1<TFunc> callback) {
		for (IComponent component : children) {
			if(ignoreException){
				try {
	    			component.invoke(funcClass,callback);
				} catch (Throwable e) {
					log.error("invoke component func error",e);
				}
			}else{
    			component.invoke(funcClass,callback);
			}
		}
	}
	public ComponentContainer ignoreException(boolean ignoreException) {
		this.ignoreException=ignoreException;
		return this;
	}
	public String toString(int level){
		StringBuilder sb=new StringBuilder();
		sb.append(Strings.padStart("",level*2, ' ')+"+"+getSource()+"\r\n");
		for (IComponent component : children) {
			if(component instanceof ComponentContainer){
				ComponentContainer c=(ComponentContainer)component;
				sb.append(c.toString(level+1));
			}else{
				sb.append(Strings.padStart("",(level+1)*2, ' ')+"-"+component+"\r\n");
			}
		}
		return sb.toString();
	}

	public String toString(){
		return toString(0);
	}
}