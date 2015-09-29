package firebats.config;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;

public class ConfigParseTest {
 	@Test
 	public void utf8() throws IOException, ConfigurationException{
 		Config c = load("zh=中文");
 		assertEquals("中文",c.get("zh"));
 	}
 	
 	@Test
 	public void autoTrim() throws IOException, ConfigurationException{
 		Config c = load("containsSpace= 会自动  trim ");
 		assertEquals("会自动  trim",c.get("containsSpace"));
 	}
 	
 	@Test
 	public void notExists() throws IOException, ConfigurationException{
 		Config c = load("");
 		assertEquals(null,c.get("notExistsProperty"));
 	}
 	
 	@Test
 	public void specialCharacters_slash() throws IOException, ConfigurationException{
  		Config c = load("SpecialCharacters=~!@#$%^&*()_+{}[]:\"|;'\\<>?,./      \r\n"
			          + "SpecialCharacters.comma=a,b            \r\n"
				      + "SpecialCharacters.point=a.b            \r\n"
  				      + "SpecialCharacters.slash=a/b       \r\n"
  				      + "SpecialCharacters.backslash=陈\\鹏                        \r\n");
 		assertEquals("~!@#$%^&*()_+{}[]:\"|;'<>?,./",c.get("SpecialCharacters"));
 		assertEquals("a,b",c.get("SpecialCharacters.comma"));
 		assertEquals("a.b",c.get("SpecialCharacters.point"));
 		assertEquals("a/b",c.get("SpecialCharacters.slash"));
  		assertEquals("陈鹏",c.get("SpecialCharacters.backslash"));
 	}
 	
 	@Test
 	public void multiLine() throws IOException, ConfigurationException{
  		Config c = load("multiLine=\\   \r\n"
  				       +"a\\    \r\n"
  				       +"b\\c   \r\n");
 		assertEquals("abc",c.get("multiLine"));
 	}
 	
 	private Config load(String content){
 		return Config.newEmpty().addFromString("", content);
 	}
}