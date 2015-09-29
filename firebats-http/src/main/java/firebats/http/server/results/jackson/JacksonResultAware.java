package firebats.http.server.results.jackson;

import firebats.http.server.ContextAware;

public interface JacksonResultAware extends ContextAware{
    /**
    * Default redirect, see {@link #redirectBySeeOther()}
    * Generates a 303 SEE_OTHER result.
    *
    * @param uri The uri to redirect.
    */
    default JacksonResult jackson(Object body){
		return new JacksonResult(getContext(),body).status200Ok();
    }
}