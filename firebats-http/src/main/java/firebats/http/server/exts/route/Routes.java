package firebats.http.server.exts.route;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;

import firebats.http.server.IRequestHandler;
import firebats.net.Path;

/**TODO SHOULD BE PRIVATE*/public class Routes {
	private List<Route> routes = new ArrayList<>();

	public void register(String path, @SuppressWarnings("rawtypes") IRequestHandler handler,
			HttpMethod... methods) {
		Path p = Path.fromPortableString(path);
		if (!p.isAbsolute()) {
			p = Path.ROOT.append(p);
		}
		register(new Route(p, handler, methods));
	}
	public void register(Route route) {
		Preconditions.checkNotNull(route,"param route should not be null.");
		routes.add(route);
	}

	/**
	 * 
	 */
	public Collection<Route> getAll() {
		return Lists.newArrayList(routes);
	}

	public Route select(HttpServerRequest<ByteBuf> request) { 
		for (Route route : routes) {
			if(route.isMatch(request)){
				return route;
			}
		}
		return null;
 	}
	@Override
	public String toString(){
		return routes.toString();
	}

	public static class Route {
		private Path path;
		private Set<HttpMethod> httpMethods;
		@SuppressWarnings("rawtypes")
		private IRequestHandler handler;

		private Route(Path path, @SuppressWarnings("rawtypes") IRequestHandler handler,
				HttpMethod... httpMethods) {
			this.path = path;
			this.handler = handler;
			this.httpMethods = ImmutableSortedSet.copyOf(httpMethods);
		}

		public boolean isMatch(HttpServerRequest<ByteBuf> request) {
			Path p = Path.fromPortableString(request.getPath());
 			return isMethodMatch(request.getHttpMethod())&&this.path.isPrefixOf(p);
		}

		public Set<HttpMethod> getHttpMethod() {
			return httpMethods;
		}

		public Path getPath() {
			return path;
		}

		public boolean isMethodMatch(HttpMethod method) {
			if (httpMethods.isEmpty())
				return true;
			return httpMethods.contains(method);
		}

		@SuppressWarnings("rawtypes")
		public IRequestHandler getHandler() {
			return handler;
		}
		
		@Override
		public String toString(){
			return Joiner.on(",").join(httpMethods)+":"+path.toPortableString()+":"+hashCode();
		}
	}



}