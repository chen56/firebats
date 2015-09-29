package firebats.templates;

import java.util.List;

import org.fest.assertions.api.Assertions;
import org.junit.Test;

import rx.functions.Action1;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import firebats.converter.Converter;
import firebats.render.IRender;

public class TemplateExtTest {
	public static class ContextArgs{
		public String somebody;
	}
	static class TemplateSpec<T>{
		private IRender<ContextArgs,T> template;
		private T result;
		static  <T> TemplateSpec<T> given(String template,Converter<String,T> provider){
			TemplateSpec<T> result=new  TemplateSpec<T>();
			result.template=Template.of(template, ContextArgs.class,provider);
			return result;
		}
		public void whenRenderByCollector(Action1<ContextArgs> collector) {
			result=template.get(collector);
		}
		public void thenResultIs(T expected) {
	    	Assertions.assertThat(result).isEqualTo(expected);
		}
		public void whenRenderByArgs(ContextArgs args) {
			result=template.get(args);
		}
	}
    @Test
    public void 模板execute_Action收集将得到结果(){
    	TemplateSpec<List<String>> c= TemplateSpec.given("通知:${somebody}结婚不收礼！",Converter.of(new Function<String,List<String>>(){
			@Override public List<String> apply(String text) {
				return Lists.newArrayList(text);
			}
    	}));
    	
    	//action正常参数
    	//action正常参数
    	c.whenRenderByArgs(new ContextArgs(){{somebody="陈浩";}});
    	c.thenResultIs(Lists.newArrayList("通知:陈浩结婚不收礼！"));
    }
    
}
