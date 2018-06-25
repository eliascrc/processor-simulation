package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.cache.CachePositionState;
import cr.ac.ucr.ecci.ci1323.cache.DataCachePosition;
import cr.ac.ucr.ecci.ci1323.cache.InstructionCachePosition;
import cr.ac.ucr.ecci.ci1323.commons.SimulationConstants;
import cr.ac.ucr.ecci.ci1323.context.ContextQueue;
import cr.ac.ucr.ecci.ci1323.controller.SimulationController;
import cr.ac.ucr.ecci.ci1323.context.Context;
import cr.ac.ucr.ecci.ci1323.exceptions.ContextChangeException;
import cr.ac.ucr.ecci.ci1323.memory.DataBus;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBlock;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBus;

import java.util.concurrent.Phaser;

/**
 * Core zero of the simulated processor.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public class CoreZero extends AbstractCore {

    private volatile MissHandler missHandler;

    private volatile Context waitingContext;

    private volatile int reservedDataCachePosition;

    private volatile int reservedInstructionCachePosition;

    private volatile boolean contextFinished;

    public CoreZero(Phaser simulationBarrier, int maxQuantum, Context startingContext,
                    SimulationController simulationController, InstructionBus instructionBus,
                    DataBus dataBus, int coreNumber) {
        super(simulationBarrier, maxQuantum, startingContext, simulationController,
                SimulationConstants.TOTAL_CORE_ZERO_CACHE_POSITIONS, instructionBus, dataBus, coreNumber);

        this.waitingContext = null;
        this.reservedDataCachePosition = this.reservedInstructionCachePosition = -1;
    }

    @Override
    public void run() {
        super.executeCore();
    }


    public void changeContext() {
        boolean startMissHandler = false;

        if (this.executionFinished && this.changeContext == ContextChange.SWAP_CONTEXTS) {
            // If the current context has finished or his quantum expired and the miss handler is requesting a swap
            // just advance and ignore the request until BRING_WAITING is set
            this.setChangeContext(ContextChange.NONE);
            this.simulationBarrier.arriveAndAwaitAdvance();
            return;
        }

        switch (this.changeContext) {
            case SWAP_CONTEXTS:
                Context tempContext = currentContext;
                this.setCurrentContext(this.waitingContext);
                this.setWaitingContext(tempContext);
                break;

            case BRING_WAITING:
                this.setCurrentContext(this.waitingContext);
                this.currentContext.setOldContext(true);
                this.setWaitingContext(null);
                break;

            case BRING_WAITING_START_MISS_HANDLER:
                this.setCurrentContext(this.waitingContext);
                this.setWaitingContext(null);
                startMissHandler = true;
                break;

            case NEXT_CONTEXT_START_MISS_HANDLER:
                this.setCurrentContext(this.nextContext);
                this.currentContext.setOldContext(true);
                this.setNextContext(null);
                startMissHandler = true;
                break;

            case NEXT_CONTEXT:
                this.setCurrentContext(this.nextContext);
                this.currentContext.setOldContext(true);
                this.setNextContext(null);
                break;
        }

        ContextChange oldContextChange = this.changeContext;
        this.setChangeContext(ContextChange.NONE);
        this.simulationBarrier.arriveAndAwaitAdvance();

        if (startMissHandler)
            this.missHandler.start();

        if (oldContextChange != ContextChange.NONE) {
            this.setContextFinished(false);
            throw new ContextChangeException();
        }
    }

    @Override
    protected void modifiableFINExecution() {
        this.setContextFinished(true);

        if (this.waitingContext == null) { // checks if there is no other context waiting in execution

            ContextQueue contextQueue = this.simulationController.getContextQueue();

            // Tries to lock the context queue
            while (!contextQueue.tryLock()) {
                this.advanceBarriers();
            }

            this.setNextContext(contextQueue.getNextContext());
            if (this.nextContext == null) { // checks if there is no other context in the queue

                if (this.waitingContext == null && this.missHandler == null) {
                    this.executionFinished = true;

                } else {

                    while (this.waitingContext == null) {
                        // TODO Preguntar
                        this.advanceBarriers();
                    }

                    // If it finishes, bring the waiting context
                    this.setChangeContext(ContextChange.BRING_WAITING);

                }

            } else { // there is a context waiting in the queue
                this.setChangeContext(ContextChange.NEXT_CONTEXT);
            }

            contextQueue.unlock();

        } else { // there is a waiting context in execution
            this.setChangeContext(ContextChange.BRING_WAITING);
        }
    }

    @Override
    protected void quantumExpired() {
        this.setContextFinished(true);

        if (this.waitingContext == null) { // checks if there is no other context waiting in execution
            super.quantumExpired();

        } else { // there is someone else waiting in execution
            ContextQueue contextQueue = this.simulationController.getContextQueue();

            // Tries to lock the context queue
            while (!contextQueue.tryLock()) {
                this.advanceBarriers();
            }

            this.currentContext.setCurrentQuantum(SimulationConstants.INITIAL_QUANTUM);
            this.currentContext.setOldContext(false);
            contextQueue.pushContext(this.currentContext);
            contextQueue.unlock();

            this.setChangeContext(ContextChange.BRING_WAITING);
        }
    }

    @Override
    protected InstructionBlock getInstructionBlockFromCache(int nextInstructionBlockNumber, int nextInstructionCachePosition) {
        boolean solvedMiss = false;
        InstructionCachePosition instructionCachePosition = this.instructionCache.getInstructionCachePosition(
                nextInstructionCachePosition);

        while (!solvedMiss) {

            while (this.reservedInstructionCachePosition == nextInstructionCachePosition) {
                this.advanceClockCycle();
            }

            if (instructionCachePosition.getTag() != nextInstructionBlockNumber) { // miss
                solvedMiss = this.enterCacheMiss(MissType.INSTRUCTION, nextInstructionBlockNumber,
                        nextInstructionCachePosition, null, -1, -1);
            } else {
                solvedMiss = true;
            }
        }

        return instructionCachePosition.getInstructionBlock();
    }

    @Override
    protected void blockDataCachePosition(int dataCachePosition) {
        while (this.reservedDataCachePosition == dataCachePosition) {
            this.advanceClockCycle();
        }
        DataCachePosition cachePosition = this.dataCache.getDataCachePosition(dataCachePosition);
        while (!cachePosition.tryLock()) {
            this.advanceClockCycle();
        }
    }

    private boolean enterCacheMiss(MissType missType, int nextBlockNumber, int nextCachePosition,
                                   DataCachePosition dataCachePosition, int dataCachePositionOffset, int finalRegister) {
        boolean solvedMiss = true;

        if (this.waitingContext != null) { // there is a waiting context,

            this.setMissHandler(new MissHandler(this, this.currentContext, missType, this.simulationBarrier,
                    nextBlockNumber, nextCachePosition, dataCachePosition, dataCachePositionOffset, finalRegister));
            this.setChangeContext(ContextChange.BRING_WAITING_START_MISS_HANDLER);

        } else if (this.missHandler != null) { // miss handler is running, must wait till it finishes
            solvedMiss = false;

        } else { // miss handler is not running and there is no waiting context

            ContextQueue contextQueue = this.simulationController.getContextQueue();
            while (!contextQueue.tryLock())
                this.advanceClockCycle();

            this.setNextContext(contextQueue.getNextContext());
            if (this.nextContext != null) {

                this.setMissHandler(new MissHandler(this, this.currentContext, missType, this.simulationBarrier,
                        nextBlockNumber, nextCachePosition, dataCachePosition, dataCachePositionOffset, finalRegister));

                this.setChangeContext(ContextChange.NEXT_CONTEXT_START_MISS_HANDLER);

            } else {
                solvedMiss = this.solveMissLocally(missType, nextBlockNumber, nextCachePosition, dataCachePosition,
                        dataCachePositionOffset, finalRegister);
            }

            contextQueue.unlock();
        }

        this.advanceClockCycle();
        return solvedMiss;
    }

    public boolean solveMissLocally(MissType missType, int nextBlockNumber, int nextCachePosition,
                                    DataCachePosition dataCachePosition, int dataCachePositionOffset, int finalRegister) {

        switch (missType) {
            case INSTRUCTION:
                return this.solveInstructionMiss(nextBlockNumber, nextCachePosition);

            case LOAD:
                return this.solveDataLoadMiss(nextBlockNumber, dataCachePosition, dataCachePositionOffset, nextCachePosition,
                        finalRegister, this);

            case STORE:
                return this.solveDataStoreMiss(nextBlockNumber, dataCachePosition, dataCachePositionOffset, nextCachePosition,
                        finalRegister, this);

            default:
                throw new IllegalArgumentException("Invalid Miss Type in core zero.");
        }
    }

    public boolean solveInstructionMiss(int nextBlockNumber, int nextCachePosition) {

        this.setReservedInstructionCachePosition(nextCachePosition);
        this.instructionCache.getInstructionBlockFromMemory(nextBlockNumber, nextCachePosition, this);
        this.setReservedInstructionCachePosition(-1);
        return true;

    }

    public boolean solveDataLoadMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int dataCachePositionNumber, int finalRegister, AbstractThread callingThread) {
        if (this.getReservedDataCachePosition() != -1) {
            return false;
        } else {
            this.setReservedDataCachePosition(dataCachePositionNumber);
        }

        DataBus dataBus = this.dataCache.getDataBus();

        if (!dataBus.tryLock()) {
            callingThread.advanceClockCycle();
            return false;
        }

        this.advanceClockCycle();
        if (dataCachePosition.getTag() != blockNumber && dataCachePosition.getState() == CachePositionState.MODIFIED) {
            this.dataCache.writeBlockToMemory(dataCachePosition, this);
            dataCachePosition.setState(CachePositionState.SHARED);
        }

        int otherDataCachePositionNumber = this.calculateOtherDataCachePosition(blockNumber);
        DataCachePosition otherDataCachePosition = dataBus.getOtherCachePosition(this.coreNumber, otherDataCachePositionNumber);

        while (!otherDataCachePosition.tryLock()) {
            callingThread.advanceClockCycle();
        }
        callingThread.advanceClockCycle();

        if (otherDataCachePosition.getTag() == blockNumber && otherDataCachePosition.getState() == CachePositionState.MODIFIED) {
            this.dataCache.writeBlockToMemory(otherDataCachePosition, callingThread);
            dataCachePosition.setDataBlock(otherDataCachePosition.getDataBlock().clone());
            otherDataCachePosition.setState(CachePositionState.SHARED);
        } else {
            this.dataCache.getBlockFromMemory(blockNumber, dataCachePositionNumber, callingThread);
        }

        otherDataCachePosition.unlock();
        dataCachePosition.setTag(blockNumber);
        callingThread.getCurrentContext().getRegisters()[finalRegister] = dataCachePosition.getDataBlock().getWord(positionOffset);
        this.setReservedDataCachePosition(-1);
        dataBus.unlock();
        return true;
    }

    public boolean solveDataStoreMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int dataCachePositionNumber, int value, AbstractThread callingThread) {
        if (this.getReservedDataCachePosition() != -1) {
            callingThread.advanceClockCycle();
            return false;
        } else {
            this.setReservedDataCachePosition(dataCachePositionNumber);
        }

        DataBus dataBus = this.dataCache.getDataBus();

        if (!dataBus.tryLock()) {
            callingThread.advanceClockCycle();
            return false;
        }

        this.advanceClockCycle();
        if (dataCachePosition.getTag() != blockNumber && dataCachePosition.getState() == CachePositionState.MODIFIED) {
            this.dataCache.writeBlockToMemory(dataCachePosition, this);
            dataCachePosition.setState(CachePositionState.SHARED);
        }

        int otherDataCachePositionNumber = this.calculateOtherDataCachePosition(dataCachePosition.getTag());
        DataCachePosition otherDataCachePosition = dataBus
                .getOtherCachePosition(this.coreNumber, otherDataCachePositionNumber);
        while (!otherDataCachePosition.tryLock()) {
            callingThread.advanceClockCycle();
        }
        callingThread.advanceClockCycle();

        if (otherDataCachePosition.getTag() != blockNumber || otherDataCachePosition.getState() == CachePositionState.INVALID) {
            this.dataCache.getBlockFromMemory(blockNumber, dataCachePositionNumber, this);
            dataCachePosition.setState(CachePositionState.MODIFIED);
        } else {
            switch (otherDataCachePosition.getState()) {
                case MODIFIED:
                    otherDataCachePosition.setState(CachePositionState.INVALID);
                    dataBus.writeBlockToMemory(otherDataCachePosition.getDataBlock(), otherDataCachePosition.getTag());
                    dataCachePosition.setDataBlock(otherDataCachePosition.getDataBlock().clone());
                    dataCachePosition.setState(CachePositionState.MODIFIED);
                    break;

                case SHARED:
                    otherDataCachePosition.setState(CachePositionState.INVALID);
                    dataCachePosition.setDataBlock(dataBus.getMemoryBlock(blockNumber).clone());
                    dataCachePosition.setState(CachePositionState.MODIFIED);
                    break;
            }
        }

        dataCachePosition.getDataBlock().getWords()[positionOffset] = value;
        dataCachePosition.setTag(blockNumber);
        otherDataCachePosition.unlock();
        this.setReservedDataCachePosition(-1);
        dataBus.unlock();

        return true;
    }

    @Override
    protected boolean handleLoadMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int dataCachePositionNumber, int finalRegister) {
        return this.enterCacheMiss(MissType.LOAD, blockNumber, dataCachePositionNumber, dataCachePosition, positionOffset, finalRegister);
    }

    @Override
    protected boolean handleStoreMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int value) {
        int dataCachePositionNumber = this.calculateCachePosition(blockNumber, this.coreNumber);

        return this.enterCacheMiss(MissType.STORE, blockNumber, dataCachePositionNumber, dataCachePosition, positionOffset, value);
    }

    private void solvedMiss() {

        if (this.waitingContext.isOldContext() && !this.contextFinished &&
                this.changeContext != ContextChange.BRING_WAITING) {
            // If the context is the old one, the current context has not finished and someone isn-t trying to bring the
            // waiting context, put the context change in swap context.
            this.setChangeContext(ContextChange.SWAP_CONTEXTS);
        }

    }

    public void finishMissHandlerExecution() {
        this.solvedMiss();
        this.setMissHandler(null);
    }

    @Override
    public void printContext() {
        super.printContext();
    }

    public MissHandler getMissHandler() {
        return missHandler;
    }

    public synchronized void setMissHandler(MissHandler missHandler) {
        this.missHandler = missHandler;
    }

    public Context getWaitingContext() {
        return waitingContext;
    }

    public synchronized void setWaitingContext(Context waitingContext) {
        this.waitingContext = waitingContext;
    }

    public int getReservedDataCachePosition() {
        return reservedDataCachePosition;
    }

    public synchronized void setReservedDataCachePosition(int reservedDataCachePosition) {
        this.reservedDataCachePosition = reservedDataCachePosition;
    }

    public int getReservedInstructionCachePosition() {
        return reservedInstructionCachePosition;
    }

    public synchronized void setReservedInstructionCachePosition(int reservedInstructionCachePosition) {
        this.reservedInstructionCachePosition = reservedInstructionCachePosition;
    }

    public boolean isContextFinished() {
        return contextFinished;
    }

    public synchronized void setContextFinished(boolean contextFinished) {
        this.contextFinished = contextFinished;
    }
}
