package au.id.bjf.vmsgreader;

/**
 * Exception thrown from {@link VmsgXmlFactory}.  Allows user to specify code
 * of specific error condition.
 */
public class VmsgXmlFactoryException extends RuntimeException {

	/** Default serial version ID */
	private static final long serialVersionUID = 1L;
	
	/** Error that caused this exception */
	private VmsgErrors errorCode;
	
	/** Message to aid diagnosis */
	private String message;

	
	public VmsgXmlFactoryException(final VmsgErrors errorCode) {
		this.errorCode = errorCode;
	}
	
	public VmsgXmlFactoryException(final VmsgErrors errorCode, final String cause) {
		this(errorCode);
		setMessage(cause);
	}
	
	public VmsgErrors getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(final VmsgErrors errorCode) {
		this.errorCode = errorCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}
	
	@Override
	public String toString() {
		return getClass().getName() + ": " + " error code " + errorCode 
				+ " cause: " + message + ".  " + super.toString();
	}
	
}
