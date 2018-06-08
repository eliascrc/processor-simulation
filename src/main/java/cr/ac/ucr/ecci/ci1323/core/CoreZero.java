package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.control.Context;
import cr.ac.ucr.ecci.ci1323.control.ContextQueue;
import cr.ac.ucr.ecci.ci1323.control.MissHandler;

public class CoreZero extends AbstractCore {

    private MissHandler missHandler;

    public CoreZero(int maxQuantum, Context startingContext, ContextQueue contextQueue) {
        super(maxQuantum, startingContext, contextQueue);
    }

    @Override
    public void run() {
        System.out.println("Core Zero! Ready. The context is: ");
        super.currentContext.print();

        this.missHandler = new MissHandler(this.currentContext);
        this.missHandler.run();
    }

}
