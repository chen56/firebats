package firebats.config;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;

import com.google.common.io.Resources;

public class FileConfigTest {
 	@Test
 	public void include() throws IOException, ConfigurationException{
 		Config c = Config.newEmpty().addFromResource("", Resources.getResource(this.getClass(), "FileConfigTest.conf"));
 		assertEquals("中文1",c.get("fileConfigTest.a"));
 		assertEquals("中文2",c.get("fileConfigTest2.a"));
 	}
}