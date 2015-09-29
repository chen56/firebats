package firebats.templates;

import rx.functions.Action1;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.samskivert.mustache.Mustache;

import firebats.converter.Converter;
import firebats.render.IRender;

/**
 * 一个模板由2部分构成：<br/>
 * <ul>
 *   <li>mustache格式模板 </li>
 *   <li>模板参数(即mustache术语中的context), </li>
 * </ul>
 * 注意：我们修改了默认mustache的参数规则：默认{{xx}}，现在${xx} </br>
 * 比如，模板为 "通知:${somebody}结婚不收礼！", 参数为: {"somebody":"陈浩"}，其结果为： "通知:陈浩结婚不收礼！"
 */
public class Template<TArg,TResult> implements IRender<TArg,TResult> {
	private com.samskivert.mustache.Template template;
	private Class<TArg> argClass;
	private Converter<String, TResult> converter;
	
    private Template(com.samskivert.mustache.Template template,Class<TArg> argClass,Converter<String,TResult> converter){
    	this.template=template;
    	this.argClass=argClass;
    	this.converter=converter;
    }
    protected Template(Template<TArg,TResult> template){
    	this.template=template.template;
    	this.argClass=template.argClass;
    	this.converter=template.converter;
    }

	public static <TArg,TResult> Template<TArg,TResult> of(String template,Class<TArg> argClass,Converter<String,TResult> converter){
 		return of(template,converter).withArg(argClass);
 	}
	
	public static <TResult> Template<Void,TResult> of(String template,Converter<String,TResult> converter){
 		return new Template<Void,TResult>(template(template),Void.class,converter);
 	}
	
	public static <TArg> StringTemplate<TArg> ofString(String template,Class<TArg> argClass){
 		return ofString(template).withArg(argClass);
 	}
	
	public static StringTemplate<Void> ofString(String template){
 		return new StringTemplate<Void>(new Template<Void,String>(template(template),Void.class,Converter.String2String));
 	}
	
	private static com.samskivert.mustache.Template template(String template) {
		return Mustache.compiler().withDelims("${ }").nullValue("null").defaultValue("?").compile(Strings.nullToEmpty(template));
	}
	
	public <TNewArg> Template<TNewArg,TResult> withArg(Class<TNewArg> argClass){
		return new Template<TNewArg,TResult>(template,argClass,converter);
	}
	
    /* (non-Javadoc)
	 * @see firebats.templates.IRender#render(rx.functions.Action1)
	 */
    @Override
	public TResult get(Action1<TArg> arg){
    	return get(collectArg(arg));
    }
    
	/* (non-Javadoc)
	 * @see firebats.templates.IRender#render(TArg)
	 */
	@Override
	public TResult get(TArg args) {
    	return converter.apply(template.execute(args==null?ImmutableMap.of():args));
	}
	
	/* (non-Javadoc)
	 * @see firebats.templates.IRender#render()
	 */
	@Override
	public TResult get() {
    	return get((TArg)null);
	}

	private TArg collectArg(Action1<TArg> arg){
		//如果是Void参数类型，表示不需要模板参数
		if(Objects.equal(Void.class,argClass)){
			return null;
		}
		TArg result=newInstance(argClass);
		if(arg!=null)	arg.call(result);
		return result;
	}
	
	private static <TArg> TArg newInstance(Class<TArg> clazz){
		try {
			return clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			Throwables.propagate(e);
		}
		//not here
		return null;
	}
	
	public static class StringTemplate<TArg> implements IRender<TArg,String> {
		private Template<TArg, String> template;

		StringTemplate(Template<TArg,String> t){
			this.template=t;
		}
		
		@Override
		public String get(Action1<TArg> arg) {
			return template.get(arg);
		}

		@Override
		public String get(TArg arg) {
			return template.get(arg);
		}

		@Override
		public String get() {
			return template.get();
		}
		public <TNewArg> StringTemplate<TNewArg> withArg(Class<TNewArg> argClass){
			return new StringTemplate<TNewArg>(template.withArg(argClass));
		}

	}
}
