package firebats.net;


import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Ignore;
import org.junit.Test;

import firebats.net.Uri.UriBuilder;
//TODO 陈鹏 URI2和WsRequest重复
public class TestUri {

	static UriBuilder uriBuilder(String uri) {
		return Uri.buildFrom(uri);
	}
	static Uri uri(String uri) {
		return Uri.parse(uri);
	}
	
	@Test
	public void defaultPort(){
		assertEquals("http://192.168.1.214/a/b",uri("http://192.168.1.214/a/b").toString());
		assertEquals(-1,uri("http://192.168.1.214/a/b").getPort());
		
		assertEquals(80, uri("http://a.com").getPortResolveDefault());
		assertEquals(443, uri("https://a.com").getPortResolveDefault());
		assertEquals(80, uri("ws://a.com").getPortResolveDefault());
		assertEquals(443, uri("wss://a.com").getPortResolveDefault());
		
		assertEquals(2222, uri("http://a.com:2222").getPortResolveDefault());
		assertEquals(2222, uri("https://a.com:2222").getPortResolveDefault());
		assertEquals(2222, uri("ws://a.com:2222").getPortResolveDefault());
		assertEquals(2222, uri("wss://a.com:2222").getPortResolveDefault());

	}

    @Test
    public void studyURI(){
        System.out.println(URI.create("http://www/a/b").resolve(URI.create("_/user/register")));
        System.out.println(URI.create("http://www/a/b").resolve(URI.create("bus:/user/register")));
        System.out.println(URI.create("http://www/a/b").resolve(URI.create("rest:inner/user/register")));

    	printUri(URI.create("file:///c:/user/register"));
    	printUri(URI.create("bus:9090/user/register"));
    	printUri(URI.create("bus:/user/register"));
    	printUri(URI.create("bus:/user/register"));
    	printUri(URI.create("rest:/user/register"));
    	printUri(URI.create("rest:/inner/user/register"));
    	
    	printUri(URI.create("mailto:java-net@java.sun.com/abs/#xxx"));
    	printUri(URI.create("urn:isbn:096139210x/sdfs/s"));
    	printUri(URI.create("http://a:s@b.com:9999/a?a=2#aaaa"));
    }
	private void printUri(URI uri) {
		System.out.println("#########################              "+uri);
		System.out.println("isAbsolute "+uri.isAbsolute());
		System.out.println("isOpaque "+uri.isOpaque());
		System.out.println("getScheme "+uri.getScheme());
		System.out.println("getSchemeSpecificPart "+uri.getSchemeSpecificPart());
    	System.out.println("getAuthority "+uri.getAuthority());
		System.out.println("getUserInfo "+uri.getUserInfo());
    	System.out.println("getHost "+uri.getHost());
    	System.out.println("getPort "+uri.getPort());
    	System.out.println("getPath "+uri.getPath());
    	System.out.println("getFragment "+uri.getFragment());
    	System.out.println("getQuery "+uri.getQuery());
	}
	@Test
	public void testAddPath() {
		assertEquals("http:/a/b",
				uriBuilder("http://host:8081,host2:8000//").addPath("/a/b").build().toURI().toString());

		assertEquals("http://host:8000/a/b/c", uriBuilder("http://host:8000/a")
		.addPath("b/c").build().toURI().toString());

		assertEquals("http://host:8000/a/b/c?x=1",
				uriBuilder("http://host:8000/a?x=1").addPath("b/c").build().toURI()
						.toString());
	}

	@Test
	public void testEncoding() {
		assertEquals("%E4%B8%AD%E6%96%87", Uri.encodeURL("中文"));
		assertEquals("/a/b?x=中文", uriBuilder("/a/b").addParam("x", "中文").build().toURI().toString());

		// 编码
		assertEquals("/a/b?x=%20", uriBuilder("/a/b").addParam("x", " ").build().toURI()	.toString());

	}

	@Test
	public void testQuery() throws URISyntaxException {
		assertEquals("/a?x=1&x=2", uriBuilder("/a").addParam("x", "1").addParam("x","2").build().toURI()
				.toString());
		
		//多参数
		assertEquals("/a?x=a&x=b", uriBuilder("/a").addParams("x", "a","b").build().toURI()	.toString());

		// 问题参数
		assertEquals("/a", uriBuilder("/a?=1").build().toURI().toString());
		assertEquals("/a", uriBuilder("/a?=").build().toURI().toString());
		assertEquals("/a?x=", uriBuilder("/a?x=").build().toURI().toString());
		assertEquals("/a?x=", uriBuilder("/a").addParam("x",null).build().toURI().toString());
	}
	@Test
	public void 参数() throws URISyntaxException {
		//空字符串
		assertEquals("/a?x=", uriBuilder("/a").addParam("x", "").build().toURI().toString());
		
		assertEquals("/a?x=", uriBuilder("/a").addParam("x", (Object)null).build().toURI().toString());
	}

	@Test
	public void testHostPortPathString() throws URISyntaxException{
		assertEquals(-1,new URI("https://a.com/a/b?a=b").getPort());
		assertEquals("a.com/a/b", uri("http://a.com/a/b?a=b").toHostPortPathString());
		assertEquals("a.com:8888/a/b", uri("http://a.com:8888/a/b?a=b").toHostPortPathString());
		assertEquals("a.com/a/b", uri("https://a.com/a/b?a=b").toHostPortPathString());
	}
	
	
	@Ignore
	@Test 
	//如果需要的话：可以开发：UriTemplate
	public void test模板功能() {
		assertEquals("a/1", uriBuilder("a/{b}").addParam("b", 1).build().toURI().toString());
		assertEquals("a/1/1", uriBuilder("a/{b}/{b}").addParam("b", 1).build().toURI().toString());
		assertEquals("http://www.honghu.com/x?a=1",
				uriBuilder("http://www.honghu.com/x?a={a}").addParam("a", 1).build().toURI().toString());
	}
	
}