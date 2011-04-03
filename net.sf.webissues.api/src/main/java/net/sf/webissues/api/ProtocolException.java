package net.sf.webissues.api;

/**
 * Exception for errors throw during communication with the WebIssues server
 */
public class ProtocolException extends Exception {

    private static final long serialVersionUID = 8948172461016958626L;

    // Incomplete list

    /**
     * Server requires authentication before continuing. This may occur if the
     * session times out
     */
    public final static int LOGIN_REQUIRED = 300;
    /**
     * Access to the request resource is denied
     */
    public final static int ACCESS_DENIED = 301;
    /**
     * An invalid string was received by the server. In practice, this usually
     * means the length of a string exceeds the maximum allowed by server, or
     * there was some problem decoding the string.
     */
    public static final int INVALID_STRING = 340;
    /**
     * User name or password is incorrect
     */
    public final static int INCORRECT_LOGIN = 302;
    /**
     * User must change their password (occurs at logon).
     */
    public final static int MUST_CHANGE_PASSWORD = 352;
    /**
     * The password provided for a password change cannot be used
     * as it is identical to the current password
     */
    public final static int CANNOT_REUSE_PASSWORD = 353;
    
    /**
     * Not sent by server, used internally to indicate authentication was
     * cancelled
     */
    public static final int AUTHENTICATION_CANCELLED = 998;
    /**
     * Not sent by server, used internally to indicate an operation was
     * cancelled
     */
    public static final int CANCELLED = 999;

    private int code;

    /**
     * Constructor.
     * 
     * @param code error code
     */
    public ProtocolException(int code) {
        super();
        init(code);
    }

    /**
     * Constructor.
     * 
     * @param code error code
     * @param message message
     * @param cause cause
     */
    public ProtocolException(int code, String message, Throwable cause) {
        super(message, cause);
        init(code);
    }

    /**
     * Constructor.
     * 
     * @param code error code
     * @param message message
     */
    public ProtocolException(int code, String message) {
        super(message);
        init(code);
    }

    /**
     * Constructor.
     * 
     * @param code error code
     * @param cause cause
     */
    public ProtocolException(int code, Throwable cause) {
        super(cause);
        init(code);
    }

    /**
     * Get the error code.
     * 
     * @return code
     */
    public int getCode() {
        return code;
    }

    private void init(int code) {
        this.code = code;
    }
}
