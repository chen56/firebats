package firebats.config;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.netflix.config.ConcurrentMapConfiguration;

import firebats.properties.Property;
import firebats.properties.PropertyFactory;

public class PropertyFactoryTest {
 	@SuppressWarnings("serial")
	@Test
    public void ConfigProperty两阶段初始化() throws IOException{
 		Config config=createConfig(new HashMap<String,Object>(){{
 			put("director","chen56");
 		}});
 		ZooConfig zoo=new ZooConfig(config);
 		assertEquals("chen56",zoo.director.get());
 	}
	@SuppressWarnings("serial")
	@Test
    public void notExists() throws IOException{
 		Config config=createConfig(new HashMap<String,Object>(){{
 		}});
 		//对配置来说，不存在的配置为null
 		assertEquals(null,config.get("director"));

 		ZooConfig zoo=new ZooConfig(config);
 		assertEquals(null,zoo.director.get());
 		assertEquals(true,zoo.director.isEmpty());
 	}
	@SuppressWarnings("serial")
	@Test
    public void withDefault() throws IOException{
 		Config config=createConfig(new HashMap<String,Object>(){{
 		}});
 		//对配置来说，不存在的配置为null
 		assertEquals(null,config.get("withDefaultString"));

 		ZooConfig zoo=new ZooConfig(config);
 		//对Property来说，每一个都是Template,而Template没有null的概念
 		assertEquals("DefaultString",zoo.withDefaultString.get());
 		assertEquals(Integer.valueOf(1),zoo.withDefaultInteger.get());
 	}
 	
 	private static class ZooConfig{
 		private PropertyFactory factory=PropertyFactory.ofNoInit();
 		private Property<Void,String> director=factory.of("director");
 		private Property<Void,String> withDefaultString=factory.of("withDefaultString").withDefault("DefaultString");
 		private Property<Void,Integer> withDefaultInteger=factory.of("withDefaultInteger").withIntegerResult().withDefault(1);
 		ZooConfig(Config config){
 			factory.init(config);
 		}
 	}
 	private static Config createConfig(Map<String, Object> properties) {
 		ConcurrentMapConfiguration result = new ConcurrentMapConfiguration();
 		for (Map.Entry<String,Object> entry : properties.entrySet()) {
			result.addProperty(entry.getKey(), entry.getValue());
		}
		return Config.newEmpty().add("test", properties);
	}
}