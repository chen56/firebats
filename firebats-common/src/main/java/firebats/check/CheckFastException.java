package firebats.check;




public class CheckFastException extends CheckException {
	private static final long serialVersionUID = 4738068121317144063L;

	public CheckFastException(CheckError error) {
		super(error);
 	}
	/**
     * 避免制造堆栈而提高异常创建速度
     * @return always null
     */
    public Throwable fillInStackTrace() {
        return null;
    }
}
