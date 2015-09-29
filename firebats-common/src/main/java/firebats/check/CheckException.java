package firebats.check;





public class CheckException extends RuntimeException {
	private static final long serialVersionUID = 4738068121317144063L;
	private CheckError error;

	public CheckException(CheckError error) {
		super(error.getMessage());
		this.error=error;
 	}
	public CheckException(CheckError error,Throwable cause) {
		super(error.getMessage(),cause);
		this.error=error;
 	}

	public String getDetail() {
		return error.getDetail();
	}

	public int getCode() {
		return error.getCode();
	}

	public CheckError getError() {
 		return error;
	}
}