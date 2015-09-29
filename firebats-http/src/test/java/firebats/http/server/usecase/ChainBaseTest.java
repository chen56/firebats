package firebats.http.server.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import io.netty.buffer.ByteBuf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;
import th.api.common.Ws;
import th.api.common.Ws.WsRequest;
import firebats.http.server.Chain;
import firebats.http.server.Context;
import firebats.http.server.FireHttpServer;
import firebats.http.server.Ext;
import firebats.http.server.IRequestHandler;
import firebats.http.server.exts.form.Form;
import firebats.http.server.exts.route.RouteExt;

public class ChainBaseTest {
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
	public void 重复链接导致错误() {
		Chain<Context, Context> root = s.chain();
		root.link(new TestUseMyFormExt());
		try {
			root.link(new TestUseMyFormExt());
			fail();
		} catch (Exception e) {
			assertEquals("already has next:MyFormExt", e.getMessage());
		}
	}

	@Test
	public void chain基本用法() {
		s.chain()
		 .link(new TestUseStatExt())
		 .link(new TestUseMyFormExt())
		 .link(new RouteExt<MyFormContext>() {{
				get("/hi2", new IRequestHandler<MyFormContext>() {
					@Override
					public Observable<?> handle(MyFormContext ctx) {
						return ctx.getResponse().writeStringAndFlush("hi2 " + ctx.form.getAttr("a"));
					}
				});
		   }})
		 .link(new RouteExt<MyFormContext>() {{
				get("/hi3", new IRequestHandler<MyFormContext>() {
					@Override
					public Observable<?> handle(MyFormContext ctx) {
						return ctx.getResponse().writeStringAndFlush("hi3 " + ctx.form.getAttr("a"));
					}
				});
		 }});
		assertEquals("hi2 aa", request().addPath("/hi2").addParameter("a", "aa").get().getString());
		//TODO how to do this case,not found是否应让最后一个chain处理？
		assertEquals("", request().addPath("/hi3").addParameter("a", "aa").get().getString());
	}
	

  	static class TestUseMyFormExt implements Ext<Context, MyFormContext> {
		@Override
		public Observable<?> call(Chain<Context,MyFormContext> chain,final Context ctx) {
			return ctx.getRequest().getContent()
					.flatMap(new Func1<ByteBuf, Observable<?>>() {
						@Override
						public Observable<?> call(ByteBuf content) {
							return chain.next(new MyFormContext(ctx, Form.decode(ctx, content)));
						}
			});
		}

		public String toString() {
			return "MyFormExt";
		}
	} 
	static class TestUseStatExt implements Ext<Context, Context> {
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

	public static class MyFormContext extends Context {
		private Form form;

		public MyFormContext(Context ctx, Form form) {
			super(ctx);
			this.form = form;
		}

		public MyFormContext(MyFormContext ctx) {
			super(ctx);
			this.form = ctx.form;
		}
	}

	private WsRequest request() {
		return ws.newRequest("http://localhost:25001/");
	}

}
