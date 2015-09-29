package firebats.http.server.results.empty;

import firebats.http.server.ContextAware;

public interface EmptyResultAware extends ContextAware{
    /**
    * Default redirect, see {@link #redirectBySeeOther()}
    * Generates a 303 SEE_OTHER result.
    *
    * @param uri The uri to redirect.
    */
    default EmptyResult empty(){
		return new EmptyResult(getContext()).status200Ok();
    }
}