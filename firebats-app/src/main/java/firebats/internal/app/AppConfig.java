package firebats.internal.app;

import java.nio.file.Path;

import firebats.app.App;
import firebats.config.Config;
import firebats.nio.Paths2;
import firebats.properties.Property;
import firebats.properties.PropertyFactory;
//TODO MOVE TO public api App.AppConfig
public class AppConfig{
 		protected final PropertyFactory factory=PropertyFactory.ofNoInit();
		/*internal*/ Config config;
		private Property<Void,Path>   home=              factory.of("app.home").withResult(Paths2.String2Path());
		private Property<Void,String> name=              factory.of("app.name");
		private Property<Void,String> stage=             factory.of("app.stage");
		private Property<Void,String> pid=               factory.of("app.pid");
		private Property<Void,String> instance=          factory.of("app.instance").withDefault(App.DEFALT_INSTANCE); 
		private Property<Void,String> host=              factory.of("app.host");
		protected AppConfig(Config config){
			this.config=config;
			this.factory.init(config);
		}
		public AppConfig(Path appHome, String appStage, String instance) {
			this.config=Config.newEmpty();
			this.factory.init(config);
            this.home.setString(appHome.toString());
            this.stage.setString(appStage.toString());
            this.instance.setString(instance.toString());
		}
		public static AppConfig from(Config config) {
			AppConfig result=new AppConfig(config);
			return result;
		}
		public static AppConfig newConfig(Path home, String stage) {
			AppConfig result=new AppConfig(Config.newEmpty());
			result.home.setString(home.toString());
			result.stage.setString(stage.toString());
			return result;
		}

		public String getHost() {
 			return host.get();
		}
		public Config getConfig() {
			return config;
		}
		public Path getHome() {
			return home.get();
		}
		public String getName() {
			return name.get();
		}
		public String getStage() {
			return stage.get();
		}
		public String getInstance() {
			return instance.get();
		}
		public String getPid() {
			return pid.get();
		}
		public AppConfig name(String appName) {
			this.name.setString(appName);
			return  this;
		}
		public AppConfig pid(String pid) {
			this.pid.setString(pid);
			return  this;
		}
		public AppConfig host(String host) {
			this.host.setString(host);
			return  this;
		}
		public AppConfig instance(String instance) {
			this.instance.setString(instance);
			return  this;
		}
		public AppConfig stage(String instance) {
			this.instance.setString(instance);
			return  this;
		}
	}