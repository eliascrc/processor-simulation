package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.control.Context;
import cr.ac.ucr.ecci.ci1323.control.ContextQueue;

abstract class AbstractCore extends Thread {

    /*
    private DataCache dataCache;
    private InstructionCache instructionCache;*/
    Context currentContext;
    ContextQueue contextQueue;
    int maxQuantum;
    int currentQuantum;
    int[] registers;

    AbstractCore (int maxQuantum, Context startingContext, ContextQueue contextQueue) {
        this.maxQuantum = maxQuantum;
        this.contextQueue = contextQueue;
        this.currentContext = startingContext;
        this.currentQuantum = 0;
        this.registers = new int[32];
    }

}
