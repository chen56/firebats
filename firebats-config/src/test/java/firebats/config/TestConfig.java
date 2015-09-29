package firebats.config;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import com.netflix.config.ConcurrentMapConfiguration;

import firebats.config.Config;
import firebats.properties.Property;

public class TestConfig {
 	@SuppressWarnings("serial")
	@Test
    public void api() throws IOException{
 		Config config=createConfig(new HashMap<String,Object>(){{
 			put("string","string value");
 			put("int","1");
 			put("json","{'a':1}");
 		}});
 		
 		//风格A 这种其实最简单，只是取值时必须知道类型
// 		assertThat(config.get("string").getString()).isEqualTo("string value");
// 		assertThat(config.get("not exists").getString("default")).isEqualTo("default");
// 		assertThat(config.get("int").getInt()).isEqualTo(1);
// 		assertThat(config.get("not exists").getInt(-1)).isEqualTo(-1);
// 		assertThat(config.get("json").getJson()).isEqualTo(json({x=1}));
// 		assertThat(config.get("json").getObject(ClassA.class)).isEqualTo(new ClassA(1));
// 		
// 		//风格B 这种没有元数据，不要也罢
// 		assertThat(config.getString("string")).isEqualTo("string value");
// 		assertThat(config.getString("not exists","default")).isEqualTo("string value");
// 		assertThat(config.getInt("int")).isEqualTo(1);
// 		assertThat(config.getInt("not exists",-1)).isEqualTo(-1);
// 		
// 		//风格C ,当前风格：好处是，指定好配置类型，传递时，就不需要知道到底是得到int呢，还是boolean
 		assertThat(config.getPropertyFactory().of("string").get()).isEqualTo("string value");
 		assertThat(config.getPropertyFactory().of("not exists").withDefault("default").get()).isEqualTo("default");
 		assertThat(config.getPropertyFactory().of("int").withIntegerResult().get()).isEqualTo(1);
 		assertThat(config.getPropertyFactory().of("not exists").withIntegerResult().withDefault(-1).get()).isEqualTo(-1);
 		
 		//风格C 的扩展使用,这种更复杂的目前不需要
// 		assertThat(config.getJson("json").get()).isEqualTo(1);
// 		assertThat(config.getObject("json",ClassA.class).get()).isEqualTo(-1);
 	}
 	@Test
 	@SuppressWarnings("serial")
	public void parameter_notExists(){
 		//given
 		Config config = createConfig(new HashMap<String,Object>(){{
 			put("desc","${not exists} hello");
 		}});
 		assertThat(config.getOriginal())
 		//then get it:
		  .isEqualTo(new LinkedHashMap<String,Object>(){{
	 			put("desc","${not exists} hello");
		  }});
 	}
 	@Test
 	@SuppressWarnings("serial")
	public void parse(){
 		//given
 		Config config = createConfig(new HashMap<String,Object>(){{
 			put("name","chen56");
 			put("desc","${name} hello");
 		}});
 		
 		//when getOriginal
 		assertThat(config.getOriginal())
 		//then get it:
		  .isEqualTo(new LinkedHashMap<String,Object>(){{
	 			put("name","chen56");
	 			put("desc","${name} hello");
		  }});
 		
 		//when getResloved
 		assertThat(config.getResloved())
 		//then get it:
		  .isEqualTo(new LinkedHashMap<String,Object>(){{
	 			put("name","chen56");
	 			put("desc","chen56 hello");
		  }});
  	}
 	@SuppressWarnings("serial") 
 	@Test
 	public void 先到_先得(){
 		Config config = createConfig(new HashMap<String,Object>(){{
 			put("name","chen56");
 		}});
 		Property<Void,String> name = config.getPropertyFactory().of("name");
 		name.setString("new");
 		assertThat(name.get()).isEqualTo("chen56");
 	}
 	@Test
 	public void 先到_先得2(){
 		Config config = Config.newEmpty();
 		Property<Void,String> name = config.getPropertyFactory().of("name");
 		name.setString("new");
 		assertThat(name.get()).isEqualTo("new");
 	}

 	
 	private static Config createConfig(Map<String, Object> properties) {
 		ConcurrentMapConfiguration result = new ConcurrentMapConfiguration();
 		for (Map.Entry<String,Object> entry : properties.entrySet()) {
			result.addProperty(entry.getKey(), entry.getValue());
		}
		return Config.newEmpty().add("test", properties);
	}
 	@Test
 	@SuppressWarnings("serial")
	public void composite_1(){
 		//given 
 		Config config = createConfig(new HashMap<String,Object>(){{
 			put("desc","${name} like ${like}");
 			put("like","food");
 			
 		}});
 		config.add("filter", new HashMap<String,Object>(){{
 			put("name","chen56");
 			put("like","book");
 		}});
 		
 		//when getResloved
 		assertThat(config.getResloved())
 		//then get it:
		  .isEqualTo(new LinkedHashMap<String,Object>(){{
	 			put("like","food");
	 			put("name","chen56");
	 			put("desc","chen56 like food");
		  }});
 		
 		assertThat(config.getPropertyFactory().of("desc").get()).isEqualTo("chen56 like food");
  	}
 	@Test
 	@SuppressWarnings("serial")
	public void composite_2(){
 		//given
 		Config config = Config.newEmpty();
 		config.add("1",new HashMap<String,Object>(){{
 			put("desc","${name} like ${like}");
 			put("like","food");
 			
 		}});
 		config.add("2", new HashMap<String,Object>(){{
 			put("name","chen56");
 			put("like","book");
 		}});
 		//when getResloved
 		assertThat(config.getResloved())
 		//then get it:
		  .isEqualTo(new LinkedHashMap<String,Object>(){{
	 			put("like","food");
	 			put("name","chen56");
	 			put("desc","chen56 like food");
		  }});
 		assertThat(config.getPropertyFactory().of("desc").get()).isEqualTo("chen56 like food");
  	}

 	

	@SuppressWarnings("serial")
	@Test
    public void getProperty() throws IOException{
		 class GetProperty_ScenarioSteps{
		 		private Property<Void,String> property;
				private Config config;
				public void givenProperties(Map<String,Object> properties) {
					config=createConfig(properties);
				}
				public void thenGetStringValue(String expected) {
					assertThat(property.get()).isEqualTo(expected);
				}
				public void whenGet(String property) {
					this.property=config.getPropertyFactory().of(property);
				}
				public void thenIsEmptyProperty(boolean expected) {
					assertThat(property.isEmpty()).isEqualTo(expected);
				}
		}
		
		GetProperty_ScenarioSteps s= new GetProperty_ScenarioSteps();
		s.givenProperties(new HashMap<String,Object>(){{
 			put("name","chen peng");
 		}});
		{
			s.whenGet("name");
			s.thenGetStringValue("chen peng");
		}
		{
			s.whenGet("not exsits property");
			s.thenGetStringValue(null);
			s.thenIsEmptyProperty(true);
		}
    }
	
}