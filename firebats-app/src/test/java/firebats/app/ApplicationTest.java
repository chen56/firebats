package firebats.app;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import firebats.app.App.AppSpecBuilder;
import firebats.component.Component;

public class ApplicationTest {
	String log = "";
	static String TestStage="test";
    String AppName=this.getClass().getName();
	@Before
	public void setup(){
		log="";
	}
	@After
	public void teardown(){
	}

	@Test
	public void name_2个app可使用同样的名称() {
		//given app with name
 		appBuilder().build();
 		appBuilder().build();
	}

	@Test
	public void 生命期方法() {
		//given app with TestablePlugin
		App app = appBuilder().build();
		app.add(Component.newComponent("").onStart(()->{log("start");}).onStop(()->{log("stop");}));
		
		//when start stop
		app.start();
		app.stop();

		//then
		assertThat(log).isEqualTo("configure;start;stop;");
	}
	

	
	private AppSpecBuilder appBuilder() {
		return App.builder(new File(".").toPath(), TestStage);
	}
	private void log(String string) {
         log+=string+";";		
	}
}