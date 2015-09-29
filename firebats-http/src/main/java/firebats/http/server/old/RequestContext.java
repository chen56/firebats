package firebats.http.server.old;

import firebats.http.server.Context;
import firebats.http.server.exts.session.HttpSession;

/**
 * 
 *  @Deprecated 替代为：使用ext自行创建
 * */
@Deprecated

public class RequestContext extends Context{
    private HttpSession session;
 	public RequestContext(HttpSession session,Context context) {
		super(context);
		this.session=session;
	}
	protected RequestContext(RequestContext context) {
		this(context.getSession(),context);
	}
 	public HttpSession getSession() {
		return session;
	}
}