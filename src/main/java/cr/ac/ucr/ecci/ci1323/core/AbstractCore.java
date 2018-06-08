package cr.ac.ucr.ecci.ci1323.core;

public abstract class AbstractCore implements Runnable {

    /*
    private DataCache dataCache;
    private InstructionCache instructionCache;*/
    private Context currentContext;
    private int maxQuantum;
    private int currentQuantum;

    public AbstractCore (int maxQuantum, Context currentContext) {

    }

}
