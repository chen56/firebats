package firebats.properties.memory;

import firebats.properties.IProperties;

public class EchoProperties implements IProperties{
	@Override
	public String get(String key) {
		return key;
	}
	@Override
	public void put(String key, String value) {
		//忽略
	}
}