package firebats;


/**
 * 避免制造堆栈而提高异常创建速度
 */
public class FirebatsFastException extends RuntimeException {

	private static final long serialVersionUID = -7154759847662353272L;

    public FirebatsFastException( String message){
        super(message);
    }

    public FirebatsFastException(String message, Throwable cause){
        super(message, cause);
    }

    public FirebatsFastException(Throwable cause){
        super(cause);
    }

    /**
     * 避免制造堆栈而提高异常创建速度
     * @return always null
     */
    public Throwable fillInStackTrace() {
        return null;
    }
}
