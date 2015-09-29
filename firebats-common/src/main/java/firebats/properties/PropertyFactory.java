package firebats.properties;

import static com.google.common.base.Preconditions.checkNotNull;
import firebats.converter.Converter;
/**
 * 主要用于2阶段初始化的场景</p>
 * 范例：
 * <pre>
</pre>
     * <blockquote><pre>
    @Test public void ConfigProperty两阶段初始化() throws IOException{
 		Config config=createConfig(new HashMap<String,Object>(){{
 			put("director","chen56");
 		}});
 		ZooConfig zoo=new ZooConfig(config);
 		assertEquals("chen56",zoo.director.get());
 	}
 	private static class ZooConfig{
 		private ConfigPropertyFactory factory=Config.newPropertyFactory();
 		private ConfigProperty<String> director=factory.getString("director");
 		ZooConfig(Config config){
 			factory.initConfig(config);
 		}
 	}
     * </pre></blockquote>
 */
public class PropertyFactory implements IProperties {
	private IProperties properties;

	private PropertyFactory() {
	}
	public static PropertyFactory ofNoInit() {
		return new PropertyFactory();
	}
	public static PropertyFactory of(IProperties properties) {
		PropertyFactory result=new PropertyFactory();
		result.init(properties);
 		return result;
	}

 	/**
 	 * 两阶段初始化，把刚才通过此factory创建的所有ConfigProperty都绑定到此参数config
 	 */
	public PropertyFactory init(IProperties properties) {
		this.properties=properties;
		return this;
	}

 	public Property<Void, String> of(String key) {
		return new Property<Void,String>(this,Converter.String2String,Void.class,key);
	}
 	
	@Override
	public String get(String key) {
 		return getProperties().get(key);
	}

	@Override
	public void put(String key, String value) {
		getProperties().put(key, value);		
	}
	
	private IProperties getProperties() {
		return checkNotNull(properties,"config 还未绑定，不能使用ConfigProperty");
	}
}