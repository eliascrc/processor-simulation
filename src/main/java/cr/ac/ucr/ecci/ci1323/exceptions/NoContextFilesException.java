package cr.ac.ucr.ecci.ci1323.exceptions;

/**
 * A runtime exception that is thrown when there are insufficient context files to run the simulation.
 *
 * @author Josué León Sarkis, Daniel Montes de Oca, Elias Calderón
 */
public class NoContextFilesException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor
     */
    public NoContextFilesException() {
    }

    /**
     * Creates a new exception with the specified message
     * @param message the message to display
     */
    public NoContextFilesException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the specified wrapped exception
     * @param cause the cause of the exception
     */
    public NoContextFilesException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new exception with the specified message and wrapped exception
     * @param message the message to display
     * @param cause the cause of the exception
     */
    public NoContextFilesException(String message, Throwable cause) {
        super(message, cause);
    }

}
