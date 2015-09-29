package firebats.http.server.exts.file;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import th.api.common.Ws;
import th.api.common.Ws.DefaultWsErrorHandler.HttpClientErrorException;
import th.api.common.Ws.WsRequest;
import firebats.http.server.FireHttpServer;
import firebats.http.server.exts.debug.DebugExt;
import firebats.http.server.exts.files.FilesExt;

public class FilesTest {
	private static final String HTTP_LOCALHOST_25001 = "http://localhost:25001/";
	private FireHttpServer s = FireHttpServer.create(25001);
	private Ws ws = new Ws();

	@Before
	public void before() {
		s.start();
	}

	@After
	public void after() {
		s.stop();
		ws.close();
	}
  
    @Test
    public void dir(){
    	System.out.println(new File(Paths.get("./build.gradle").toUri()).isFile());
     	s.chain()
    	 .link(DebugExt.newEnableDebug())
    	 .link(new FilesExt("/a","./"));
		assertTrue(request().addPath("/a/build.gradle").get().getString().contains("sourceCompatibility"));
    }
 	
    @Test
    public void files_and_dir(){
    	System.out.println(new File(Paths.get("./build.gradle").toUri()).isFile());
     	s.chain()
    	 .link(DebugExt.newEnableDebug())
   	     .link(new FilesExt("/b/build","./"))
   	     .link(new FilesExt("/a/b","./build.gradle"))
   	     ;
		assertTrue(request().addPath("/b/build.gradle").get().getString().contains("sourceCompatibility"));
		assertTrue(request().addPath("/a/b").get().getString().contains("sourceCompatibility"));
     }
 	
    @Test
    public void files_root_to_afile(){
    	System.out.println(new File(Paths.get("./build.gradle").toUri()).isFile());
     	s.chain()
    	 .link(DebugExt.newEnableDebug())
   	     .link(new FilesExt("/","./build.gradle"))
   	     ;
		assertTrue(request().addPath("/").get().getString().contains("sourceCompatibility"));
    }
 	
    @Test
    public void when_pass_parameters_then_ok(){
    	System.out.println(new File(Paths.get("./build.gradle").toUri()).isFile());
     	s.chain()
    	 .link(DebugExt.newEnableDebug())
   	     .link(new FilesExt("/x/y/z","./build.gradle"))
   	     ;
		assertTrue(request().addPath("/x/y/z").addParameter("a","b").get().getString().contains("sourceCompatibility"));
    }
    @Test
    public void when_pass_中文路径_then_ok(){
    	System.out.println(new File(Paths.get("./build.gradle").toUri()).isFile());
     	s.chain()
    	 .link(DebugExt.newEnableDebug())
   	     .link(new FilesExt("/x/y/中文","./build.gradle"))
   	     ;
     	System.out.println("xxxxxxxx "+request().addPath("/x/y/中文").addParameter("a","b").get().getString());
		assertTrue(request().addPath("/x/y/中文").addParameter("a","b").get().getString().contains("sourceCompatibility"));
    }
    
    @Test
    public void when_非法路径字符_then_notfound(){
    	System.out.println(new File(Paths.get("./build.gradle").toUri()).isFile());
     	s.chain()
    	 .link(DebugExt.newEnableDebug())
   	     .link(new FilesExt("/x/y/z","./build.gradle"))
   	     ;
     	try {
     		//问号是非法字符
     		request().addPath("/x/y/z?a=b").addParameter("a","b").get();
            fail();
		} catch (HttpClientErrorException e) {
			assertEquals("404 Not Found",e.getMessage());
		}
    }

	private WsRequest request() {
		return ws.newRequest(HTTP_LOCALHOST_25001);
	}

}
