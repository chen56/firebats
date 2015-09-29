package firebats.http.server.exts.session;

import firebats.http.server.ContextAware;

public interface SessionAware extends ContextAware{
    /**
    * get form.
    */
    default HttpSession getSession(){
		return SessionExt.get(getContext());
    }
}