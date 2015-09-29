package firebats.check;

import static firebats.check.CheckErrorType.Exception;
import static firebats.check.CheckErrorType.Fail;
import rx.functions.Action1;

import com.google.common.base.Strings;

import firebats.render.IRender;
import firebats.templates.Template;

/**
 * 系统断言(或检查),类似Preconditions/Assert，但比其多了错误码和基于Mustache的错误消息模板,<br/>
 * 不取名为Assert是由于Assert概念是作为前置后置条件检查，来尽早提示程序的bug，<br/>
 * 而我们主要用来抛出携带错误码的业务型错误 <br/>
 */

public final class Check<CONTEXT>{
	
	/**通用业务失败*/
	public static final Check<firebats.check.Check.GeneralFail> GeneralFail =           f(100001,"${info}",GeneralFail.class);public static class GeneralFail{public String info;}
	/**通用故障*/
	public static final Check<firebats.check.Check.GeneralException> GeneralException = e(100002,"${info}",GeneralException.class);public static class GeneralException{public String info;}
	/**参数检查*/
	public static final Check<firebats.check.Check.ParamInvalid> ParamInvalid =         e(100003,"param invalid[${name}]=[${value}]",ParamInvalid.class);public static class ParamInvalid{public Object name,value;}
//	/**由于系统接口和客户端有重大升级，必须升级客户端才能继续使用系统。*/
//	public static final Check<firebats.check.Check.ClientNeedUpgrade> ClientNeedUpgrade=f(100004,"${info}",ClientNeedUpgrade.class);public static class ClientNeedUpgrade{public Object info;}
	/***/
	public static final Check<Void> ServiceTimeout =               e(100005,"Service timeout,please retry");
//	/***/
//	public static final Check<Void> ServerMaintenance =            e(100006,"服务器正在维护，暂不提供访问");
	
	//------------------------------------------
	//<http status code 映射>
	//------------------------------------------
	/**同http status code 未登陆*/
	public static final Check<firebats.check.Check.Unauthorized> Unauthorized =         f(100401,"${info}",Unauthorized.class);public static class Unauthorized{public Object info;}
	/**同http status code 禁止访问*/
	public static final Check<firebats.check.Check.Forbidden> Forbidden =               f(100403,"${info}",Forbidden.class);public static class Forbidden{public Object info;}
	/**同http status code 禁止访问*/
	public static final Check<firebats.check.Check.NotFound> NotFound =                 f(100404,"${info}",NotFound.class);public static class NotFound{public Object info;}
	//------------------------------------------
	//</http status code 映射>
	//------------------------------------------
	
	private CheckErrorType errorType; 
	private int errorCode;
	private IRender<CONTEXT,String> render;

	public static <CONTEXT> Check<CONTEXT> of(CheckErrorType errorType,int errorCode, IRender<CONTEXT,String> render) {
		return new Check<CONTEXT>(errorType,errorCode,render);
	}
	public static <CONTEXT> Check<CONTEXT> f(int errorCode, String messageTemplate,Class<CONTEXT> contextClass) {
		return of(Fail,errorCode, Template.ofString(messageTemplate, contextClass));
	}
	public static <CONTEXT> Check<CONTEXT> e(int errorCode, String messageTemplate,Class<CONTEXT> contextClass) {
		return of(Exception,errorCode, Template.ofString(messageTemplate, contextClass));
	}
	public static Check<Void> f(int errorCode, String messageTemplate) {
		return of(Fail,errorCode, Template.ofString(messageTemplate, Void.class));
	}
	public static Check<Void> e(int errorCode, String messageTemplate) {
		return of(Exception,errorCode, Template.ofString(messageTemplate, Void.class));
	}
	private Check(CheckErrorType errorType,int errorCode,IRender<CONTEXT,String> render) {
		this.errorType=errorType;
		this.errorCode=errorCode;
		this.render=render;
	}

	public void fail(CONTEXT context) {
 		throw toCheckError(context).toException();
	}
	public void fail(Action1<CONTEXT> contextSetter) {
 		throw toCheckError(contextSetter).toException();
	}
	public void fail() {
 		throw toCheckError().toException();
	}
	public CheckError toCheckError(CONTEXT context) {
		return CheckError.of(errorType,errorCode,message(context));
	}
	public CheckError toCheckError() {
		return CheckError.of(errorType,errorCode,message());
	}
	public CheckError toCheckError(Action1<CONTEXT> contextSetter) {
		return CheckError.of(errorType,errorCode,message(contextSetter));
	}
	public String message(){
    	return render.get();
    }
	public String message(CONTEXT context){
    	return render.get(context);
    }
	public String message(Action1<CONTEXT> contextSetter){
    	return render.get(contextSetter);
    }
	public boolean check(boolean expression, CONTEXT context) {
		if (!expression) {
			fail(context);
		}
		return expression;
	}
	public boolean check(boolean expression, Action1<CONTEXT> contextSetter) {
		if (!expression) {
			fail(contextSetter);
		}
		return expression;
	}
	public boolean check(boolean expression) {
		if (!expression) {
			fail();
		}
		return expression;
	}
	public <T> T checkNotNull(T reference,CONTEXT context) {
		check(reference!=null,context);
		return reference;
	}
	public <T> T checkNotNull(T reference,Action1<CONTEXT> contextSetter) {
		check(reference!=null,contextSetter);
		return reference;
	}
	public <T> T checkNotNull(T reference) {
		check(reference!=null);
		return reference;
	}
	public String checkNotEmpty(String reference,CONTEXT context) {
		check(!Strings.isNullOrEmpty(reference),context);
		return reference;
	}
	public String checkNotEmpty(String reference,Action1<CONTEXT> contextSetter) {
		check(!Strings.isNullOrEmpty(reference),contextSetter);
		return reference;
	}
	public String checkNotEmpty(String reference) {
		check(!Strings.isNullOrEmpty(reference));
		return reference;
	}
	public int getErrorCode() {
		return errorCode;
	}
	
}