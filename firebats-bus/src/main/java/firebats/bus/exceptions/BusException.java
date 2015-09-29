package firebats.bus.exceptions;

public class BusException extends RuntimeException {
	private static final long serialVersionUID = 3279505381301611076L;

	public BusException(String message) {
		super(message);
	}
    public BusException(String message, Throwable cause) {
        super(message, cause);
    }
    public BusException( Throwable cause) {
        super(cause);
    }
}