package firebats.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

public class TextAssert {
	public static final String DefaultSplit="\r\n";
    List<String> actualLines=new ArrayList<>();
	private String split;
	private String actual; 
	private TextAssert(String actual,String split) {
		this.actual=actual;
		this.actualLines=Lists.newArrayList(actual,DefaultSplit);
		this.split=split;
 	}

	public TextAssert $(String str) {
		actualLines.add(str);
		return this;
	}

	@Override
	public String toString() {
		return Joiner.on(split).join(actualLines);
	}
	
    public static TextAssert of(String str){
		return new TextAssert(str,DefaultSplit);
    }
    
    public static TextAssert of(){
 		return new TextAssert("",DefaultSplit);
    }

	/**
	 */
	public String toSbCode() {
		int max=0;
		for (String line : actualLines) {
			if(line.length()>max)max=line.length();
		}
		
		List<String> transform=Lists.newArrayList();
		for (String string : transform) {
			transform.add(String.format(".$(\"%1$-" + max + "s\")", string));
		}
		
		return String.format("TextAssert.of()%s%s%s;", 
				split,
				Joiner.on(split).join(transform),
				split);
	}
	public String toSbString() {
		int max=0;
		for (String line : actualLines) {
			if(line.length()>max)max=line.length();
		}
		List<String> transform=Lists.newArrayList();
		for (String string : transform) {
			transform.add( String.format("%1$-" + max + "s|", string));
		}
		return Joiner.on(split).join(transform);
	}
    public TextAssert printSbCode(){
    	System.out.println(toSbCode());
    	return this;
    }
	@Override
	public final boolean equals(Object other) {
		if(this==other)return true;
		if(!(other instanceof TextAssert)) return false;
		if(!canEqual(other)) return false;

		TextAssert that = (TextAssert) other;
		return Objects.equal(this.actualLines, that.actualLines);
	}

	public boolean canEqual(Object other) {
		return (other instanceof TextAssert);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(actualLines);
	}

	public JsonAssert jsonAssert() {
 		return JsonAssert.assertThat(toString());
	}
	
	public static TextAssert assertThat(String actual) {
		return of(actual);
	}
	/**
	 */
	public TextAssert shouldContains(String expected) {
		Assert.assertEquals(expected,actual);
		return this;
	}

	/**
	 */
	public TextAssert shouldEquals(String expected) {
//		TextAssert e=TextAssert.of(expected);
		Assert.assertEquals(expected,this.actual);
		return this;
	}

}