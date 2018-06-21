package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.context.Context;

import java.util.concurrent.Phaser;

public abstract class AbstractThread extends Thread {

    protected Phaser simulationBarrier;
    protected Context currentContext;

    AbstractThread(Phaser simulationBarrier, Context currentContext) {
        this.simulationBarrier = simulationBarrier;
        this.currentContext = currentContext;
    }

    public void advanceClockCycle() {
        this.simulationBarrier.arriveAndAwaitAdvance();
        this.currentContext.incrementClockCycle();
        this.simulationBarrier.arriveAndAwaitAdvance();
    }

    public Phaser getSimulationBarrier() {
        return simulationBarrier;
    }

    public void setSimulationBarrier(Phaser simulationBarrier) {
        this.simulationBarrier = simulationBarrier;
    }

    public Context getCurrentContext() {
        return currentContext;
    }

    public void setCurrentContext(Context currentContext) {
        this.currentContext = currentContext;
    }
}
