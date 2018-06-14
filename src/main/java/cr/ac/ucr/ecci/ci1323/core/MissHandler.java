package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.cache.DataCache;
import cr.ac.ucr.ecci.ci1323.cache.InstructionCache;
import cr.ac.ucr.ecci.ci1323.context.Context;

import java.util.concurrent.Phaser;

public class MissHandler extends Thread {

    private CoreZero coreZero;
    private Context context;
    private MissType missType;
    private Phaser simulationBarrier;
    private int nextCachePosition;
    private int nextBlockNumber;

    MissHandler(CoreZero coreZero, Context context, MissType missType, Phaser simulationBarrier, int nextBlockNumber,
                int nextCachePosition) {
        this.coreZero = coreZero;
        this.context = context;
        this.missType = missType;
        this.simulationBarrier = simulationBarrier;
        this.nextCachePosition = nextCachePosition;
        this.nextBlockNumber = nextBlockNumber;
    }

    @Override
    public void run() {
        this.simulationBarrier.register();
        this.solveMiss();
        // TODO (DANIEL) resolvio fallo
    }

    public void solveMiss() {

        switch (this.missType) {
            case INSTRUCTION:
                this.solveInstructionMiss();
            default:
                throw new IllegalArgumentException("Invalid Miss Type in miss handler.");
        }
    }

    private void solveInstructionMiss() {
        // there is no other cache position reserved
        this.coreZero.setReservedInstructionCachePosition(this.nextCachePosition);
        this.coreZero.getInstructionCache().getInstructionBlockFromMemory(this.nextBlockNumber,
                this.nextCachePosition, this.coreZero);
        this.coreZero.setReservedInstructionCachePosition(-1);
    }

    private void advanceClockCycle() {
        this.simulationBarrier.arriveAndAwaitAdvance();
        this.context.incrementClockCycle();
        // Por ahora nada mas
        this.simulationBarrier.arriveAndAwaitAdvance();
    }

    public void setCoreZero(CoreZero coreZero) {
        this.coreZero = coreZero;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setMissType(MissType missType) {
        this.missType = missType;
    }

    public void setSimulationBarrier(Phaser simulationBarrier) {
        this.simulationBarrier = simulationBarrier;
    }

}
