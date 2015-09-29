package firebats.bus.exceptions;


/**
 * 避免制造堆栈而提高异常创建速度
 */
public class FastBusException extends BusException {

	private static final long serialVersionUID = -7154759847662353272L;

    public FastBusException( String message){
        super(message);
    }

    public FastBusException(String message, Throwable cause){
        super(message, cause);
    }

    public FastBusException(Throwable cause){
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
