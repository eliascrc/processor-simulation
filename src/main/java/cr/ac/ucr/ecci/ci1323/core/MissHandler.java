package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.cache.DataCache;
import cr.ac.ucr.ecci.ci1323.cache.InstructionCache;
import cr.ac.ucr.ecci.ci1323.context.Context;

import java.util.concurrent.Phaser;

public class MissHandler extends AbstractThread {

    private CoreZero coreZero;
    private MissType missType;
    private int nextCachePosition;
    private int nextBlockNumber;

    MissHandler(CoreZero coreZero, Context context, MissType missType, Phaser simulationBarrier, int nextBlockNumber,
                int nextCachePosition) {
        super(simulationBarrier, context);
        this.coreZero = coreZero;
        this.missType = missType;
        this.simulationBarrier.register();
        this.nextCachePosition = nextCachePosition;
        this.nextBlockNumber = nextBlockNumber;
    }

    @Override
    public void run() {
        this.solveMiss();
        this.simulationBarrier.arriveAndDeregister();
    }

    public void solveMiss() {
        switch (this.missType) {
            case INSTRUCTION:
                this.solveInstructionMiss();
                break;

            default:
                throw new IllegalArgumentException("Invalid Miss Type in miss handler.");
        }
    }

    private void solveInstructionMiss() {
        // there is no other cache position reserved
        this.coreZero.setReservedInstructionCachePosition(this.nextCachePosition);
        this.coreZero.getInstructionCache().getInstructionBlockFromMemory(this.nextBlockNumber,
                this.nextCachePosition, this);
        this.coreZero.setReservedInstructionCachePosition(-1);
        this.coreZero.setWaitingContext(this.currentContext);
        System.out.println("MH:" + this.currentContext.getContextNumber());
        this.coreZero.finishMissHandlerExecution();
    }

    public void setCoreZero(CoreZero coreZero) {
        this.coreZero = coreZero;
    }

    public void setMissType(MissType missType) {
        this.missType = missType;
    }

    public void setSimulationBarrier(Phaser simulationBarrier) {
        this.simulationBarrier = simulationBarrier;
    }

}
