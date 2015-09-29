package firebats.check;

public enum CheckErrorType {
    Fail("F"),
    Exception("E");
    
    private String tip;
    
	CheckErrorType(String tip){
    	this.tip=tip;
    }
	public String getTip() {
		return tip;
	}
}