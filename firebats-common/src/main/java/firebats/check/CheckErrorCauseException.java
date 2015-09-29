package firebats.check;

import firebats.FirebatsFastException;

public class CheckErrorCauseException extends FirebatsFastException{
	private static final long serialVersionUID = -7154759847662353272L;
    public CheckErrorCauseException(CheckException cause){
        super(cause);
    }
    public CheckErrorCauseException(String message) {
        super(message);
    }
}