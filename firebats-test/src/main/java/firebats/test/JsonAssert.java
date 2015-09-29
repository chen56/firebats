package firebats.test;

import java.io.IOException;

import org.junit.Assert;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * 默认情况，数组是严格比较长度的 ，而object是比较字段是否包含的
 * 
 * 
 */
public class JsonAssert {
	private JsonNode actual;
	private final static ObjectMapper mapper = new ObjectMapper();
	private final static ObjectMapper prettyMapper = new ObjectMapper();

	static {
		// Non-standard JSON but we allow C style comments in our JSON
		configBasic(mapper);
		configBasic(prettyMapper);
		prettyMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		

	}

	private static void configBasic(ObjectMapper mapper) {
 		mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
		mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);


	}

	public static JsonAssert assertThat(String actual) {
		JsonAssert result = new JsonAssert();
		try {
			result.actual = mapper.readTree(actual);
		} catch (IOException e) {
			throw new IllegalArgumentException(
					"actual should be a json string,but:\r\n" + actual, e);
		}
		return result;
	}

	public static JsonAssert assertThat(Object actual) {
		try {
			return assertThat(mapper.writeValueAsString(actual));
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("format json error: " + actual,
					e);
		}
	}

	/** 数组是否也做contains处理，默认情况，数组是严格比较长度的 */
	public JsonAssert withArrayContains() {
		JsonAssert result = new JsonAssert();
		result.actual = actual;
		return result;
	}

	/**
	 * 比较expected的json是否包含于actual中，注意，数组是严格比较长度的，但数组的元素也是比较包含的
	 */
	public JsonAssert shouldContains(String expected) {
		try {
			shouldContains(mapper.readTree(expected), actual, "");
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"expected should be a json string,but:" + expected, e);
		}
		return this;
	}

	/**
	 * 比较expected的json是否包含于actual中，注意，数组是严格比较长度的，但数组的元素也是比较包含的
	 */
	public JsonAssert shouldEquals(String expected) {
		try {
			Assert.assertEquals(mapper.readTree(expected), actual);
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"expected should be a json string,but:" + expected, e);
		}
		return this;
	}

	private static void shouldContains(Object expected, Object actual,
			String path) {
		if (expected instanceof ObjectNode) {
			if (actual instanceof ObjectNode) {
				ObjectNode expectedObj = (ObjectNode) expected;
				ObjectNode actualObject = (ObjectNode) actual;
				for (String fieldName : Lists.newArrayList(expectedObj
						.fieldNames())) {
					Object expectedField = expectedObj.get(fieldName);
					shouldContains(expectedField, actualObject.get(fieldName),
							path + "/" + fieldName);
				}
			} else {
				Assert.fail(path + "should be a object:" + actual.getClass());
			}
		} else if (expected instanceof ArrayNode) {
			if (actual instanceof ArrayNode) {
				assertJsonArray(((ArrayNode) expected), ((ArrayNode) actual),
						path);
			} else {
				Assert.fail(path + " should be a array:" + actual);
			}
		} else {
			Assert.assertEquals(
					String.format("path[%s] should be equal", path), expected,
					actual);
		}
	}

	private static void assertJsonArray(ArrayNode expecteds, ArrayNode actuals,
			String path) {
		if (expecteds == actuals) {
			return;
		}
		int expectedsLength = assertArraysAreSameLength(expecteds, actuals,
				path);
		for (int i = 0; i < expectedsLength; i++) {
			shouldContains(expecteds.get(i), actuals.get(i), path + "/" + i);
		}
	}

	private static int assertArraysAreSameLength(ArrayNode expecteds,
			ArrayNode actuals, String path) {
		if (expecteds == null) {
			Assert.fail(path + " expected array was null");
		}
		if (actuals == null) {
			Assert.fail(path + " actual array was null");
		}
		if (actuals.size() != expecteds.size()) {
			Assert.fail(path + " array lengths differed, expected.length="
					+ expecteds.size() + " actual.length=" + actuals.size());
		}
		return expecteds.size();
	}

	public JsonAssert printAssertString() {
		try {
			Object v = prettyMapper.treeToValue(actual, Object.class);
			String json = prettyMapper.writeValueAsString(v);
			System.out.println(TextAssert.of(json.replace("\"", "'")).toSbCode());
		} catch (JsonProcessingException e) {
			Throwables.propagate(e);
		}
		return this;
	}
}
