package cr.ac.ucr.ecci.ci1323.exceptions;

/**
 * A runtime exception that is thrown when a core tries to handle the context queue by miss using the lock
 *
 * @author Elias Calderon
 */
public class TryLockException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor
     */
    public TryLockException() {
    }

    /**
     * Creates a new exception with the specified message
     * @param message the message to display
     */
    public TryLockException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the specified wrapped exception
     * @param cause the cause of the exception
     */
    public TryLockException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new exception with the specified message and wrapped exception
     * @param message the message to display
     * @param cause the cause of the exception
     */
    public TryLockException(String message, Throwable cause) {
        super(message, cause);
    }

}
