package firebats.internal.bus.json;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

/*
 * ---------------------------------------------------
 * private
 * ---------------------------------------------------
 */
public class JacksonHelper{
	private static JsonFactory jsonFactory = new JsonFactory();
	/**
	 * Stream api 比较快，也比正则简单，只要字段比较靠前，就很快 快速找到json中某些字段的值，只找出第一级字段 例如
	 * find("{a:1,b:{c:2},d:3}","a","b") //result> {a:1,d:3}
	 */
	public static Map<String, String> find(String json, String... fields) {
		/*** read from file ***/
		Map<String, String> result = new HashMap<>();

		try (JsonParser p = jsonFactory.createParser(json)) {
			Set<String> fieldsSet = Sets.newHashSet(fields);
			int finded = 0;
			int currentLevel = 0;
			while (p.nextToken() != null) {
				if (finded == fields.length) {
					break;
				}
				JsonToken current = p.getCurrentToken();
				if (current.isStructStart()) {
					currentLevel += 1;
				} else if (current.isStructEnd()) {
					currentLevel -= 1;
				}
				if (currentLevel == 1) {
					String name = p.getCurrentName();
					if (fieldsSet.contains(name)) {
						p.nextToken();
						result.put(name, p.getText());
						finded += 1;
					}
				}
			}
			p.close();
		} catch (Throwable e) {
			Throwables.propagate(e);
		}
		return result;
	}
}