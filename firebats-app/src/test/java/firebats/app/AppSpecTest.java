package firebats.app;

import static firebats.nio.Paths2.path;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import firebats.app.App;

public class AppSpecTest {
 	private static final Path AppHome = new File(".").toPath();
	static String log = "";
	@Before
	public void setup(){
		log="";
	}

	 @Test
	public void 约定路径() throws NumberFormatException, IOException{
		//given
		App appSpec=App
				.builder(AppHome,"test")
				.instance("chen56")
				.build();
		assertThat(appSpec.getResourcesDir()).isEqualTo(AppHome.resolve(path("resources")));
		assertThat(appSpec.getStageDir()).isEqualTo(AppHome.resolve(path("resources/stages/test")));
		assertThat(appSpec.getStageAppConfFile()).isEqualTo(AppHome.resolve(path("resources/stages/test/app.conf")));
		assertThat(appSpec.getInstanceDir()).isEqualTo(AppHome.resolve(path("resources/instances/chen56")));
		assertThat(appSpec.getInstanceAppConfFile()).isEqualTo(AppHome.resolve(path("resources/instances/chen56/app.conf")));
	}
	 @Test
	public void pidFile() throws NumberFormatException, IOException{
		//given
		 App appSpec=App
				.builder(AppHome,"test")
				.instance("chen56")
				.build();
		assertThat(appSpec.getPidFile()).isEqualTo(AppHome.resolve(path("app.pid")));
		
	}

	 @Test
	public void instance_default() throws NumberFormatException, IOException{
 		//given
		 App appSpec=App
				.builder(AppHome,"test")
				.host("testHost")
				.name("testApp")
				.build();
		assertThat(appSpec.getAppConfig().getHome().toString()).isEqualTo(".");
		assertThat(appSpec.getAppConfig().getHost()).isEqualTo("testHost");
		assertThat(appSpec.getAppConfig().getInstance()).isEqualTo("default");
		assertThat(appSpec.getAppConfig().getName()).isEqualTo("testApp");
		assertThat(appSpec.getAppConfig().getPid()).isNotNull();
		assertThat(appSpec.getAppConfig().getStage()).isEqualTo("test");
	}
	 @SuppressWarnings("serial")
	@Test
	public void instance_chen56() throws NumberFormatException, IOException{
 		//given
		 App appSpec=App
				.builder(AppHome,"test")
 				.host("chenHost(java)")
				.name("testApp")
				.instance("chen56")
				.build();
		Map<String, Object> config = appSpec.getConfig().getResloved();
		config.remove("app.pid");
 		assertThat(config)
 		//then get it:
		  .isEqualTo(new LinkedHashMap<String,Object>(){{
	 			put("developer","chenpeng(instances/chen56)");
	 			put("project","firebats(instances/chen56)");
	 			put("title","chenpeng(instances/chen56) dev firebats(instances/chen56)(stages/test)");
	 			put("app.stage","test");
	 			put("app.home",".");
	 			put("app.instance","chen56");
	 			put("app.host","chenHost(java)");
	 			put("app.name","testApp");
		  }});
	}
	 
	 
	 @Test
	public void default_lookup_host() throws NumberFormatException, IOException{
		App appSpec=App
				.builder(AppHome,"test")
				.build();
		System.out.println("default_lookup_host:"+appSpec.getHost());
		assertThat(appSpec.getInstance()).isEqualTo(App.DEFALT_INSTANCE);
		assertThat(appSpec.getHost()).isNotNull();
	}
	 
	 @Test
	public void default_lookup_pid() throws NumberFormatException, IOException{
		 App appSpec=App
				.builder(AppHome,"test")
				.build();
		assertThat(appSpec.getInstance()).isEqualTo(App.DEFALT_INSTANCE);
		assertThat(appSpec.getPid()).isNotNull();
	}
	 @Test
	public void stages_not_exists() throws NumberFormatException, IOException{
		try {
			App.builder(AppHome,"stages_not_exists")
				.build();
            fail();
		} catch (Exception e) {
			assertThat(e).hasMessage("instance[default] dir[.\\resources\\instances\\default] not exists");
		}
 	}
}