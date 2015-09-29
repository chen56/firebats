package firebats.http.server.results.string;

import firebats.http.server.ContextAware;

public interface StringResultAware extends ContextAware{
    /**
    * Default redirect, see {@link #redirectBySeeOther()}
    * Generates a 303 SEE_OTHER result.
    *
    * @param uri The uri to redirect.
    */
    default StringResult string(String body){
		return new StringResult(getContext(),body).status200Ok();
    }
}