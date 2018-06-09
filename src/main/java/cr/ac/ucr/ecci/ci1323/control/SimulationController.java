package cr.ac.ucr.ecci.ci1323.control;

import cr.ac.ucr.ecci.ci1323.control.context.Context;
import cr.ac.ucr.ecci.ci1323.control.context.ContextQueue;
import cr.ac.ucr.ecci.ci1323.core.CoreOne;
import cr.ac.ucr.ecci.ci1323.core.CoreZero;

import java.util.ArrayList;

public class SimulationController {

    private volatile ContextQueue contextQueue;
    private volatile ArrayList<Context> finishedThreads;
    private CoreZero coreZero;
    private CoreOne coreOne;

    public SimulationController() {
        this.contextQueue = new ContextQueue();
        this.finishedThreads = new ArrayList<Context>();
    }

    private void parseContextFile () {

        this.contextQueue.pushContext(new Context(21));
        this.contextQueue.pushContext(new Context(21));

    }

    public void runSimulation() {
        this.parseContextFile();

        this.coreZero = new CoreZero(5, this.contextQueue.getNextContext(), this);
        this.coreOne = new CoreOne(5, this.contextQueue.getNextContext(), this);

        this.coreZero.run();
        this.coreOne.run();
    }

    public ContextQueue getContextQueue() {
        return contextQueue;
    }

    public ArrayList<Context> getFinishedThreads() {
        return finishedThreads;
    }

}
