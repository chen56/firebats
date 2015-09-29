package firebats.http.server.exts.route;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rx.Observable;
import th.api.common.Ws;
import th.api.common.Ws.WsRequest;
import firebats.http.server.Context;
import firebats.http.server.FireHttpServer;
import firebats.http.server.IRequestHandler;
import firebats.http.server.exts.form.Form;
import firebats.http.server.exts.form.FormExt;
import firebats.http.server.exts.route.RouteExt;

public class RouteTest {
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
    public void form(){
    	s.chain()
		 .link(new RouteExt<Context>() {{
				get("/a", new IRequestHandler<Context>() {
					@Override
					public Observable<?> handle(Context ctx) {
					    return	ctx.getResponse().writeStringAndFlush("get "+ctx.getRequest().getPath());
					}
				});
		   }}) ;
		assertEquals("get /a", request().addPath("/a").get().getString());
		assertEquals("get /a", request().addPath("/a/").get().getString());
		assertEquals("get /a/b", request().addPath("/a/b").get().getString());
		
		//只按子路径匹配，/ab 不是 /a 的子路径
		assertEquals("", request().addPath("/ab").get().getString());
    }
 	
	private WsRequest request() {
		return ws.newRequest(HTTP_LOCALHOST_25001);
	}

}
