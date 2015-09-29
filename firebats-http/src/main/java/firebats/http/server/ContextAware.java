package firebats.http.server;


/**
 * 需要context才能办事的类或接口，可以implements此接口，以得到context。<p></p>
 * 此类命名符合IOC(Inversion of control ): Something...Aware 习惯
 */
public interface ContextAware {
	Context getContext();
}