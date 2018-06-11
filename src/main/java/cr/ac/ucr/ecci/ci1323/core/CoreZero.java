package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.controller.SimulationController;
import cr.ac.ucr.ecci.ci1323.context.Context;

public class CoreZero extends AbstractCore {

    private MissHandler missHandler;

    public CoreZero(int maxQuantum, Context startingContext, SimulationController simulationController) {
        super(maxQuantum, startingContext, simulationController);
    }

    @Override
    public void run() {
        System.out.println("Core Zero! Ready. The context is: ");
        super.currentContext.print();

        this.missHandler = new MissHandler(super.instructionCache, super.dataCache, this.currentContext);
        this.missHandler.run();
    }

}
