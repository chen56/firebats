package firebats.i18n;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import firebats.properties.memory.MemoryProperties;

public class RawI18nTest {
    @Test
    public void teatRawI18n(){
     	I18ns i18ns = I18ns.create("en-US");
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

    	I18n i18n=i18ns.select("en-US,en;q=0.8,zh-CN;q=1.0,zh;q=0.4");
    	assertEquals("username required",i18n.get("user.username.required"));
    	I18n i18n2=i18ns.select("zh-CN;q=1.0,zh;q=0.4,en-US,en;q=0.8");
    	assertEquals("需要输入用户名",i18n2.get("user.username.required"));
    	
    	assertEquals("null",i18ns.getDefault(),i18ns.select(null));
    	assertEquals("忽略ranges语法错误",i18ns.getDefault(),i18ns.select("a="));
    }
}