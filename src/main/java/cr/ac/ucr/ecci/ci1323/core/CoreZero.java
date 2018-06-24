package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.cache.CachePositionState;
import cr.ac.ucr.ecci.ci1323.cache.DataCachePosition;
import cr.ac.ucr.ecci.ci1323.cache.InstructionCachePosition;
import cr.ac.ucr.ecci.ci1323.commons.SimulationConstants;
import cr.ac.ucr.ecci.ci1323.context.ContextQueue;
import cr.ac.ucr.ecci.ci1323.controller.SimulationController;
import cr.ac.ucr.ecci.ci1323.context.Context;
import cr.ac.ucr.ecci.ci1323.memory.DataBus;
import cr.ac.ucr.ecci.ci1323.memory.Instruction;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBlock;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBus;

import java.util.concurrent.Phaser;

/**
 * Core zero of the simulated processor.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public class CoreZero extends AbstractCore {

    private MissHandler missHandler;
    private Context waitingContext;

    private volatile int reservedDataCachePosition;

    private volatile int reservedInstructionCachePosition;

    private volatile ContextChange changeContext;
    private boolean waitingForReservation;

    public CoreZero(Phaser simulationBarrier, int maxQuantum, Context startingContext,
                    SimulationController simulationController, InstructionBus instructionBus,
                    DataBus dataBus, int coreNumber) {
        super(simulationBarrier, maxQuantum, startingContext, simulationController,
                SimulationConstants.TOTAL_CORE_ZERO_CACHE_POSITIONS, instructionBus, dataBus, coreNumber);

        this.waitingContext = null;
        this.reservedDataCachePosition = this.reservedInstructionCachePosition = -1;
        this.changeContext = ContextChange.NONE;
    }

    @Override
    public void run() {
        super.executeCore();
    }

    @Override
    protected void finishFINExecution() {
        if (this.waitingContext == null) { // checks if there is no other context waiting in execution

            ContextQueue contextQueue = this.simulationController.getContextQueue();

            // Tries to lock the context queue
            while (!contextQueue.tryLock()) {
                this.advanceClockCycle();
            }

            Context nextContext = contextQueue.getNextContext();
            if (nextContext == null) { // checks if there is no other context in the queue
                if (this.waitingContext == null && missHandler == null) {
                    this.executionFinished = true;
                } else {

                    while (this.waitingContext == null) { //
                        simulationBarrier.arriveAndAwaitAdvance();
                        simulationBarrier.arriveAndAwaitAdvance();
                    }

                    // If it finishes, bring the waiting context
                    this.changeContext = ContextChange.BRING_WAITING;
                    this.advanceClockCycle();

                }

            } else { // there is a context waiting in the queue
                System.out.println("cc: " + currentContext.getContextNumber() + " mh: " + missHandler.getCurrentContext().getContextNumber());
                this.currentContext = nextContext;
            }

            contextQueue.unlock();

        } else { // there is a waiting context in execution
            this.waitingContext.setOldContext(true);
            this.currentContext = this.waitingContext;
            this.waitingContext = null;
        }
    }

    @Override
    protected void executeSW(Instruction instruction) {

    }

    @Override
    protected void executeLW(Instruction instruction) {

    }

    @Override
    protected boolean handleLoadMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int dataCachePositionNumber, int finalRegister) {
            return this.enterCacheMiss(MissType.LOAD, blockNumber, dataCachePositionNumber, dataCachePosition, positionOffset, finalRegister);
    }

    @Override
    protected boolean handleStoreMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int value) {
        return false;
    }

    @Override
    protected boolean handleStoreHit(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int value) {
        return false;
    }

    @Override
    protected void blockDataCachePosition(int dataCachePosition) {
        while(this.reservedDataCachePosition == dataCachePosition) {
            this.advanceClockCycle();
        }
        DataCachePosition cachePosition = this.dataCache.getDataCachePosition(dataCachePosition);
        while (!cachePosition.tryLock()) {
            this.advanceClockCycle();
        }
    }

    @Override
    protected void quantumExpired() {
        if (this.waitingContext == null) { // checks if there is no other context waiting in execution

            super.quantumExpired();

        } else { // there is a waiting context in execution
            ContextQueue contextQueue = this.simulationController.getContextQueue();

            // Tries to lock the context queue
            while (!contextQueue.tryLock()) {
                this.advanceClockCycle();
            }
            this.currentContext.setOldContext(false);
            contextQueue.pushContext(this.currentContext);
            contextQueue.unlock();

            this.currentContext = this.waitingContext;
            this.waitingContext = null;
        }
    }

    @Override
    protected InstructionBlock getInstructionBlockFromCache(int nextInstructionBlockNumber, int nextInstructionCachePosition) {
        boolean solvedMiss = false;
        while (!solvedMiss) {
            while (this.reservedInstructionCachePosition != -1) {
                this.advanceClockCycle();
            }

            InstructionCachePosition instructionCachePosition = this.instructionCache
                    .getInstructionCachePosition(nextInstructionCachePosition);

            if (instructionCachePosition.getTag() != nextInstructionBlockNumber) { // miss
                solvedMiss = this.enterCacheMiss(MissType.INSTRUCTION, nextInstructionBlockNumber,
                        nextInstructionCachePosition, null, -1, -1);
            } else {
                solvedMiss = true;
            }
        }

        return this.instructionCache.getInstructionCachePosition(nextInstructionCachePosition).getInstructionBlock();
    }

    private boolean enterCacheMiss(MissType missType, int nextBlockNumber, int nextCachePosition, DataCachePosition dataCachePosition, int dataCachePositionOffset, int finalRegister) {
        boolean solvedMiss = true;
        ContextQueue contextQueue = this.simulationController.getContextQueue();
        if (this.waitingContext != null) { // there is a waiting context,
            this.missHandler = new MissHandler(this, this.currentContext, missType, this.simulationBarrier,
                    nextBlockNumber, nextCachePosition, dataCachePosition, dataCachePositionOffset, finalRegister);
            this.currentContext = this.waitingContext;
            this.waitingContext = null;
            this.missHandler.start();

        } else if (this.missHandler != null) { // miss handler is running, must try to solve by itself
            solvedMiss = this.solveMissLocally(missType, nextBlockNumber, nextCachePosition, dataCachePosition, dataCachePositionOffset, finalRegister);
        } else { // miss handler is not running and there is no waiting context
            this.missHandler = new MissHandler(this, this.currentContext, missType, this.simulationBarrier,
                    nextBlockNumber, nextCachePosition, dataCachePosition, dataCachePositionOffset, finalRegister);

            while (!contextQueue.tryLock())
                this.advanceClockCycle();

            Context nextContext = contextQueue.getNextContext();
            if (nextContext != null) {
                nextContext.setOldContext(false);
                this.currentContext = nextContext;
                this.missHandler.start();
            }
            contextQueue.unlock();
        }

        return solvedMiss;
    }

    public boolean solveMissLocally(MissType missType, int nextBlockNumber, int nextCachePosition, DataCachePosition dataCachePosition, int dataCachePositionOffset, int finalRegister) {

        switch (missType) {
            case INSTRUCTION:
                return this.solveInstructionMissLocally(nextBlockNumber, nextCachePosition);
            case LOAD:
                return this.solveDataLoadMissLocally(nextBlockNumber, dataCachePosition, dataCachePositionOffset, nextCachePosition, finalRegister);
            default:
                throw new IllegalArgumentException("Invalid Miss Type in core zero.");
        }
    }

    public boolean solveDataLoadMissLocally(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int dataCachePositionNumber, int finalRegister) {
        if(this.getReservedDataCachePosition() != -1) {
            this.advanceClockCycle();
            return false;
        } else {
            this.setReservedDataCachePosition(dataCachePositionNumber);
        }

        DataBus dataBus = this.dataCache.getDataBus();

        if (!dataBus.tryLock()) {
            this.advanceClockCycle();
            return false;
        }

        this.advanceClockCycle();
        if (dataCachePosition.getTag() != blockNumber && dataCachePosition.getState() == CachePositionState.MODIFIED) {
            this.dataCache.writeBlockToMemory(dataCachePosition, this);

        }

        int otherDataCachePositionNumber = this.calculateOtherDataCachePosition(dataCachePosition.getTag());
        DataCachePosition otherDataCachePosition = dataBus.getOtherCachePosition(this.coreNumber, otherDataCachePositionNumber);

        while (!otherDataCachePosition.tryLock()) {
            this.advanceClockCycle();
        }
        this.advanceClockCycle();

        if (otherDataCachePosition.getTag() == blockNumber && otherDataCachePosition.getState() == CachePositionState.MODIFIED) {
            this.dataCache.writeBlockToMemory(otherDataCachePosition, this);
            dataCachePosition.setDataBlock(otherDataCachePosition.getDataBlock().clone());
            otherDataCachePosition.setState(CachePositionState.SHARED);
        } else {
            this.dataCache.getBlockFromMemory(blockNumber, dataCachePositionNumber, this);
        }

        otherDataCachePosition.unlock();
        this.currentContext.getRegisters()[finalRegister] = dataCachePosition.getDataBlock().getWord(positionOffset);
        this.setReservedDataCachePosition(-1);
        dataBus.unlock();
        return true;
    }

    public boolean solveInstructionMissLocally(int nextBlockNumber, int nextCachePosition) {
        if (this.reservedInstructionCachePosition == -1) { // there is no other cache position reserved
            this.waitingForReservation = false;
            this.reservedInstructionCachePosition = nextCachePosition;
            this.instructionCache.getInstructionBlockFromMemory(nextBlockNumber, nextCachePosition, this);
            this.reservedInstructionCachePosition = -1;
            return true;
        }

        this.waitingForReservation = true;
        return false;
    }

    private void solvedMiss(Context solvedMissContext) {

        if (this.waitingContext == null) {
            System.out.println("Hola 2");
        }

        if (this.currentContext == null) { // there is no other thread in execution
            //TODO Pensarlo mejor
            this.currentContext = this.waitingContext;
            this.waitingContext = null;

        } else if (this.waitingContext.isOldContext()) // miss was resolved in old thread
            this.changeContext = ContextChange.SWAP;

        else if (this.waitingForReservation) // current thread in execution is in miss
            this.changeContext = ContextChange.SWAP;

        else // current thread in execution is not in miss
            this.changeContext = ContextChange.NONE;


    }


    public void finishMissHandlerExecution() {
        this.solvedMiss(this.missHandler.getCurrentContext());
        this.missHandler = null;
    }

    public void changeContext() {

        switch (this.changeContext) {
            case SWAP:
                Context tempContext = currentContext;
                this.currentContext = this.waitingContext;
                this.waitingContext = tempContext;
                break;
            case BRING_WAITING:
                this.currentContext = this.waitingContext;
                this.waitingContext = null;
                break;
        }

        this.changeContext = ContextChange.NONE;
    }

    public boolean isWaitingForReservation() {
        return waitingForReservation;
    }

    public void setWaitingForReservation(boolean waitingForReservation) {
        this.waitingForReservation = waitingForReservation;
    }

    public ContextChange isChangeContext() {
        return changeContext;
    }

    public void setChangeContext(ContextChange changeContext) {
        this.changeContext = changeContext;
    }

    public MissHandler getMissHandler() {
        return missHandler;
    }

    public void setMissHandler(MissHandler missHandler) {
        this.missHandler = missHandler;
    }

    public Context getWaitingContext() {
        return waitingContext;
    }

    public void setWaitingContext(Context waitingContext) {
        this.waitingContext = waitingContext;
    }

    public int getReservedDataCachePosition() {
        return reservedDataCachePosition;
    }

    public void setReservedDataCachePosition(int reservedDataCachePosition) {
        this.reservedDataCachePosition = reservedDataCachePosition;
    }

    public int getReservedInstructionCachePosition() {
        return reservedInstructionCachePosition;
    }

    public void setReservedInstructionCachePosition(int reservedInstructionCachePosition) {
        this.reservedInstructionCachePosition = reservedInstructionCachePosition;
    }

}
