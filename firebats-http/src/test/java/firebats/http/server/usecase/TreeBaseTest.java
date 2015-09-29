package firebats.http.server.usecase;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import rx.Observable;
import rx.functions.Action0;
import th.api.common.Ws;
import th.api.common.Ws.WsRequest;
import firebats.http.server.Chain;
import firebats.http.server.Context;
import firebats.http.server.FireHttpServer;
import firebats.http.server.Ext;
import firebats.http.server.IRequestHandler;
import firebats.http.server.exts.form.Form;
import firebats.http.server.exts.form.FormExt;
import firebats.http.server.exts.route.RouteExt;
import firebats.http.server.exts.tree.TreeExt;

public class TreeBaseTest {
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
	public void tree() {
		s.chain()
		  .link(new TreeExt(){{
 			 root().child(new FormExt()).route(new RouteExt<Context>(){{
				get("/hi2", new IRequestHandler<Context>() {
					@Override
					public Observable<?> handle(Context ctx) {
						Form form=FormExt.get(ctx);
						return ctx.getResponse().writeStringAndFlush("hi2 " + form.getAttr("a"));
					}
				});
		     }});
 			root().child(new TestUse_StatExt()).route(new RouteExt<Context>(){{
				get("/hi3", new IRequestHandler<Context>() {
					@Override
					public Observable<?> handle(Context ctx) {
						return ctx.getResponse().writeStringAndFlush("hi3 " + ctx.getRequest().getQueryParameters().get("a").get(0));
					}
				});
		     }});
 		  }});
		assertEquals("hi2 aa", request().addPath("/hi2").addParameter("a", "aa").get().getString());
		assertEquals("hi3 aa", request().addPath("/hi3").addParameter("a", "aa").get().getString());
	}

	static class TestUse_StatExt implements Ext<Context, Context> {
		@Override
		public Observable<?> call(Chain<Context,Context> chain,final Context ctx) {
			long start=System.nanoTime();
			//start
			return chain.next(ctx).doOnCompleted(new Action0(){
 				@Override
				public void call() {
 					long ns = System.nanoTime()-start;
					System.out.println(ctx.getRequest().getPath()+" 花费时间"+ns+" ns");
 				}
			});
		}
	}
	private WsRequest request() {
		return ws.newRequest("http://localhost:25001/");
	}

}
