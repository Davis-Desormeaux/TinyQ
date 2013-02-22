package tinyq;

public class UDPException extends Exception {
	private static final long serialVersionUID = 2047567224076407329L;

	public UDPException(Exception e) {
		super(e);
	}

	public UDPException(String message, Exception e) {
		super(message, e);
  }
}
