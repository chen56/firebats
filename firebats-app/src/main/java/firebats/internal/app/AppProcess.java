package firebats.internal.app;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;

import firebats.app.App;


public class AppProcess {
	private static Logger log=LoggerFactory.getLogger(AppProcess.class.getName());

    transient String pid;

	private Path appHome;

	///////////////////////////////////////////////////////////////
    //create 
	//////////////////////////////////////////////////////////////
	private AppProcess(Path appHome, String pid){
		this.appHome=appHome;
        this.pid=pid;
	}
	public static Optional<AppProcess> connect(Path appHome) {
		Preconditions.checkNotNull(appHome, "appHome should not be null");
		File pidFile=App.getPidFile(appHome).toFile();
		if(!pidFile.exists()) return  Optional.absent();
		Preconditions.checkState(pidFile.exists(),"%s not exists, app not start.",pidFile);
 		try {
			return Optional.of(new AppProcess(appHome,Files.toString(pidFile, Charsets.UTF_8)));
		} catch (IOException e) {
			Throwables.propagate(e);
		}
 		//not here
 		return Optional.absent();
	}

	///////////////////////////////////////////////////////////////
    //cmd 
	//////////////////////////////////////////////////////////////
	public String killProcess(){
		return killProcess(getPid());
	}
	
	/*internal*/ static String killProcess(String pid){
		try {
			String cmd=null;
			if(isWindows()){
				//windows上一般用来测试，使用/f强制执行即可
				cmd=String.format("taskkill /pid %s /t /f", pid);
			}else if(isLinux()){
				cmd=String.format("kill -15 %s", pid);
			}else{
				Preconditions.checkState(false,"sorry,kill cmd not support for Os(%s)  ",osName());
			}
			log.info(cmd);
			Process p=Runtime.getRuntime().exec(cmd);
			int exit = p.waitFor();
			try(InputStreamReader x=new InputStreamReader(p.getInputStream(),Charsets.UTF_8)){
				String returnString = CharStreams.toString(x);
				return String.format("exit %s \n %s", exit,returnString);
			}
		} catch (Exception e) {
			Throwables.propagate(e);
		}
		//not here
		return null;
	}
	
	///////////////////////////////////////////////////////////////
    //query 
	//////////////////////////////////////////////////////////////
	public String getPid() {
		return pid;
	}
	public Path getAppHome(){
		return appHome;
	}
	private static boolean isWindows() {
		String os=osName();
		return os.indexOf("windows") != -1 || os.indexOf("nt") != -1;
	}
	private static String osName() {
		return System.getProperty("os.name").toLowerCase();
	}
	@SuppressWarnings("unused") private static boolean isMac() {
		return osName().indexOf("mac") != -1;
	}
	private static boolean isLinux() {
		return osName().indexOf("linux") != -1;
	}
}
