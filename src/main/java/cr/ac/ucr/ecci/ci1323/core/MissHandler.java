package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.cache.DataCache;
import cr.ac.ucr.ecci.ci1323.cache.InstructionCache;
import cr.ac.ucr.ecci.ci1323.control.context.Context;
import cr.ac.ucr.ecci.ci1323.memory.DataBus;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBus;

public class MissHandler extends Thread {

    private DataCache dataCache;
    private InstructionCache instructionCache;
    private Context context;

    public MissHandler (InstructionCache instructionCache, DataCache dataCache, Context context) {
        this.instructionCache = instructionCache;
        this.dataCache = dataCache;
        this.context = context;
    }

    @Override
    public void run() {
        System.out.println("Miss Handler ready! Context: ");
        this.context.print();
    }

}
