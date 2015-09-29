package firebats.reflect;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestClasses {
    static class A{
    	String s ;
    }
	A a;
	String b;
	@Test
    public void test1(){
    	assertEquals(null,fieldValue("/b"));
    	Classes.setFieldValue(this,"/b","s");
    	assertEquals("s",b);
    	assertEquals("s",fieldValue("/b"));

    	Classes.setFieldValue(this,"/a/s","s");
    	assertEquals("s",a.s);
    	assertEquals("s",fieldValue("/a/s"));

    	Classes.setFieldValue(this,"/a/s/notExsits","s");
    	Classes.setFieldValue(this,"","s");
    	Classes.setFieldValue(this,"/","s");
    }
	private Object fieldValue(String path) {
		return Classes.getFieldValueByPath(this,path);
	}
}
