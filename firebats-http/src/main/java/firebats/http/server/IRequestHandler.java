package firebats.http.server;




public interface IRequestHandler<TCtx>{
	/**
	 *  可以返回任意类型的结果，他们将被上级chain处理，
	 *  比如上级chain区别对待Object或HttpResult或Observable<?> 类型的结果，
	 */
	public Object handle(TCtx ctx);
}