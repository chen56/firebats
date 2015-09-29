package firebats.http.server.exts.route;

import io.netty.handler.codec.http.HttpMethod;
import rx.Observable;
import firebats.check.Check;
import firebats.http.server.Chain;
import firebats.http.server.Context;
import firebats.http.server.Ext;
import firebats.http.server.IRequestHandler;
import firebats.http.server.exts.route.Routes.Route;

public class RouteExt<TInOut extends Context> implements Ext<TInOut,TInOut> {
	private Routes routes = new Routes();
	public  RouteExt<TInOut> get(String path, IRequestHandler<TInOut> handler) {
		return service(path, handler, HttpMethod.GET);
	}

	public  RouteExt<TInOut> post(String path, IRequestHandler<TInOut> handler) {
		return service(path, handler, HttpMethod.POST);
	}

	public RouteExt<TInOut> service(String path, IRequestHandler<TInOut> handler) {
		return service(path, handler);
	}

	public RouteExt<TInOut> service(HttpMethod method0, String path,
			IRequestHandler<TInOut> handler) {
		return service(path, handler, method0);
	}

	public RouteExt<TInOut> service(HttpMethod method0, HttpMethod method1, String path,
			IRequestHandler<TInOut> handler) {
		return service(path, handler, method0, method1);
	}

	public RouteExt<TInOut> service(HttpMethod method0, HttpMethod method1,
			HttpMethod method2, String path, IRequestHandler<TInOut> handler) {
		return service(path, handler, method0, method1, method2);
	}

	public RouteExt<TInOut> service(HttpMethod method0, HttpMethod method1,
			HttpMethod method2, HttpMethod method3, String path,
			IRequestHandler<TInOut> handler) {
		return service(path, handler, method0, method1, method2, method3);
	}

	public RouteExt<TInOut> service(String path, IRequestHandler<TInOut> handler,HttpMethod... methods) {
		getRoutes().register(path, handler, methods);
		return this;
	}
	
 	@SuppressWarnings("unchecked")
	public Observable<?> call(Chain<TInOut,TInOut> chain,TInOut ctx) {
 		Route route = getRoutes().select(ctx.getRequest());
		if (route == null) {
			return Observable.error(Check.NotFound.toCheckError(new Check.NotFound(){{
				this.info =String.format("not found %s %s", ctx.getRequest().getHttpMethod(),ctx.getRequest().getPath()); 
			}}).toFastException());
		}
		Object result=route.getHandler().handle(ctx);
		return result instanceof Observable ? (Observable<?>)result:Observable.just(result);
	}
	public Routes getRoutes() {
		return routes;
	}
}