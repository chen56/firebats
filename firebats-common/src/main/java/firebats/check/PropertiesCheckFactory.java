package firebats.check;

import firebats.properties.IProperties;
import firebats.properties.PropertyFactory;

public class PropertiesCheckFactory implements IProperties {
	private PropertyFactory factory=PropertyFactory.ofNoInit();

	public static PropertiesCheckFactory ofNoInit() {
		return new PropertiesCheckFactory();
	}

	@Override
	public String get(String key) {
		return factory.get(key);
	}

	@Override
	public void put(String key, String value) {
		factory.put(key, value);
	}

 	/**
 	 * 两阶段初始化，把刚才通过此factory创建的所有ConfigProperty都绑定到此参数config
 	 */
	public PropertiesCheckFactory init(IProperties properties) {
		this.factory.init(properties);
		return this;
	}

	public <TArg> Check<TArg> f(int errorCode, String key, Class<TArg> argClass) {
		return Check.<TArg>of(CheckErrorType.Fail,errorCode,factory.of(key).withArg(argClass));
	}
 
	public Check<Void> f(int errorCode, String key) {
		return Check.<Void>of(CheckErrorType.Fail,errorCode,factory.of(key));
	}
	public <TArg> Check<TArg> e(int errorCode, String key, Class<TArg> argClass) {
		return Check.<TArg>of(CheckErrorType.Exception,errorCode, factory.of(key).withArg(argClass));
	}
	public Check<Void> e(int errorCode, String key) {
		return Check.<Void>of(CheckErrorType.Exception,errorCode,factory.of(key));
	}

}
