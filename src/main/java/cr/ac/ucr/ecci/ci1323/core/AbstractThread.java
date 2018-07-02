package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.context.Context;

import java.util.concurrent.Phaser;

/**
 * Abstract thread which contains the shared properties of a thread, which can be a Core or MissHandler thread, and
 * inherits them the methods of advancing the clock cycle or the barriers.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public abstract class AbstractThread extends Thread {

    protected volatile Phaser simulationBarrier;
    protected volatile Context currentContext;

    AbstractThread(Phaser simulationBarrier, Context currentContext) {
        this.simulationBarrier = simulationBarrier;
        this.currentContext = currentContext;
    }

    /**
     * Advances the clock cycle in "zero time", by using 2 barriers.
     */
    public void advanceClockCycle() {
        this.simulationBarrier.arriveAndAwaitAdvance();
        this.currentContext.incrementClockCycle();
        this.simulationBarrier.arriveAndAwaitAdvance();
    }

    /**
     * Advances both barriers without incrementing the clock cycle.
     */
    protected void advanceBarriers() {
        this.simulationBarrier.arriveAndAwaitAdvance();
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
