package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.cache.CachePositionState;
import cr.ac.ucr.ecci.ci1323.cache.DataCachePosition;
import cr.ac.ucr.ecci.ci1323.context.Context;

import java.util.concurrent.Phaser;

public class MissHandler extends AbstractThread {

    private volatile CoreZero coreZero;
    private volatile MissType missType;
    private volatile int nextCachePosition;
    private volatile int nextBlockNumber;
    private volatile DataCachePosition dataCachePosition;
    private volatile int dataCachePositionOffset;
    private volatile int finalRegister;

    MissHandler(CoreZero coreZero, Context context, MissType missType, Phaser simulationBarrier, int nextBlockNumber,
                int nextCachePosition, DataCachePosition dataCachePosition, int dataCachePositionOffset, int finalRegister) {
        super(simulationBarrier, context);
        this.coreZero = coreZero;
        this.missType = missType;
        this.nextCachePosition = nextCachePosition;
        this.nextBlockNumber = nextBlockNumber;
        this.dataCachePosition = dataCachePosition;
        this.dataCachePositionOffset = dataCachePositionOffset;
        this.finalRegister = finalRegister;
    }

    @Override
    public void run() {
        this.simulationBarrier.register();
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

            case STORE:
                this.solveDataStoreMiss();
                break;

            case STORE_HIT:
                this.solveDataStoreHit();
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

            while (!this.dataCachePosition.tryLock()) {
                this.advanceClockCycle();
            }

            solvedMiss = this.coreZero.solveDataLoadMiss(this.nextBlockNumber, this.dataCachePosition,
                        this.dataCachePositionOffset, this.nextCachePosition, this.finalRegister, this);

        }
    }

    private void solveDataStoreMiss() {
        boolean solvedMiss = false;
        while (!solvedMiss) {

            while (!this.dataCachePosition.tryLock()) {
                this.advanceClockCycle();
            }

            solvedMiss = this.coreZero.solveDataStoreMiss(this.nextBlockNumber, this.dataCachePosition, this.dataCachePositionOffset, this.nextCachePosition, this.finalRegister, this);
        }
    }

    private void solveDataStoreHit() {
        boolean solvedMiss = false;
        while (!solvedMiss) {

            while (!this.dataCachePosition.tryLock()) {
                this.advanceClockCycle();
            }

            solvedMiss = this.coreZero.solveDataStoreHit(this.nextBlockNumber, this.dataCachePosition, this.nextCachePosition, this.dataCachePositionOffset, this.finalRegister, this);
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

    public int getNextCachePosition() {
        return nextCachePosition;
    }
}
