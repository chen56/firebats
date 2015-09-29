package firebats.test;


import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import firebats.test.JsonAssert;
import firebats.test.TextAssert;

public class TestJsonAssert {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
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
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
				false);
		mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
	}

    @Test
    public void actual应为json字串(){
    	errorJson("x");
    	errorJson("{");
    }
    
    private void errorJson(String json){
    	try {
    		json(json);
    		fail();
		} catch (Exception e) {
		}
    }
    //single quotes double quote
    @Test
    public void test() throws JsonParseException, JsonMappingException, JsonProcessingException, IOException{
    	json("{}").shouldContains("{}");
   
    	TextAssert.of()
    	.$("{                      ")
    	.$("  'status' : 'ok',     ")
    	.$("  'b' : '1'            ")
    	.$("}                      ")
    	.jsonAssert()
    	.shouldContains("{'status':'ok'}")
    	;
    }

	private String jsonStr(String json) {
 		return json.replace("'", "\"");
	}

	private JsonAssert json(String str) {
		return JsonAssert.assertThat(str.replace("'", "\""));
	}
	
}
