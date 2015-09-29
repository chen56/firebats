package firebats.http.server;

import static org.junit.Assert.*;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.ServerCookieEncoder;

import org.junit.Test;

/**
 * @author Nitesh Kant
 */
public class CookieTest {
    @Test
    public void testSetCookie() throws Exception {
        DefaultHttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        HttpHeaders headers=nettyResponse.headers();
		headers.add(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(new DefaultCookie("a","a v")));
		headers.add(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(new DefaultCookie("a","a v")));
        assertEquals(2,headers.entries().size());
    }
    @Test
    public void testSetCookie2() throws Exception {
        DefaultHttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        HttpHeaders headers=nettyResponse.headers();
		headers.set(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(new DefaultCookie("a","a v")));
		headers.set(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(new DefaultCookie("a","a v")));
        assertEquals(1,headers.entries().size());
    }
}
