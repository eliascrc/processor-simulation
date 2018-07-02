package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.context.Context;

import java.util.concurrent.Phaser;

/**
 * Abstract class that represents an abstraction for cores and the miss handler, in order to advance clock cycles or barriers
 * and manipulating the context that they're currently executing. Extends a runnable thread of Java.
 *
 * @author Elias Calderon, Josue Leon, Daniel Montes de Oca
 */
public abstract class AbstractThread extends Thread {

    /**
     * The barriers for advancing clock cycles.
     */
    protected volatile Phaser simulationBarrier;

    /**
     * The context that is currently executing the core or miss handler.
     */
    protected volatile Context currentContext;

    /**
     * Constructor that receives the barrier and context.
     * @param simulationBarrier the simulation's barrier.
     * @param currentContext the context to execute.
     */
    AbstractThread(Phaser simulationBarrier, Context currentContext) {
        this.simulationBarrier = simulationBarrier;
        this.currentContext = currentContext;
    }

    /**
     * Advances a clock cycle in the time zero between two barriers.
     */
    public void advanceClockCycle() {
        this.simulationBarrier.arriveAndAwaitAdvance();
        this.currentContext.incrementClockCycle();
        this.simulationBarrier.arriveAndAwaitAdvance();
    }

    /**
     * Advances only the barriers, and doesn't increment the clock cycle of the context.
     */
    protected void advanceBarriers() {
        this.simulationBarrier.arriveAndAwaitAdvance();
        this.simulationBarrier.arriveAndAwaitAdvance();
    }

    //----------------------------------------------------------------------------------------
    // Setters and Getters
    //----------------------------------------------------------------------------------------

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
