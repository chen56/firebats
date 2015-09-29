package firebats.json;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import firebats.reflect.TypeRef;

public class JsonTest {
    @Test
    public void test1(){
    	assertEquals("{'fInt':1}",firebats.json.Jackson.normal().encode(new A()).replaceAll("\"","'"));
    	A a = Jackson.normal().decode("{'fInt':2}".replaceAll("'","\""), new TypeRef<A>(){});
    	assertEquals(2,a.fInt);
    	a = Jackson.normal().decode("{'fInt':null}".replaceAll("'","\""), new TypeRef<A>(){});
    	assertEquals(0,a.fInt);
    }
    @Test
    public void test2(){
    	String x="{\"c\":\"chat.message_push\",\"data\":{\"span\":\"1000\",\"message\":\"你好啊\",\"time\":\"2015-02-10 12:41:15\",\"username\":\"MartinHwang\",\"roomId\":\"544e007ab99a88bc8b6c401e\",\"status\":\"success\"}}";
    	Object readed = Jackson.normal().decode(x,new TypeRef<Map<String,Object>>() {});
    	assertEquals(x,Jackson.normal().encode(readed));
    	System.out.println(Jackson.normal().encode(new ApiBody(readed)));
     }

	public static class ApiBody{
		public Object data;
 		public ApiBody(Object data){
			this.data=data;
		}
 	}


    static class A{
    	int fInt=1;
    }
}