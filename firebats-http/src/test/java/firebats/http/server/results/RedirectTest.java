package firebats.http.server.results;

import static org.junit.Assert.assertEquals;
import io.netty.handler.codec.http.HttpResponseStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rx.Observable;
import th.api.common.Ws;
import th.api.common.Ws.WsRequest;
import firebats.http.server.Chain;
import firebats.http.server.Context;
import firebats.http.server.Ext;
import firebats.http.server.FireHttpServer;
import firebats.http.server.IRequestHandler;
import firebats.http.server.exts.route.RouteExt;
import firebats.http.server.results.redirect.RedirectResultAware;
import firebats.http.server.results.string.StringResultAware;

public class RedirectTest {
	private static final String HTTP_LOCALHOST_25001 = "http://localhost:25001/";
	FireHttpServer s = FireHttpServer.create(25001);
	Ws ws = new Ws();

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
	public void redirectProvider() {
		class TestContext extends Context implements RedirectResultAware,StringResultAware{
 			protected TestContext(Context context) {
				super(context);
 			}
 		}
		class TestExt implements Ext<Context,TestContext>{
			@Override
			public Observable<?> call(Chain<Context, TestContext> chain,Context ctx) {
				return chain.next(new TestContext(ctx));
			} 		
		}
		s.chain()
		  .link(new TestExt())
		  .link(new RouteExt<TestContext>() {{
				get("/hi", new IRequestHandler<TestContext>() {
					@Override
					public Object handle(TestContext ctx) {
						System.out.println("getPath "+ctx.getRequest().getPath());
						return ctx.string("hi:"+ctx.getRequest().getQueryString()).render();
					}
				});
				get("/redirectToHi", new IRequestHandler<TestContext>() {
					@Override
					public Object handle(TestContext ctx) {
						return ctx.redirect("http://localhost:25001/hi?a=b").render();
					}
				});
		   }});
		assertEquals("hi:a=b", request().addPath("/redirectToHi").get().getString());
 	}
	

	//redirect 原理就是设置300系列转向
	@Test
	public void rawRedirect() {
		s.chain()
 		  .link(new RouteExt<Context>() {{
				get("/hi", new IRequestHandler<Context>() {
					@Override
					public Observable<?> handle(Context ctx) {
						ctx.getResponse().writeString("hi");
						return Observable.empty();
					}
				});
				get("/redirectToHi", new IRequestHandler<Context>() {
					@Override
					public Observable<?> handle(Context ctx) {
						ctx.getResponse().setStatus(HttpResponseStatus.SEE_OTHER);
						ctx.getResponse().getHeaders().setHeader("Location","/hi");
 						ctx.getResponse().writeString("to hi");
						return Observable.empty();
					}
				});
		   }});
		assertEquals("hi", request().addPath("/redirectToHi").get().getString());
	}

	private WsRequest request() {
		return ws.newRequest(HTTP_LOCALHOST_25001);
	}

}
