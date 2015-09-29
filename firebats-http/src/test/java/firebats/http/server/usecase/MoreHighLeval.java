package firebats.http.server.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rx.Observable;
import th.api.common.Ws;
import th.api.common.Ws.DefaultWsErrorHandler.HttpClientErrorException;
import th.api.common.Ws.WsRequest;
import copy.org.springframework.http.HttpStatus;
import firebats.http.server.Chain;
import firebats.http.server.Context;
import firebats.http.server.Ext;
import firebats.http.server.FireHttpServer;
import firebats.http.server.IRequestHandler;
import firebats.http.server.exts.route.RouteExt;
import firebats.http.server.results.redirect.RedirectResultAware;
import firebats.http.server.results.string.StringResultAware;

public class MoreHighLeval {
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
						return ctx.string("hi").status400BadRequest().render();
					}
				});
				get("/redirectToHi", new IRequestHandler<TestContext>() {
					@Override
					public Object handle(TestContext ctx) {
						return ctx.redirect("/hi").status303SeeOther().render();
					}
				});
		   }});
 		try {
			request().addPath("/redirectToHi").get();
			fail();
		} catch (HttpClientErrorException e) {
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
 		}
	}
	
	
	private WsRequest request() {
		return ws.newRequest("http://localhost:25001/");
	}


}
