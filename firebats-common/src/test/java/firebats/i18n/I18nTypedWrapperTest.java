package firebats.i18n;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import rx.functions.Func1;
import firebats.properties.Property;
import firebats.properties.PropertyFactory;
import firebats.properties.memory.MemoryProperties;

public class I18nTypedWrapperTest {
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
    	UserI18nTypedWrapper userMessage=i18ns.select("en-US,en;q=0.8,zh-CN;q=1.0,zh;q=0.4",UserI18nTypedWrapper.class,new Func1<I18n,UserI18nTypedWrapper>(){
			@Override
			public UserI18nTypedWrapper call(I18n i18n) {
				return new UserI18nTypedWrapper(i18n);
			}
    	});
    	assertEquals("username required",userMessage.username_required.get());
    	
    	//模板
    	assertEquals("username[?] already exists",userMessage.username_alreadyExists.get());
    	assertEquals("username[chen] already exists",userMessage.username_alreadyExists.get(new UserI18nTypedWrapper.username_alreadyExists(){{username="chen";}}));
    }
    public static class UserI18nTypedWrapper{
    	protected final PropertyFactory factory=PropertyFactory.ofNoInit();
        public Property<Void, String> username_required=factory.of("user.username.required");
        public Property<username_alreadyExists, String> username_required2=factory.of("user.username.required").withArg(username_alreadyExists.class);
        public Property<username_alreadyExists,String> username_alreadyExists=factory.of("user.username.alreadyExists").withArg(username_alreadyExists.class) ;public static class username_alreadyExists{public String username;}
    
    	public UserI18nTypedWrapper(I18n i18n) {
    		factory.init(i18n.getResource());
    	}
    }
}