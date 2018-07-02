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

    /**
     *  The thread dedicated to solve misses.
     */
    private volatile MissHandler missHandler;

    /**
     * Context waiting to enter to execution.
     */
    private volatile Context waitingContext;

    /**
     * An array for the data cache position reserved by another context.
     *
     * reservedDataCachePosition[0] -> The reserved data cache position
     * reservedDataCachePosition[1] -> The context that reserved the position.
     */
    private volatile int[] reservedDataCachePosition;

    /**
     * The instruction cache position reserved by another context.
     */
    private volatile int reservedInstructionCachePosition;

    /**
     * Indicates if the current context has finished.
     */
    private volatile boolean contextFinished;

    /**
     * Indicates if the current context is waiting for reservation to access cache position.
     */
    private volatile boolean contextWaitingForReservation;

    /**
     * Constructor that receives the necessary parameters for the simulation.
     *
     * @param simulationBarrier the simulation's barrier.
     * @param maxQuantum the maximum quantum of a context.
     * @param startingContext the first context to execute.
     * @param simulationController the controller of the simulation.
     * @param instructionBus the instruction bus.
     * @param dataBus the data bus.
     * @param coreNumber the core's number.
     */
    public CoreZero(Phaser simulationBarrier, int maxQuantum, Context startingContext,
                    SimulationController simulationController, InstructionBus instructionBus,
                    DataBus dataBus, int coreNumber) {

        super(simulationBarrier, maxQuantum, startingContext, simulationController,
                SimulationConstants.TOTAL_CORE_ZERO_CACHE_POSITIONS, instructionBus, dataBus, coreNumber);

        this.waitingContext = null;
        this.reservedDataCachePosition = new int[]{-1, -1};
        this.reservedInstructionCachePosition = -1;
    }

    /**
     * Method for when the thread runs.
     */
    @Override
    public void run() {
        super.executeCore();
    }

    /**
     * Changes the context depending on the value of the changeContext variable.
     */
    public void changeContext() {

        if (this.executionFinished && this.changeContext == ContextChange.SWAP_CONTEXTS) {
            // If the current context has finished or his quantum expired and the miss handler is requesting a swap
            // just advance and ignore the request until the current context requests to bring the waiting context.
            this.setChangeContext(ContextChange.NONE);
            this.simulationBarrier.arriveAndAwaitAdvance();
            return;
        }

        switch (this.changeContext) {
            case SWAP_CONTEXTS:
                if (!this.instructionFinished) {
                    // The SWAP is going to take the current context out in some random place of execution.
                    // So, for safety, if the instruction didn't finish decrement the PC and make it repeat the instruction.
                    this.currentContext.decrementPC();
                }
                Context tempContext = this.currentContext;
                this.setCurrentContext(this.waitingContext);
                this.setWaitingContext(tempContext);
                break;

            case BRING_WAITING:
                this.setCurrentContext(this.waitingContext);
                this.setWaitingContext(null);
                break;

            case NEXT_CONTEXT:
                this.setCurrentContext(this.nextContext);
                this.setNextContext(null);
                break;
        }

        ContextChange oldContextChange = this.changeContext; // Store the old context change for future use
        this.setChangeContext(ContextChange.NONE);

        this.simulationBarrier.arriveAndAwaitAdvance(); // Arrive to the barrier

        if (oldContextChange != ContextChange.NONE) {   // If there was a change of context

            // Reset the flags
            this.setContextFinished(false);
            this.setContextWaitingForReservation(false);
            this.setInstructionFinished(true);

            // Indicate that there was a change of context with an exception.
            throw new ContextChangeException();
        }
    }

    /**
     * The modifiable part of the execution of a FIN instruction for core zero.
     * It finishes the context taking the waiting context and miss handler into account.
     */
    @Override
    protected void modifiableFINExecution() {
        this.setContextFinished(true);

        if (this.waitingContext == null) { // checks if there is no other context waiting to enter to execution

            ContextQueue contextQueue = this.simulationController.getContextQueue();

            // Tries to lock the context queue
            while (!contextQueue.tryLock()) {
                this.advanceBarriers();
            }

            this.setNextContext(contextQueue.getNextContext());

            if (this.nextContext == null) { // There aren't any contexts left in the queue

                if (this.waitingContext == null && this.missHandler == null) { // There is no other context in the core solving a miss
                    this.executionFinished = true;

                } else { // There is a context solving the miss

                    while (this.waitingContext == null) { // Wait until it finishes the miss
                        this.advanceBarriers();
                    }

                    // When it finishes, bring the waiting context
                    this.setChangeContext(ContextChange.BRING_WAITING);
                    this.currentContext.setOldContext(true);

                }

            } else { // There is a context waiting in the queue
                this.setChangeContext(ContextChange.NEXT_CONTEXT);
            }

            contextQueue.unlock();

        } else { // There is a waiting context in execution
            this.setChangeContext(ContextChange.BRING_WAITING);
            this.currentContext.setOldContext(true);
        }
    }

    /**
     * Quantum expiration logic for the core zero.
     *
     * @see AbstractCore#quantumExpired()
     */
    @Override
    protected void quantumExpired() {
        this.setContextFinished(true);

        if (this.waitingContext == null) { // There is no other context waiting to enter to execution
            super.quantumExpired(); // Use the super class method.

        } else { // There is someone else waiting to execute
            ContextQueue contextQueue = this.simulationController.getContextQueue();

            // Tries to lock the context queue
            while (!contextQueue.tryLock()) {
                this.advanceBarriers();
            }

            this.currentContext.setCurrentQuantum(SimulationConstants.INITIAL_QUANTUM); // Reset quantum
            this.currentContext.setOldContext(false); // The context with the expired quantum  is not the old context
            contextQueue.pushContext(this.currentContext); // Put it in the queue
            contextQueue.unlock();

            this.setChangeContext(ContextChange.BRING_WAITING); // Bring the waiting context to execution
            this.currentContext.setOldContext(true); // Set it as the old context
        }
    }

    /**
     * Get the instruction block from cache
     *
     * @param nextInstructionBlockNumber the number of the instruction block to get.
     * @param nextInstructionCachePosition the number of the instruction cache position to get.
     * @return The instruction block from cache.
     */
    @Override
    protected InstructionBlock getInstructionBlockFromCache(int nextInstructionBlockNumber, int nextInstructionCachePosition) {
        boolean solvedMiss = false;
        InstructionCachePosition instructionCachePosition = this.instructionCache.getInstructionCachePosition(
                nextInstructionCachePosition);

        while (!solvedMiss) { // While the miss is not solved.

            // While there is some else with the position reserved, advance a clock cycle.
            while (this.reservedInstructionCachePosition == nextInstructionCachePosition) {
                this.advanceClockCycle();
            }

            if (instructionCachePosition.getTag() != nextInstructionBlockNumber) { // If the tag in the cache is different, there is a cache miss.
                solvedMiss = this.enterCacheMiss(MissType.INSTRUCTION, nextInstructionBlockNumber,
                        nextInstructionCachePosition, null, -1, -1);

            } else { // If not, it was a hit.
                solvedMiss = true;
            }
        }

        return instructionCachePosition.getInstructionBlock();
    }

    /**
     * Reserves and locks a data cache position.
     *
     * @param dataCachePositionNumber the number of the data cache position to reserve and lock.
     */
    @Override
    protected void lockDataCachePosition(int dataCachePositionNumber) {
        int contextNumber = this.currentContext.getContextNumber();
        int reservingContextNumber = this.getReservedDataCachePosition()[1];

        // While the cache position is already reserved by another context, just advance clock cycles
        while (reservingContextNumber != -1 && reservingContextNumber != contextNumber &&
                this.getReservedDataCachePosition()[0] == dataCachePositionNumber) {
            this.advanceClockCycle();
        }

        DataCachePosition cachePosition = this.dataCache.getDataCachePosition(dataCachePositionNumber);
        while (!cachePosition.tryLock()) {
            this.advanceClockCycle();
        }
    }

    /**
     * Handles the data cache miss for a load of the core zero. It calls the enter cache miss method.
     *
     * @param blockNumber the block number needed by the load.
     * @param dataCachePosition the data cache position needed by the load.
     * @param positionOffset the offset for the word needed from the cache.
     * @param dataCachePositionNumber the data cache position number needed by the load.
     * @param finalRegister the number of the register where the word should be loaded.
     * @return true if solved, false if not.
     */
    @Override
    protected boolean handleLoadMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int dataCachePositionNumber, int finalRegister) {
        return this.enterCacheMiss(MissType.LOAD, blockNumber, dataCachePositionNumber, dataCachePosition, positionOffset, finalRegister);
    }

    private boolean enterCacheMiss(MissType missType, int nextBlockNumber, int nextCachePosition,
                                   DataCachePosition dataCachePosition, int dataCachePositionOffset, int finalRegister) {
        boolean solvedMiss = true;

        if (this.waitingContext != null) { // there is a waiting context,

            this.setMissHandler(new MissHandler(this, this.currentContext, missType, this.simulationBarrier,
                    nextBlockNumber, nextCachePosition, dataCachePosition, dataCachePositionOffset, finalRegister));
            this.setChangeContext(ContextChange.BRING_WAITING);
            this.missHandler.start();

            if (dataCachePosition != null) {
                dataCachePosition.unlock();
            }

        } else if (this.missHandler != null) { // miss handler is running, must wait till it finishes
            this.setContextWaitingForReservation(true);
            solvedMiss = false;

            if (dataCachePosition != null) {
                dataCachePosition.unlock();
            }

        } else { // miss handler is not running and there is no waiting context

            ContextQueue contextQueue = this.simulationController.getContextQueue();
            while (!contextQueue.tryLock())
                this.advanceBarriers();

            this.setNextContext(contextQueue.getNextContext());
            if (this.nextContext != null) {

                this.setMissHandler(new MissHandler(this, this.currentContext, missType, this.simulationBarrier,
                        nextBlockNumber, nextCachePosition, dataCachePosition, dataCachePositionOffset, finalRegister));

                this.setChangeContext(ContextChange.NEXT_CONTEXT);
                this.missHandler.start();

                if (dataCachePosition != null) {
                    dataCachePosition.unlock();
                }

            } else {
                solvedMiss = this.solveMissLocally(missType, nextBlockNumber, nextCachePosition, dataCachePosition,
                        dataCachePositionOffset, finalRegister);
            }

            contextQueue.unlock();
        }

        this.advanceClockCycle();
        return solvedMiss;
    }

    /**
     * A switch for solving a miss locally depending on the miss type.
     *
     * @param missType the type of miss.
     * @param nextBlockNumber the number of the instruction block to handle the miss.
     * @param nextCachePosition the number of the instruction cache position where the block should be loaded.
     * @param dataCachePosition the data cache position needed by the miss.
     * @param dataCachePositionOffset the offset for the word needed to be loaded to cache.
     * @param finalRegister the number of the register where the word should be loaded or stored, if it applies.
     * @return true if solved, false if not.
     */
    private boolean solveMissLocally(MissType missType, int nextBlockNumber, int nextCachePosition,
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

            case STORE_HIT:
                return this.solveDataStoreHit(nextBlockNumber, dataCachePosition, nextCachePosition, dataCachePositionOffset,
                        finalRegister, this);

            default:
                throw new IllegalArgumentException("Invalid Miss Type in core zero.");
        }
    }

    /**
     * Solves an instruction miss by reserving the instruction cache position and bringing the instruction block from
     * memory.
     *
     * @param nextBlockNumber the number of the instruction block to bring from memory.
     * @param nextCachePosition the number of the instruction cache position where the block should be loaded.
     * @return true, because it will certainly load the block.
     */
    private boolean solveInstructionMiss(int nextBlockNumber, int nextCachePosition) {

        this.setReservedInstructionCachePosition(nextCachePosition);
        this.instructionCache.getInstructionBlockFromMemory(nextBlockNumber, nextCachePosition, this);
        this.setReservedInstructionCachePosition(-1);
        return true;

    }

    /**
     * Solves the data cache miss for a load instruction.
     *
     * @param blockNumber the block number needed by the load.
     * @param dataCachePosition the data cache position needed by the load.
     * @param positionOffset the offset for the word needed from the cache.
     * @param dataCachePositionNumber the data cache position number needed by the load.
     * @param finalRegister the number of the register where the word should be loaded.
     * @param callingThread the abstract thread calling to reserved the position.
     * @return true if solved, false if not.
     */
    public boolean solveDataLoadMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset,
                                     int dataCachePositionNumber, int finalRegister, AbstractThread callingThread) {

        int reservingContext = this.getReservedDataCachePosition()[1];
        int contextNumber = callingThread.currentContext.getContextNumber();

        // If there is another context with a reservation, release the locks, advance clock cycle and return false.
        if (reservingContext != -1 && reservingContext != contextNumber) {
            dataCachePosition.unlock();
            callingThread.advanceClockCycle();
            return false;
        }

        this.setReservedDataCachePosition(dataCachePositionNumber, contextNumber);

        DataBus dataBus = this.dataCache.getDataBus();

        // If the bus is already locked, release the locks, advance clock cycle and return false.
        if (!dataBus.tryLock()) {
            dataCachePosition.unlock();
            callingThread.advanceClockCycle();
            return false;
        }
        callingThread.advanceClockCycle();

        // If there is a modified block in the position, write it to memory
        if (dataCachePosition.getTag() != blockNumber && dataCachePosition.getState() == CachePositionState.MODIFIED) {
            this.dataCache.writeBlockToMemory(dataCachePosition, callingThread);
        }

        int otherDataCachePositionNumber = this.calculateOtherDataCachePosition(blockNumber);
        DataCachePosition otherDataCachePosition = dataBus.getOtherCachePosition(this.coreNumber, otherDataCachePositionNumber);

        // Try to lock the other cache position.
        while (!otherDataCachePosition.tryLock()) {
            callingThread.advanceClockCycle();
        }
        callingThread.advanceClockCycle();

        // If the other cache position is modified, write it to memory and bring it to the cache.
        if (otherDataCachePosition.getTag() == blockNumber && otherDataCachePosition.getState() == CachePositionState.MODIFIED) {
            this.dataCache.writeBlockToMemory(otherDataCachePosition, callingThread);
            this.dataCache.setPositionFromAnother(dataCachePosition, otherDataCachePosition);
            otherDataCachePosition.setState(CachePositionState.SHARED);

        } else { // If it wasn't modified, bring it from memory.
            this.dataCache.getBlockFromMemory(blockNumber, dataCachePositionNumber, callingThread);
        }

        // Release locks an reservations.
        otherDataCachePosition.unlock();
        callingThread.getCurrentContext().getRegisters()[finalRegister] = dataCachePosition.getDataBlock().getWord(positionOffset);
        dataBus.unlock();
        dataCachePosition.unlock();
        this.setReservedDataCachePosition(-1, -1);
        return true;
    }

    /**
     * Indicates if the reservation for the data cache can be made. If it can, reserves the position.
     *
     * @param dataCachePositionNumber the number of the data cache position to reserve.
     * @param callingThread the abstract thread calling to reserved the position.
     * @return true if reserved, false if not.
     */
    private boolean canMakeReservation(int dataCachePositionNumber, AbstractThread callingThread) {
        int reservingContext = this.getReservedDataCachePosition()[1];
        int contextNumber = callingThread.currentContext.getContextNumber();

        // If there is some other context that reserved a position
        if (reservingContext != -1 && reservingContext != contextNumber) {
            return false;
        }

        this.setReservedDataCachePosition(dataCachePositionNumber, callingThread.currentContext.getContextNumber());
        return true;
    }

    /**
     * Handles a store miss in the data cache for the core zero. It calls the enter cache miss method.
     *
     * @param blockNumber the block number needed by the store.
     * @param dataCachePosition the data cache position needed by the store.
     * @param positionOffset the offset for the word needed from the cache.
     * @param value the value to store.
     * @return true if solved, false if not.
     */
    @Override
    protected boolean handleStoreMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int value) {
        int dataCachePositionNumber = this.calculateCachePosition(blockNumber, this.coreNumber);

        return this.enterCacheMiss(MissType.STORE, blockNumber, dataCachePositionNumber, dataCachePosition, positionOffset, value);
    }

    /**
     * Solves a data cache miss for a store instruction.
     *
     * @param blockNumber the block number needed by the store.
     * @param dataCachePosition the data cache position needed by the store.
     * @param positionOffset the offset for the word needed from the cache.
     * @param dataCachePositionNumber the data cache position number needed by the store.
     * @param value the value to store.
     * @param callingThread the abstract thread calling to perform the store miss.
     * @return true if solved, false if not.
     */
    public boolean solveDataStoreMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset,
                                      int dataCachePositionNumber, int value, AbstractThread callingThread) {

        int reservingContext = this.getReservedDataCachePosition()[1];
        int contextNumber = callingThread.currentContext.getContextNumber();

        // If there is another context with a reservation, release the locks, advance clock cycle and return false.
        if (reservingContext != -1 && reservingContext != contextNumber) {
            dataCachePosition.unlock();
            callingThread.advanceClockCycle();
            return false;

        }

        this.setReservedDataCachePosition(dataCachePositionNumber, contextNumber);

        DataBus dataBus = this.dataCache.getDataBus();

        // If the bus is already locked, release the locks, advance clock cycle and return false.
        if (!dataBus.tryLock()) {
            dataCachePosition.unlock();
            callingThread.advanceClockCycle();
            return false;
        }
        callingThread.advanceClockCycle();

        // If there is a modified block in the position, write it to memory
        if (dataCachePosition.getTag() != blockNumber && dataCachePosition.getState() == CachePositionState.MODIFIED) {
            this.dataCache.writeBlockToMemory(dataCachePosition, callingThread);
        }

        int otherDataCachePositionNumber = this.calculateOtherDataCachePosition(blockNumber);
        DataCachePosition otherDataCachePosition = dataBus.getOtherCachePosition(this.coreNumber, otherDataCachePositionNumber);

        // Try to lock the other cache position.
        while (!otherDataCachePosition.tryLock()) {
            callingThread.advanceClockCycle();
        }
        callingThread.advanceClockCycle();

        // If the other data cache position does not have block or it is invalid, just bring it from memory
        if (otherDataCachePosition.getTag() != blockNumber || otherDataCachePosition.getState() == CachePositionState.INVALID) {
            this.dataCache.getBlockFromMemory(blockNumber, dataCachePositionNumber, callingThread);
            dataCachePosition.setState(CachePositionState.MODIFIED);

        } else { // If the block is in the other position then...

            switch (otherDataCachePosition.getState()) {
                case MODIFIED: // If it is modified, write it to memory, invalidate the other data cache position, bring it to local cache and set it to modified.
                    this.dataCache.writeBlockToMemory(otherDataCachePosition, callingThread);
                    otherDataCachePosition.setState(CachePositionState.INVALID);

                    this.dataCache.setPositionFromAnother(dataCachePosition, otherDataCachePosition);
                    dataCachePosition.setState(CachePositionState.MODIFIED);
                    break;

                case SHARED: // If it is shared, invalidate the other data cache position, bring it from memory and set it to modified
                    otherDataCachePosition.setState(CachePositionState.INVALID);

                    this.dataCache.getBlockFromMemory(blockNumber, dataCachePositionNumber, callingThread);
                    dataCachePosition.setState(CachePositionState.MODIFIED);
                    break;
            }
        }

        // Release locks and reservations.
        otherDataCachePosition.unlock();
        dataCachePosition.getDataBlock().getWords()[positionOffset] = value;
        dataBus.unlock();
        dataCachePosition.unlock();
        this.setReservedDataCachePosition(-1, -1);

        return true;
    }

    /**
     * Handles a store hit in the data cache for the core zero. It calls the enter cache miss method.
     *
     * @param blockNumber the block number needed by the store.
     * @param dataCachePosition the data cache position needed by the store.
     * @param positionOffset the offset for the word needed from the cache.
     * @param value the value to store.
     * @return true if stored, false if not.
     */
    @Override
    protected boolean handleStoreHit(int blockNumber, DataCachePosition dataCachePosition, int dataCachePositionNumber, int positionOffset, int value) {
        return this.enterCacheMiss(MissType.STORE_HIT, blockNumber, dataCachePositionNumber, dataCachePosition, positionOffset, value);
    }

    /**
     * Solves a data cache miss for a store instruction.
     *
     * @param blockNumber the block number needed by the store.
     * @param dataCachePosition the data cache position needed by the store.
     * @param positionOffset the offset for the word needed from the cache.
     * @param dataCachePositionNumber the data cache position number needed by the store.
     * @param value the value to store.
     * @param callingThread the abstract thread calling to perform the store miss.
     * @return true if solved, false if not.
     */
    public boolean solveDataStoreHit(int blockNumber, DataCachePosition dataCachePosition, int dataCachePositionNumber,
                                     int positionOffset, int value, AbstractThread callingThread) {

        // If the data cache position is modified, just store the word.
        if (dataCachePosition.getState() == CachePositionState.MODIFIED) {
            dataCachePosition.getDataBlock().getWords()[positionOffset] = value;
            dataCachePosition.unlock();
            callingThread.advanceClockCycle();
            return true;
        }

        // Try to make the reservation for the data cache position. If it was not possible, release the locks, advance a cycle and return false.
        if (!this.canMakeReservation(dataCachePositionNumber, callingThread)) {
            dataCachePosition.unlock();
            callingThread.advanceClockCycle();
            return false;
        }

        DataBus dataBus = this.dataCache.getDataBus();
        // If the bus is already locked, release the locks, advance a clock cycle and return false.
        if (!dataBus.tryLock()) {
            dataCachePosition.unlock();
            callingThread.advanceClockCycle();
            return false;
        }

        callingThread.advanceClockCycle();

        int otherDataCachePositionNumber = this.calculateOtherDataCachePosition(dataCachePosition.getTag());
        DataCachePosition otherCachePosition = dataBus.getOtherCachePosition(this.coreNumber, otherDataCachePositionNumber);

        // Try to lock the other data cache position.
        while (!otherCachePosition.tryLock()) {
            callingThread.advanceClockCycle();
        }
        callingThread.advanceClockCycle();

        // Only if the other data cache position state is shared, set it to invalid. If not, don't touch it.
        if (otherCachePosition.getTag() == blockNumber && otherCachePosition.getState() != CachePositionState.SHARED)
            otherCachePosition.setState(CachePositionState.INVALID);

        // Set the local data cache position to modified and store the value.
        dataCachePosition.setState(CachePositionState.MODIFIED);
        dataCachePosition.getDataBlock().getWords()[positionOffset] = value;

        // Release the locks and reservations.
        otherCachePosition.unlock();

        this.setReservedDataCachePosition(-1, -1);

        dataBus.unlock();
        dataCachePosition.unlock();

        return true;
    }

    /**
     * Indicates that a swap of contexts is needed when the miss handler finishes. It is made only if the context is the
     * oldest one, the context currently executing hasn't finished or is waiting for reservation or if it indicated that
     * the waiting context must be brought to execution.
     */
    private void solvedMiss() {

        if (this.waitingContext.isOldContext() && !this.contextFinished && !this.contextWaitingForReservation &&
                this.changeContext != ContextChange.BRING_WAITING) {
            // If the context is the old one, the current context has not finished and someone isn-t trying to bring the
            // waiting context, put the context change in swap context.
            this.setChangeContext(ContextChange.SWAP_CONTEXTS);
        }

    }

    /**
     * Logic for finishing the miss handler execution.
     */
    public void finishMissHandlerExecution() {
        this.solvedMiss();
        this.setMissHandler(null);
    }

    /**
     * Prints the current context by calling the super method.
     */
    @Override
    public void printContext() {
        super.printContext();
    }

    //----------------------------------------------------------------------------------------
    // Setters and Getters
    //----------------------------------------------------------------------------------------

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

    public int[] getReservedDataCachePosition() {
        return reservedDataCachePosition;
    }

    public synchronized void setReservedDataCachePosition(int reservedDataCachePosition, int contextNumber) {
        this.reservedDataCachePosition = new int[]{reservedDataCachePosition, contextNumber};
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

    public boolean isContextWaitingForReservation() {
        return contextWaitingForReservation;
    }

    public synchronized void setContextWaitingForReservation(boolean contextWaitingForReservation) {
        this.contextWaitingForReservation = contextWaitingForReservation;
    }
}
