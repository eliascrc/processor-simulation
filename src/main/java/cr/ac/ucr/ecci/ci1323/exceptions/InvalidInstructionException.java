package cr.ac.ucr.ecci.ci1323.exceptions;

/**
 * A runtime exception that is thrown when an instruction doesn´t meet the standard structure
 *
 * @author Daniel Montes de Oca
 */
public class InvalidInstructionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor
     */
    public InvalidInstructionException() {
    }

    /**
     * Creates a new exception with the specified message
     * @param message the message to display
     */
    public InvalidInstructionException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the specified wrapped exception
     * @param cause the cause of the exception
     */
    public InvalidInstructionException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new exception with the specified message and wrapped exception
     * @param message the message to display
     * @param cause the cause of the exception
     */
    public InvalidInstructionException(String message, Throwable cause) {
        super(message, cause);
    }
}