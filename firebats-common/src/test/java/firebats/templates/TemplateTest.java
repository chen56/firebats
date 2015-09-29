package firebats.templates;

import org.fest.assertions.api.Assertions;
import org.junit.Test;

import rx.functions.Action1;
import firebats.render.IRender;
import firebats.templates.Template.StringTemplate;

public class TemplateTest {
	public static class ContextArgs{
		public String somebody;
	}
	class TemplateSpec{
		private IRender<ContextArgs,String> template;
		private String result;
		void given(String template){
			this.template=Template.ofString(template, ContextArgs.class);
		}
		public void whenRenderByCollector(Action1<ContextArgs> collector) {
			result=template.get(collector);
		}
		public void thenResultIs(String expected) {
	    	Assertions.assertThat(result).isEqualTo(expected);
		}
		public void whenRenderByArgs(ContextArgs args) {
			result=template.get(args);
		}
	}
    @Test
    public void 模板execute_Action收集将得到结果(){
    	
    	TemplateSpec c=new TemplateSpec();
    	c.given("通知:${somebody}结婚不收礼！");
    	
    	//action正常参数
    	c.whenRenderByCollector(new Action1<ContextArgs>(){
			@Override public void call(ContextArgs t1) {
				t1.somebody="陈浩";
			}});
    	c.thenResultIs("通知:陈浩结婚不收礼！");
    	
    	//action参数字段为null
    	c.whenRenderByCollector(new Action1<ContextArgs>(){
			@Override public void call(ContextArgs t1) {
				t1.somebody=null;
			}});
    	c.thenResultIs("通知:?结婚不收礼！");
    	
    	//action参数为null
     	c.whenRenderByCollector(null);
    	c.thenResultIs("通知:?结婚不收礼！");
    }
    
    @Test
    public void 模板execute_Action收集将得到结果2(){
    	TemplateSpec c=new TemplateSpec();
    	c.given("通知:${somebody}结婚不收礼！");
    	
    	//action正常参数
    	c.whenRenderByArgs(new ContextArgs(){{somebody="陈浩";}});
    	c.thenResultIs("通知:陈浩结婚不收礼！");
    	
    	//action参数字段为null
    	c.whenRenderByArgs(new ContextArgs(){{somebody=null;}});
    	c.thenResultIs("通知:?结婚不收礼！");
    	
    	//action参数为null
    	c.whenRenderByArgs(null);
    	c.thenResultIs("通知:?结婚不收礼！");
    }

}
