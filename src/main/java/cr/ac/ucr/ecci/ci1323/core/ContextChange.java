package cr.ac.ucr.ecci.ci1323.core;

/**
 * Enumeration used to specify the type of context change that the core has to realize.
 *
 * @author Elias Calderon, Josue Leon, Daniel Montes de Oca
 */
public enum ContextChange {

    /**
     * Swap the current context with the one waiting for entry
     */
    SWAP_CONTEXTS,

    /**
     * Bring the waiting context to execution. The current context has probably finished, expired his quantum,
     * or is in the miss handler.
     */
    BRING_WAITING,

    /**
     * Bring the context brought from the context queue to execution. The current context has probably finished, expired his quantum,
     * or is in the miss handler.
     */
    NEXT_CONTEXT,

    /**
     * There is no context change to be made.
     */
    NONE,

}
