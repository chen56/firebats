package firebats.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import firebats.reflect.TypeRef;

public class Jackson {
	private ObjectMapper objectMapper;

	private Jackson(ObjectMapper mapper) {
		this.objectMapper = mapper;
	}

	/**实际环境使用*/
	public static Jackson normal() {
		return Singleton.normal;
	}

	/**漂亮的格式化*/
	public static Jackson pretty() {
		return Singleton.pretty;
	}

	public String encode(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException("Failed to encode as JSON: "+ e.getMessage(), e);
		}
	}
	
	public byte[] encodeToByte(Object obj) {
		try(ByteArrayOutputStream  out = new ByteArrayOutputStream()) {
	        objectMapper.writeValue(out, obj);
	        return out.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("Failed to encode as JSON: "+ e.getMessage(), e);
		}
	}

	public <T> T decode(String str, TypeRef<T> typeToken) {
		try {
//			return objectMapper.readValue(str,toJacksonTypeReference(typeToken.getGuavaTypeToken()));
			return objectMapper.readValue(str,objectMapper.constructType(typeToken.getType()));
		} catch (Exception e) {
			throw new RuntimeException("Failed to dncode as JSON: "+ str, e);
		} 
	}

	public <T> T decode(String str, Class<T> typeToken) {
		try {
			return objectMapper.readValue(str, typeToken);
		} catch (Exception e) {
			throw new RuntimeException("Failed to decode:" +str, e);
		}
	}
	public <T> T decode(byte[] bytes,TypeRef<T> typeRef) {
		try {
			return objectMapper.readValue(bytes, objectMapper.constructType(typeRef.getType()));
		} catch (Exception e) {
			throw new RuntimeException("Failed to encode as JSON: "+ e.getMessage(), e);
		}
	}
	
	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

//	private static class JacksonTypeReferenceWarper<T> extends TypeReference<T> {
//		private Type runtimeType;
//		private JacksonTypeReferenceWarper(Type runtimeType) {
//			this.runtimeType = runtimeType;
//		}
//
//		@Override public Type getType() {
//			return runtimeType;
//		}
//	}

	private static class Singleton {
		private final static Jackson normal = new Jackson(new ObjectMapper());
		private final static Jackson pretty = new Jackson(new ObjectMapper());
		static {
			// Non-standard JSON but we allow C style comments in our JSON
			configBasic(normal.objectMapper);
			configBasic(pretty.objectMapper);
			pretty.objectMapper.configure(SerializationFeature.INDENT_OUTPUT,true);
		}

		private static void configBasic(ObjectMapper mapper) {
			mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, false);
			mapper.setSerializationInclusion(Include.NON_NULL);
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
			mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
			mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
			mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		}
	}


}
