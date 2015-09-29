package firebats.http.server.exts.form;

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

public class FormTest {
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
    public static class A{
    	public String a;

		public String getA() {
			return a;
		}

		public void setA(String a) {
			this.a = a;
		}
    }
    @Test
    public void form(){
    	s.chain()
//    	 .link(new DebugExt())
    	 .link(new FormExt())
		 .link(new RouteExt<Context>() {{
				get("/hi2", new IRequestHandler<Context>() {
					@Override
					public Observable<?> handle(Context ctx) {
						try {
							Form form = FormExt.get(ctx);
							A a=form.as(A.class);
//						    return	ctx.getResponse().writeStringAndFlush("hi2 " + form.getAttr("a")+" " +a.a);
						    return	ctx.getResponse().writeStringAndFlush("hi2 " + form.getAttr("a")+" "+a.a);
//							return Observable.just(1);

						} catch (Exception e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						}
					}
				});
		   }}) ;
		assertEquals("hi2 aa aa", request().addPath("/hi2").addParameter("a", "aa").get().getString());
    }
 	
	private WsRequest request() {
		return ws.newRequest(HTTP_LOCALHOST_25001);
	}

}
