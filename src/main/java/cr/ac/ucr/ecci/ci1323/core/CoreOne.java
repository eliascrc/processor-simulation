package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.control.Context;
import cr.ac.ucr.ecci.ci1323.control.ContextQueue;

public class CoreOne extends AbstractCore {

    public CoreOne(int maxQuantum, Context startingContext, ContextQueue contextQueue) {
        super(maxQuantum, startingContext, contextQueue);
    }

    @Override
    public void run() {
        System.out.println("Core One! Ready. The context is: ");
        super.currentContext.print();
    }

}
