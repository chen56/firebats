package firebats.i18n;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import rx.functions.Func1;
import firebats.check.Check;
import firebats.check.PropertiesCheckFactory;
import firebats.properties.memory.MemoryProperties;

public class I18nCheckTest {
 	I18ns i18ns = I18ns.create("en-US");
	@Before
	public void before(){
		i18ns.put("en-US",new MemoryProperties(){{
    		put("user.username.required","username required");
    		put("user.username.alreadyExists","username[${username}] already exists");
    	}});
		i18ns.put("zh-Hans",new MemoryProperties(){{
    		put("user.username.required","需要输入用户名");
    		put("user.username.alreadyExists","用户名[${username}] 已存在");
    	}});
		i18ns.put("zh",i18ns.get("zh-Hans").get().getResource());
		i18ns.put("en",i18ns.get("en-US").get().getResource());
	}
	
    @Test
    public void typed_property(){
    	UserI18nCheck userMessage=i18ns.select("en-US,en;q=0.8,zh-CN;q=1.0,zh;q=0.4",UserI18nCheck.class,new Func1<I18n,UserI18nCheck>(){
			@Override
			public UserI18nCheck call(I18n i18n) {
				return new UserI18nCheck(i18n);
			}
    	});
    	//模板
    	assertEquals("username[?] already exists",userMessage.username_alreadyExists.message());
    	assertEquals("username[chen] already exists",userMessage.username_alreadyExists.message(new UserI18nCheck.Username_AlreadyExists(){{username="chen";}}));
    }
    public static class UserI18nCheck{
    	protected final PropertiesCheckFactory factory=PropertiesCheckFactory.ofNoInit();
//    	protected final I18nPropertyFactory factory=I18nPropertyFactory.create();
     	Check<Username_AlreadyExists> username_alreadyExists  = factory.f(200005, "user.username.alreadyExists", Username_AlreadyExists.class);static class Username_AlreadyExists{public String username;}
    	public UserI18nCheck(I18n i18n) {
    		factory.init(i18n.getResource());
    	}
    }
}