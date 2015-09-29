package firebats.app.launch;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import firebats.app.App;
import firebats.component.IComponent;
import firebats.internal.app.AppProcess;
import firebats.json.Jackson;

public class AppMain{
	private JCommandMain mainCmd = new JCommandMain();
	private StartJCommand startCmd=new StartJCommand();
	private StopJCommand stopCmd=new StopJCommand();
    private JCommander jcommand=new JCommander(mainCmd);
	private static Logger log=LoggerFactory.getLogger(AppMain.class.getName());

	private IAppConfigurer appProvider; 
    { 			
    	jcommand.addCommand("start",startCmd);
	    jcommand.addCommand("stop",stopCmd);
    }
	private AppMain(IAppConfigurer appProvider) {
		this.appProvider=appProvider;
	}

	public static void main(String[] args,IAppConfigurer appProvider) throws IOException {
		try {
			new AppMain(appProvider).run(args);
		} catch (InterruptedException e) {
			System.err.println("app was Interrupted "+e.getMessage());
			e.printStackTrace();
		}
	}
	
    public interface IAppConfigurer{
    	IComponent configure(App app);
    	default void start(App app){
     		app.startAndWait();
    	}
    }
	private void run(String[] args) throws InterruptedException {
		try {
			jcommand.parse(args);
		} catch (ParameterException e) {
			System.out.println("commond error: "+e.getMessage());
			return;
		}
		if(mainCmd.help||jcommand.getParsedCommand()==null){
			if(Strings.isNullOrEmpty(jcommand.getParsedCommand())){
				jcommand.usage();
 			}else{
				jcommand.usage(jcommand.getParsedCommand());
 			}
		}else{
			if(mainCmd.debug){
				info(cmdDebugInfo(args).toString());
			}
			
			Path appHome = AppMain.getAppHome();
			String cmd=jcommand.getParsedCommand();
			if(Objects.equal("start",cmd)){
	    		App app = App
	    				.builder(appHome, startCmd.stage)
	    				.instance(startCmd.instance)
	    				.build();
	    		app.add(appProvider.configure(app));
				appProvider.start(app);
				return;
			}
			if(Objects.equal("stop",cmd)){
				stop(appHome);
				return;
			}
		}
	}

	private static void stop(Path appHome) {
		Optional<AppProcess> current=App.connect(appHome);
		if(!current.isPresent()){
			error("app(%s) not start, can not stop",appHome);
			System.exit(-1);
		}else{
			System.out.println(current.get().killProcess()); 
			System.exit(0);
		}
	}

	private StringBuilder cmdDebugInfo(String[] args){
		StringBuilder sb=new StringBuilder();
		sb.append("\n");
		sb.append("************************************************************************\n");
		sb.append("args: "+Lists.newArrayList(args));
		sb.append("\n");
		sb.append("\nmain-cmd: ");
		sb.append(Jackson.pretty().encode(mainCmd));
		sb.append("\nstart-cmd: ");
		sb.append(Jackson.pretty().encode(startCmd));
		sb.append("\nstop-cmd: ");
		sb.append(Jackson.pretty().encode(stopCmd));
		sb.append("\n");
		sb.append("system user.dir ["+System.getProperty("user.dir")+"] 不用此目录");
		sb.append("\n");
		sb.append("*************************************************************************\n");
        return sb;
	}
	
    public static Path getAppHome() {
		String APP_HOME=System.getenv("APP_HOME");
		Path appHome=null;
		if(APP_HOME!=null){
			appHome=path(APP_HOME);
			info("env APP_HOME is not set, use java System.getProperty('user.dir') : %s",appHome);
		}else{
			appHome=path(System.getProperty("user.dir"));
			info("env APP_HOME : %s",appHome);
		}
		return appHome;
	}

	public static Path path(String first,String ... more){
    	return FileSystems.getDefault().getPath(first,more);
    }
    public static void info(String format,Object ... args){
    	log.info("INFO  - App - "+String.format(format, args));
    }
    private static void error(String format,Object ... args){
    	log.error("ERROR - App - "+String.format(format, args));
    }
    public static class JCommandMain {
		@Parameter(names = {"-debug","--debug"}, description = "Debug jcommand info",help=true) 
		public boolean debug=false;
		@Parameter(names = {"-h","--help"}, description = "Debug mode",help=true) 
		public boolean help = false;
	}

    @Parameters(separators = " ",commandDescriptionKey="start", commandDescription = "start app")
    public static class StartJCommand{
		@Parameter(names = {"-s","-stage","--stage"},required=true, description = "可选stage") 
		public String stage;

		@Parameter(names = {"-i","-instance","--instance"}, description = "deploy default is 'default'",help=true) 
		public String instance=App.DEFALT_INSTANCE;
    }
    
    @Parameters(separators = " ",commandDescriptionKey="stop", commandDescription = "stop app")
    public static class StopJCommand{
    	
    }
}