package firebats.http.server.exts.form;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;

import play.data.Form;
import firebats.json.Jackson;

public class Play2FormTest {
    @SuppressWarnings("serial") @Test
    public void test1(){
		Form<FormData> f=Form.form(FormData.class);
    	f=f.bind(new LinkedHashMap<String,String>(){{
    		put("string","string");
    		put("stringArray[0]","$a0");
    		put("stringArray[1]","$a1");
    		put("objectArray[0].string","$objectArray[0].string");
    		put("objectArray[0].stringArray[0]","$objectArray[0].stringArray[0]");
    		put("objectArray[0].objectArray[0].string","$objectArray[0].objectArray[0].string");
    		put("objectMap[a].string","$objectMap[a].string");
    	}});
    	System.out.println("test play form"+f.get());
    }
    //spring 数据绑定要求：必须有getter和setter
    @SuppressWarnings("serial") @Test
    public void testSpring(){
    	LinkedHashMap<String, String> data = new LinkedHashMap<String,String>(){{
    		put("string","a1");
    		put("stringArray[0]","a0");
    	}};
    	DataBinder   dataBinder = new DataBinder(new FormData());
    	dataBinder.setAllowedFields("string","stringArray");
//        dataBinder.setConversionService(play.data.format.Formatters.conversion);
        dataBinder.setAutoGrowNestedPaths(true);
        dataBinder.bind(new MutablePropertyValues(data));
        BindingResult result = dataBinder.getBindingResult();
 	    System.out.println(result.getTarget());
    }
    public static class FormData{
    	public String string;
		public ArrayList<String> stringArray=new ArrayList<>();
    	public SubData[] objectArray;
    	public Map<String,SubData> objectMap;

    	public Map<String, SubData> getObjectMap() {
			return objectMap;
		}
		public void setObjectMap(Map<String, SubData> objectMap) {
			this.objectMap = objectMap;
		}
		public String getString() {
			return string;
		}
		public void setString(String string) {
			this.string = string;
		}
		public ArrayList<String> getStringArray() {
			return stringArray;
		}
		public void setStringArray(ArrayList<String> stringArray) {
			this.stringArray = stringArray;
		}
		public SubData[] getObjectArray() {
			return objectArray;
		}
		public void setObjectArray(SubData[] objectArray) {
			this.objectArray = objectArray;
		}
		public String toString(){
			return Jackson.pretty().encode(this);
		}
    }
    public static class SubData{
    	public String string;
		public ArrayList<String> stringArray=new ArrayList<>();
    	SubData[] objectArray;

    	public String getString() {
			return string;
		}
		public void setString(String string) {
			this.string = string;
		}
		public ArrayList<String> getStringArray() {
			return stringArray;
		}
		public void setStringArray(ArrayList<String> stringArray) {
			this.stringArray = stringArray;
		}
		public SubData[] getObjectArray() {
			return objectArray;
		}
		public void setObjectArray(SubData[] objectArray) {
			this.objectArray = objectArray;
		}
    }
}
