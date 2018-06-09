package cr.ac.ucr.ecci.ci1323.control;

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

        this.contextQueue.pushContext(new Context(21, new int[]{2, 2, 2, 2, 2}));
        this.contextQueue.pushContext(new Context(21, new int[]{2, 3, 3, 2, 3}));

    }

    public void runSimulation() {
        this.parseContextFile();

        this.coreZero = new CoreZero(5, this.contextQueue.getNextContext(), this.contextQueue);
        this.coreOne = new CoreOne(5, this.contextQueue.getNextContext(), this.contextQueue);

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
