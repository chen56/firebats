package firebats.http.server.results.redirect;

import io.netty.handler.codec.http.HttpResponseStatus;
import firebats.http.server.ContextAware;

public interface RedirectResultAware extends ContextAware{
    /**
    * Default redirect,Generates a 303 SEE_OTHER result.<p></p> 
    * if you want to change the status code, use: {@link HttpResult#status302Found()}...<p></p>
    *
    * @param uri The uri to redirect.
    */
    default RedirectResult redirect(String uri){
    	return new RedirectResult(getContext(),HttpResponseStatus.SEE_OTHER, uri);
    }
}