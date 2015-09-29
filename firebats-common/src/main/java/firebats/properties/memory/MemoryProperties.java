package firebats.properties.memory;

import java.util.LinkedHashMap;
import java.util.Map;

import firebats.properties.IProperties;
/**测试使用*/
public class MemoryProperties implements IProperties{
    private Map<String,String> map=new LinkedHashMap<String,String>();
	@Override
	public String get(String key) {
		return map.get(key);
	}

	@Override
	public void put(String key, String value) {
		map.put(key, value);
 	}
}
