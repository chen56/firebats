package firebats.http.server.exts.session;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import firebats.http.server.exts.session.HttpSession;
import firebats.http.server.exts.session.HttpSession.ExpireMode;
import firebats.http.server.exts.session.HttpSession.HttpSessionBuilder;
import firebats.json.Jackson;

public class SessionTest {
	static String SecretKey="wVcvIJpPLLW4UuKjlE5prGLrlZDrzPyRQfXg5hlBOYwtVFwQSkInEXwKw4OPiRFW";
	
     @Test
    public void newCookie() throws UnsupportedEncodingException {
         HttpSession session = sessionBuilder().buildNewEmptySession();
         assertEquals(false,session.isChanged());
         session.put("uid", "111");
         assertEquals(true,session.isChanged());
         assertEquals("806d845e75fa183d0bea93d4ad78a259eac6def4-%00uid%3A111%00",session.toCookie().getValue());
         assertEquals("PLAY_SESSION",session.toCookie().getName());
         
         HttpSession loaded = sessionBuilder().buildFromCookie(session.toCookie());
         assertEquals(false,loaded.isChanged());
         loaded.put("uid", "222");
         assertEquals(true,loaded.isChanged());
    }
    
     @Test
    public void maxAge() throws UnsupportedEncodingException {
    	 HttpSessionBuilder b = sessionBuilder().testUseCurrentTimeMillis(1000).setExpireMode(ExpireMode.CookieAndSession, /*maxAge*/1L,TimeUnit.SECONDS);
         HttpSession session = b.buildNewEmptySession();
         assertEquals(false,session.isChanged());
         session.put("uid", "111");
         assertEquals(true,session.isChanged());
         assertEquals("PLAY_SESSION=d22354823ec1cafb3ed9f2707301565246a284ce-%00___TS%3A2000%00%00uid%3A111%00, path=/, maxAge=1s",session.toCookie().toString());
         assertEquals(1,session.toCookie().getMaxAge());
         
         //没有超时
         HttpSession loaded = b.buildFromCookie(session.toCookie());
         assertEquals("PLAY_SESSION=d22354823ec1cafb3ed9f2707301565246a284ce-%00___TS%3A2000%00%00uid%3A111%00, path=/, maxAge=1s",loaded.toCookie().toString());

         assertEquals(1000,loaded.getMaxAgeMillis());
         loaded.put("uid", "222");
         //设置了ExpireMode.CookieAndSession 每次都是更新状态
         assertEquals(true,loaded.isChanged());
         
         HttpSession expired=  b.testUseCurrentTimeMillis(3000).buildFromCookie(loaded.toCookie());
         assertEquals("PLAY_SESSION=35a63c46e2ca2ed123fd00d77f7973caa5d0b2a3-%00___TS%3A4000%00, path=/, maxAge=1s",expired.toCookie().toString());
         
         System.out.println(Jackson.pretty().encode(expired.toCookie()));
    }
    

     @Test
    public void readPlaySession() throws UnsupportedEncodingException {
         String cookieStr="PLAY_FLASH=; PLAY_ERRORS=; PLAY_SESSION=1d93ceb48e0a590cc74bdc15ab414f172908ee6f-%00aid%3A50c592bc6a4340abce4cc858%00";
         HttpSession session = sessionBuilder().buildFromCookiesHeader(cookieStr);
         assertEquals("50c592bc6a4340abce4cc858",session.get("aid"));
    }

 	private HttpSessionBuilder sessionBuilder() {
		return HttpSession.builder(SecretKey,"PLAY_SESSION");
	}

}