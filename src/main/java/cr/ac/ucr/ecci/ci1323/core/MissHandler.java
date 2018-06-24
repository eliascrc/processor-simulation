package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.cache.CachePositionState;
import cr.ac.ucr.ecci.ci1323.cache.DataCache;
import cr.ac.ucr.ecci.ci1323.cache.DataCachePosition;
import cr.ac.ucr.ecci.ci1323.cache.InstructionCache;
import cr.ac.ucr.ecci.ci1323.context.Context;

import java.util.concurrent.Phaser;

public class MissHandler extends AbstractThread {

    private CoreZero coreZero;
    private MissType missType;
    private int nextCachePosition;
    private int nextBlockNumber;
    private DataCachePosition dataCachePosition;
    private int dataCachePositionOffset;
    private int finalRegister;

    MissHandler(CoreZero coreZero, Context context, MissType missType, Phaser simulationBarrier, int nextBlockNumber,
                int nextCachePosition, DataCachePosition dataCachePosition, int dataCachePositionOffset, int finalRegister) {
        super(simulationBarrier, context);
        this.coreZero = coreZero;
        this.missType = missType;
        this.simulationBarrier.register();
        this.nextCachePosition = nextCachePosition;
        this.nextBlockNumber = nextBlockNumber;
        this.dataCachePosition = dataCachePosition;
        this.dataCachePositionOffset = dataCachePositionOffset;
        this.finalRegister = finalRegister;
    }

    @Override
    public void run() {
        this.solveMiss();
        this.coreZero.setWaitingContext(this.currentContext);
        this.coreZero.finishMissHandlerExecution();
        this.simulationBarrier.arriveAndDeregister();
    }

    public void solveMiss() {
        switch (this.missType) {
            case INSTRUCTION:
                this.solveInstructionMiss();
                break;

            case LOAD:
                this.solveDataLoadMiss();
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
    }

    private void solveDataLoadMiss() {
        boolean solvedMiss = false;
        while (!solvedMiss) {
            this.coreZero.blockDataCachePosition(this.nextCachePosition);

            if (dataCachePosition.getTag() != this.nextBlockNumber || dataCachePosition.getState() == CachePositionState.INVALID) {
                solvedMiss = this.coreZero.solveDataLoadMissLocally(this.nextBlockNumber, this.dataCachePosition, this.dataCachePositionOffset, this.nextCachePosition, this.finalRegister);
            } else { // Hit
                this.currentContext.getRegisters()[this.finalRegister] = dataCachePosition.getDataBlock().getWord(dataCachePositionOffset);
                solvedMiss = true;
            }
            dataCachePosition.unlock();
        }
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
