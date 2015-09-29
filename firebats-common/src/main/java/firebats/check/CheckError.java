package firebats.check;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;

import firebats.internal.http.HttpStatus;

//TODO error detail 一般用于调试，应该设置开关，在不需要时不要传送
public final class CheckError implements java.io.Serializable{
	private static final long serialVersionUID = 1505252056005431516L;
	private int code;// 自定义错误码
	private String message;// 错误描述
	private CheckErrorType type;
	private String detail;// 异常堆栈或细节原因，如果有的话,仅供调试
	/*for serialize*/ private CheckError(){}
	private CheckError(CheckErrorType type,int code,String message,String cause) {
		this.type=checkNotNull(type);
		this.code=code;
		this.message=message;
		this.detail=cause;
	}
	public static CheckError of(CheckErrorType type,int code,String message,Throwable cause){
		String detail = Throwables.getStackTraceAsString(cause);
		return new CheckError(type,code,message,detail);
	}
	public static CheckError of(CheckErrorType type,int code, String message, String detail) {
		return new CheckError(type,code,message,detail);
	}
	public static CheckError of(CheckErrorType type,int code,String message){
		return new CheckError(type,code,message,null);
	}
	
	public CheckErrorType getType() {
		return type;
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
	
	public String getDetail() {
		return detail;
	}
	
	public CheckError withDetail(String detail){
		return CheckError.of(type, code, message, detail);
	}
	public CheckError withDetail(Throwable detail){
		return CheckError.of(type, code, message, detail);
	}
	
	public CheckException toFastException() {
		return new CheckFastException(this);
	}
	public CheckException toException(String cause) {
 		return new CheckException(this,new CheckErrorCauseException(cause));
	}
	public CheckException toException(boolean includeStackTrace) {
 		return new CheckException(this);
	}
	public CheckException toException() {
 		return new CheckException(this,new CheckErrorCauseException(getDetail()));
	}
	public boolean isException() {
		return Objects.equal(type, CheckErrorType.Exception) ;
	}
	public boolean isFail() {
		return Objects.equal(type, CheckErrorType.Fail) ;
	}
	public int asHttpCode() {
		int c = code%1000;
		if(HttpStatus.containsCode(c)){
			return c;
		}
 		return 400;
	}
	@Override
	public String toString() {
 		return MoreObjects.toStringHelper(CheckError.class)
 				.add("code",code)
 				.add("message",message)
 				.add("type",type)
 				.add("detail",detail)
 				.toString();
	}
}