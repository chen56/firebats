package firebats.app;

import static firebats.nio.Paths2.path;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.io.Files;

import firebats.component.Component;
import firebats.component.ComponentContainer;
import firebats.component.IComponent;
import firebats.component.IComponentProvider;
import firebats.component.funcs.IStart;
import firebats.config.Config;
import firebats.internal.app.AppConfig;
import firebats.internal.app.AppProcess;
import firebats.json.Jackson;
import firebats.net.InterfaceAddresses;
/**use App.newRuntime
 * 
 * config 优先级：java > instance > stage
 */
public class App implements IComponentProvider {
	public static final String FILE_APP_CONF = "app.conf";
	public static final String FILE_APP_PID = "app.pid";
	public static final String DEFALT_INSTANCE = "default";

	private ClassLoader classLoader;
	private AppConfig config;
	private static Logger log=LoggerFactory.getLogger(App.class.getName());
    private ComponentContainer container=Component.newContainer(this);
    private CountDownLatch stop=new CountDownLatch(1);
	///////////////////////////////////////////////////////////////
    //factory  
	//////////////////////////////////////////////////////////////

	private App(AppSpecBuilder builder) {
		this.config=builder.getAppConfig();

		if(Strings.isNullOrEmpty(this.config.getInstance())){
	        this.config.instance(DEFALT_INSTANCE);
			log.info("instance not set, use [{}]",DEFALT_INSTANCE);
		}

		//先合并instance config
        Preconditions.checkArgument(getStageDir().toFile().exists(),"instance[%s] dir[%s] not exists",getStage(),getStageDir());
        Preconditions.checkArgument(getInstanceAppConfFile().toFile().exists(),"instance[%s] conf[%s] not exists",getInstance(),getInstanceAppConfFile());
        this.config.getConfig().addFromFile(getInstanceAppConfFile().toString(), getInstanceAppConfFile());
 
        //先合并stage config
        Preconditions.checkArgument(getStageDir().toFile().exists(),"stage[%s] dir[%s] not exists",getStage(),getStageDir());
        Preconditions.checkArgument(getInstanceAppConfFile().toFile().exists(),"stage[%s] conf[%s] not exists",getInstance(),getStageAppConfFile());
        this.config.getConfig().addFromFile(getStageAppConfFile().toString(), getStageAppConfFile());
        
        
		if(Strings.isNullOrEmpty(this.config.getHost())){
			Optional<InetAddress> guessIp = InterfaceAddresses.guessIpAddress();
			if(!guessIp.isPresent()){
				log.error("app.host not find, and can not guess host , stop run!!!!!!!!!!!!!!!");
				throw new RuntimeException("app.host not find, and can not guess host , stop run!!!!!!!!!!!!!!!");
			}
	        this.config.host(guessIp.get().getHostAddress());
		}
		log.info("use host ",this.config.getHost());

		if(Strings.isNullOrEmpty(this.config.getPid())){
	        this.config.pid(guessPid());
			log.debug("app.pid="+this.config.getPid());
		}
        Preconditions.checkArgument(!Strings.isNullOrEmpty(this.config.getInstance())); 
        
        container.add(Component.newComponent("process").onStart(new IStart() {
			@Override public void start() throws Exception {
				writeAppRuntimeInfo();
			}
		}));
	}

	public static AppSpecBuilder builder(Path appHome, String stage) {
		return new AppSpecBuilder(appHome,stage);
	}
	public static Optional<AppProcess> connect(Path appHome) {
 		return AppProcess.connect(appHome);
	}

	
	///////////////////////////////////////////////////////////////
    //factory  
	//////////////////////////////////////////////////////////////
	public static Path getPidFile(Path home) {
		return home.resolve(FILE_APP_PID);
	}

	
	///////////////////////////////////////////////////////////////
    //关键方法  
	//////////////////////////////////////////////////////////////

	public void add(IComponent child) {
		container.add(child);
	}
	public void add(IComponentProvider child) {
		add(child.getComponent());
	}

	/**先用主配置初始化后，再load指定的 配置,即：在指定配置中可以使用主配置的元素*/
	public Config loadConfig(Path filePath) {
		Config result=Config.newEmpty();
		//合并主配置
		result.add(FILE_APP_CONF, config.getConfig());

		//合并要load的配置
		result.addFromFile(filePath.toFile().getName(), filePath);
		return result;
	}

	@Override public IComponent getComponent() {
 		return container;
	}

    public void start(){
    	container.start();
    }
    
    public void stop(){
    	container.stop();
    	stop.countDown();
    }

	public void startAndWait() {
		start();
		try {
			stop.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	///////////////////////////////////////////////////////////////
    //common override 
	//////////////////////////////////////////////////////////////
    @Override
    public final boolean equals(Object other) {
		if(this==other)return true;
		if(!(other instanceof App)) return false;
		if(!canEqual(other)) return false;
		App that = (App) other;
		return Objects.equal(this.getPid(),that.getPid());
    }
	public boolean canEqual(Object other) {
		return (other instanceof App);
	}
	
	@Override
	public String toString() {
		return "pid["+getPid()+"] at "+getHome();
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(this.getPid());
	}

	///////////////////////////////////////////////////////////////
    //query 
	//////////////////////////////////////////////////////////////
	public ClassLoader getClassLoader() {
		return classLoader;
	}
	
	public String getHost() {
		return config.getHost();
	}
	public String getName() {
		return config.getName();
	}
	public String getStage() {
		return config.getStage();
	}
	public String getInstance() {
		return this.config.getInstance();
	}
	public Config getConfig() {
		return config.getConfig();
	}
	public AppConfig getAppConfig() {
		return config;
	}
	public String getPid() {
		return this.config.getPid();
	}
	public Path getHome(){
		return this.config.getHome();
	}
	public Path getRootHome(){
		return this.config.getHome().getParent();
	}
    public String toJsonString(){
    	return Jackson.pretty().encode(this);
    }	
	public Path getPidFile() {
		return getHome().resolve(FILE_APP_PID);
	}
	private Path getRuntimeConfigFile() {
		return getHome().resolve(FILE_APP_CONF);
	}
	public Path getResourcesDir() {
		return getHome().resolve(path("resources"));
	}
	public Path getStageDir() {
		return getResourcesDir().resolve(path("stages",getStage()));
	}
	public Path getStageAppConfFile() {
		return getStageDir().resolve(FILE_APP_CONF);
	}
	public Path getInstanceDir() {
		return getResourcesDir().resolve(path("instances",getInstance()));
	}
	public Path getInstanceAppConfFile() {
		return getInstanceDir().resolve(FILE_APP_CONF);
	}
	///////////////////////////////////////////////////////////////
	//private helper
	//////////////////////////////////////////////////////////////
	private void ensureDir(Path path) {
		File parent = path.toFile().getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}
	}
	/**
	 * 把runtime信息写入到文件.
	 */
	public void writeAppRuntimeInfo() {
		Path pidFile=getPidFile();
		ensureDir(pidFile);
		try {
			Files.write(this.getPid(), pidFile.toFile(), Charsets.UTF_8);
		} catch (IOException e) {
			Throwables.propagate(e);
		}
		pidFile.toFile().deleteOnExit();
		
		Path config=getRuntimeConfigFile();
		getAppConfig().getConfig().writeToPropertiesFile(config);
		config.toFile().deleteOnExit();
        log.debug("write app.home: "+getHome());
        log.debug("write app.pid: "+pidFile);
        log.debug("write app.conf: "+config);
 	}


	private static String guessPid() {
		try {
			String jvmName = ManagementFactory.getRuntimeMXBean().getName();
			return jvmName.split("@")[0];
		}
		catch (Throwable ex) {
			Throwables.propagate(ex);
		}
		//not here
		return null;
	}
    public static Optional<Inet4Address> guessIpAddress(){
        try {
			for (NetworkInterface netint : Collections.list(NetworkInterface.getNetworkInterfaces())){
				if(!netint.isUp())continue;
				if(netint.isLoopback())continue;
				if(netint.isVirtual())continue;
				if(netint.isPointToPoint())continue;
				
			    for (InetAddress inetAddress : Collections.list(netint.getInetAddresses())) {
					if(!(inetAddress instanceof Inet4Address)) continue;
					Inet4Address net4=(Inet4Address)inetAddress;
					if(net4.isLoopbackAddress())continue;
					if(net4.isLinkLocalAddress())continue;
					if(net4.isAnyLocalAddress())continue;
					if(net4.isMulticastAddress())continue;
					if(!net4.isSiteLocalAddress())continue;
					return Optional.of(net4);
				}
			}
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
        return Optional.absent();
    }
    
	///////////////////////////////////////////////////////////////
	//other class
	//////////////////////////////////////////////////////////////

	public static class AppSpecBuilder{
		private ClassLoader classLoader=Thread.currentThread().getContextClassLoader(); 
		private AppConfig config;
		/*internal*/ AppSpecBuilder(Path home, String stage) {
			Preconditions.checkNotNull(home, "home should not be null");
			this.config=AppConfig.newConfig(home,stage);
		}
		
	    /*internal*/ AppConfig getAppConfig() {
			return config;
		}
		public App build(){
	    	App result = new App(this);
	    	return result;
	    }
	    public AppSpecBuilder classloaders(ClassLoader classLoader){
	    	this.classLoader=classLoader;
			return this;
	    }
		public ClassLoader getClassLoader() {
			return classLoader;
		}
	    public AppSpecBuilder name(String appName) {
			this.config.name(appName);
			return  this;
		}
		public AppSpecBuilder host(String host) {
			this.config.host(host);
			return  this;
		}
		public AppSpecBuilder instance(String instance) {
			this.config.instance(instance);
			return this;
		}
	}
}