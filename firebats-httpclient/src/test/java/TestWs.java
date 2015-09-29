

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.Ignore;
import org.junit.Test;

import copy.org.eclipse.core.runtime.IProgressMonitor;
import th.api.common.Ws;
import th.api.common.Ws.WsRequest;

public class TestWs {
	static Ws ws = new Ws();

	static WsRequest uri(String uri) {
		return ws.newRequest(uri);
	}

	@Test
	public void testAddPath() {
		assertEquals("http://host:8000/a/b",
				uri("http://host:8000//").addPath("/a/b").getURIWithQuery().toString());

		assertEquals("http://host:8000/a/b/c", uri("http://host:8000/a")
				.addPath("b/c").getURIWithQuery().toString());

		assertEquals("http://host:8000/a/b/c?x=1",
				uri("http://host:8000/a?x=1").addPath("b/c").getURIWithQuery()
						.toString());
	}

	@Test
	public void testEncoding() {
		assertEquals("%E4%B8%AD%E6%96%87", Ws.encodeURL("中文"));
		assertEquals("/a/b?x=中文", uri("/a/b").addParameter("x", "中文").getURIWithQuery().toString());

		// 编码
		assertEquals("/a/b?x=%20", uri("/a/b").addParameter("x", " ").getURIWithQuery()	.toString());

	}

	@Test
	public void testQuery() throws URISyntaxException {
		assertEquals("/a?x=1", uri("/a").addParameter("x", "1").getURIWithQuery()
				.toString());
		
		//多参数
		assertEquals("/a?x=a&x=b", uri("/a").addParameters("x", "a","b").getURIWithQuery()	.toString());

		// 问题参数
		assertEquals("/a", uri("/a?=1").getURIWithQuery().toString());
		assertEquals("/a", uri("/a?=").getURIWithQuery().toString());
		assertEquals("/a?x=", uri("/a?x=").getURIWithQuery().toString());
		assertEquals("/a", uri("/a").addParameter("x",null).getURIWithQuery().toString());
	}
	@Test
	public void 参数() throws URISyntaxException {
		//空字符串
		assertEquals("/a?x=", uri("/a").addParameter("x", "").getURIWithQuery().toString());
		
		assertEquals("/a", uri("/a").addParameter("x", (Object)null).getURIWithQuery().toString());
	}

	@Ignore
	@Test
	public void testUploadFiles(){
		System.out.println(uri("http://127.0.0.1:9000/submitFile")
		.addParameter("title", "a")
		.addParameter("title", "aa")
//		.addFormParameter("a","aa")
//		.addFile("pom",new File("./pom.xml"))
		.addFileParameter("photo",new File("./a.txt"))
		.addFileParameter("photo",new File("./b.txt"))
		.post().getString());
		//对应控制器：
//		public static void submitFile(String[] title,File[] photo) {
//		    System.out.println(Arrays.asList(title));
//		    System.out.println(Arrays.asList(photo));
//		}
		
		
	}
	
	@Test
	public void test1(){
		ws.newRequest("http://www.baidu.com/")
			  .addParameter("xmgdUserId","1")
		     .addFileParameter("photo",new File("test_resources/player_avatar.png"))
		     .get();
	}
	
	@Ignore
	@Test 
	//如果需要的话：可以开发：UriTemplate
	public void test模板功能() {
		assertEquals("a/1", uri("a/{b}").addParameter("b", 1).getURIWithQuery().toString());
		assertEquals("a/1/1", uri("a/{b}/{b}").addParameter("b", 1).getURIWithQuery().toString());
		assertEquals("http://www.honghu.com/x?a=1",	
				uri("http://www.honghu.com/x?a={a}").addParameter("a", 1).getURIWithQuery().toString());
	}
	
    public static class ConsoleMonitor implements IProgressMonitor{
		private int totalWork;
		private boolean canceled; 
        private double worked;
		@Override
		public void beginTask(String name, int totalWork) {
			d(name + ":" + totalWork);
			this.totalWork=totalWork;
		}

		private void d(String string) {
			System.out.println("th.common.TestMonitor "+string);
		}

		@Override
		public void done() {
			d("done");
		}

		@Override
		public void internalWorked(double work) {
			worked+=work;
			d("worked: " + worked / totalWork *100 +"% "  + worked +"/"+ totalWork);
		}

		@Override
		public boolean isCanceled() {
			return canceled;
		}

		@Override
		public void setCanceled(boolean value) {
			this.canceled = value;
		}

		@Override
		public void setTaskName(String name) {
			d("task: " + name);
		}

		@Override
		public void subTask(String name) {
			d("subTask: " + name);
		}

		@Override
		public void worked(int work) {
			internalWorked(work);
		}
    }

}