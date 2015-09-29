package firebats.check;

import firebats.i18n.I18n;

public class Checks {
	protected final PropertiesCheckFactory factory=PropertiesCheckFactory.ofNoInit();

	/**通用业务失败*/
	public final Check<GeneralFail> GeneralFail =           factory.f(100001,"firebats.check.generalFail",GeneralFail.class);public static class GeneralFail{public String info;}
	/**通用故障*/
	public final Check<GeneralException> GeneralException = factory.e(100002,"firebats.check.generalException",GeneralException.class);public static class GeneralException{public String info;}
	/**参数检查*/
	public final Check<ParamInvalid> ParamInvalid =         factory.e(100003,"firebats.check.paramInvalid",ParamInvalid.class);public static class ParamInvalid{public Object name,value;}
	/**由于系统接口和客户端有重大升级，必须升级客户端才能继续使用系统。*/
	public final Check<ClientNeedUpgrade> ClientNeedUpgrade=factory.f(100004,"firebats.check.clientNeedUpgrade",ClientNeedUpgrade.class);public static class ClientNeedUpgrade{public Object info;}
	/***/
	public final Check<Void> ServiceTimeout =               factory.e(100005,"firebats.check.serviceTimeout");
	/***/
	public final Check<Void> ServerMaintenance =            factory.e(100006,"firebats.check.serverMaintenance");
	
	//------------------------------------------
	//<http status code 映射>
	//------------------------------------------
	/**同http status code 未登陆*/
	public final Check<Unauthorized> Unauthorized =         factory.f(100401,"firebats.check.unauthorized",Unauthorized.class);public static class Unauthorized{public Object info;}
	/**同http status code 禁止访问*/
	public final Check<Forbidden> Forbidden =               factory.f(100403,"firebats.check.forbidden",Forbidden.class);public static class Forbidden{public Object info;}
	/**同http status code 禁止访问*/
	public final Check<NotFound> NotFound =                 factory.f(100404,"firebats.check.notFound",NotFound.class);public static class NotFound{public Object info;}

    public Checks(I18n i18n){
    	factory.init(i18n.getResource());
    }
}
