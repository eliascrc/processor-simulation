package cr.ac.ucr.ecci.ci1323.exceptions;

public class ContextChangeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor
     */
    public ContextChangeException() {
    }

    /**
     * Creates a new exception with the specified message
     * @param message the message to display
     */
    public ContextChangeException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the specified wrapped exception
     * @param cause the cause of the exception
     */
    public ContextChangeException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new exception with the specified message and wrapped exception
     * @param message the message to display
     * @param cause the cause of the exception
     */
    public ContextChangeException(String message, Throwable cause) {
        super(message, cause);
    }
}