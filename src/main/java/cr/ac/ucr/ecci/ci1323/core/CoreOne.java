package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.control.SimulationController;
import cr.ac.ucr.ecci.ci1323.control.context.Context;
import cr.ac.ucr.ecci.ci1323.control.context.ContextQueue;

public class CoreOne extends AbstractCore {

    public CoreOne(int maxQuantum, Context startingContext, SimulationController simulationController) {
        super(maxQuantum, startingContext, simulationController);
    }

    @Override
    public void run() {
        System.out.println("Core One! Ready. The context is: ");
        super.currentContext.print();
    }

}
