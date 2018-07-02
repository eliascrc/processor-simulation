package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.cache.CachePositionState;
import cr.ac.ucr.ecci.ci1323.cache.DataCachePosition;
import cr.ac.ucr.ecci.ci1323.cache.InstructionCachePosition;
import cr.ac.ucr.ecci.ci1323.commons.SimulationConstants;
import cr.ac.ucr.ecci.ci1323.controller.SimulationController;
import cr.ac.ucr.ecci.ci1323.context.Context;
import cr.ac.ucr.ecci.ci1323.exceptions.ContextChangeException;
import cr.ac.ucr.ecci.ci1323.memory.DataBus;
import cr.ac.ucr.ecci.ci1323.memory.Instruction;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBlock;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBus;

import java.util.concurrent.Phaser;

/**
 * Core one of the simulated processor.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public class CoreOne extends AbstractCore {

    /**
     * Class constructor
     * @param simulationBarrier the barrier that controls the simulation
     * @param maxQuantum the quantum that the user specified
     * @param startingContext the first context of the core
     * @param simulationController the controller of the simulation
     * @param instructionBus the instruction bus of the simulation
     * @param dataBus the data bus of the simulation
     * @param coreNumber the number of the core, usually 1
     */
    public CoreOne(Phaser simulationBarrier, int maxQuantum, Context startingContext,
                   SimulationController simulationController, InstructionBus instructionBus,
                   DataBus dataBus, int coreNumber) {
        super(simulationBarrier, maxQuantum, startingContext, simulationController,
                SimulationConstants.TOTAL_FIRST_CORE_CACHE_POSITIONS, instructionBus, dataBus, coreNumber);
    }

    /**
     * Starts the execution of the core
     */
    @Override
    public void run() {
        super.executeCore();
    }

    /**
     * Changes the context if necessary
     */
    @Override
    public void changeContext() {

        switch (this.changeContext) {

            case NEXT_CONTEXT:
                this.setCurrentContext(this.nextContext);
                this.currentContext.setOldContext(true);
                this.setNextContext(null);
                break;
        }

        ContextChange oldContextChange = this.changeContext;
        this.setChangeContext(ContextChange.NONE);
        this.simulationBarrier.arriveAndAwaitAdvance();

        if (oldContextChange != ContextChange.NONE)
            throw new ContextChangeException();

    }

    /**
     * Executes a load instruction
     * @param instruction the instruction that will be executed
     */
    @Override
    protected void executeLW(Instruction instruction) {
        super.executeLW(instruction);

    }

    /**
     * Tries to lock a data cache position
     * @param dataCachePositionNumber the number of the position that will be locked
     */
    @Override
    protected void lockDataCachePosition(int dataCachePositionNumber) {
        DataCachePosition cachePosition = this.dataCache.getDataCachePosition(dataCachePositionNumber);
        while (!cachePosition.tryLock()) {
            this.advanceClockCycle();
        }
    }

    /**
     * @param blockNumber the block number of the data block that will be attempted to retrieve
     * @param dataCachePosition the data cache position that will hold the retrieved data block
     * @param positionOffset the offset of the word that is being searched
     * @param dataCachePositionNumber the number of the data cache position that will get the block if the miss is
     *                                solved
     * @param finalRegister the register in which the word will be loaded
     * @return true if the miss was solved, false if not
     */
    @Override
    protected boolean handleLoadMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int dataCachePositionNumber, int finalRegister) {

        DataBus dataBus = this.dataCache.getDataBus();

        if (!dataBus.tryLock()) {
            dataCachePosition.unlock();
            this.advanceClockCycle();
            return false;
        }
        this.advanceClockCycle();

        if (dataCachePosition.getTag() != blockNumber && dataCachePosition.getState() == CachePositionState.MODIFIED) {
            this.dataCache.writeBlockToMemory(dataCachePosition, this);
        }

        int otherDataCachePositionNumber = this.calculateOtherDataCachePosition(blockNumber);
        DataCachePosition otherDataCachePosition = dataBus.getOtherCachePosition(this.coreNumber, otherDataCachePositionNumber);

        while (!otherDataCachePosition.tryLock()) {
            this.advanceClockCycle();
        }
        this.advanceClockCycle();

        if (otherDataCachePosition.getTag() == blockNumber && otherDataCachePosition.getState() == CachePositionState.MODIFIED) {
            this.dataCache.writeBlockToMemory(otherDataCachePosition, this);
            this.dataCache.setPositionFromAnother(dataCachePosition, otherDataCachePosition);
            otherDataCachePosition.setState(CachePositionState.SHARED);

        } else {
            this.dataCache.getBlockFromMemory(blockNumber, dataCachePositionNumber, this);
        }

        otherDataCachePosition.unlock();
        this.currentContext.getRegisters()[finalRegister] = dataCachePosition.getDataBlock().getWord(positionOffset);
        dataBus.unlock();
        dataCachePosition.unlock();
        return true;
    }

    /**
     * Handles a store hit it can fail and return false if the position was shared
     * @param blockNumber the block number in which the value must be stored
     * @param dataCachePosition the data cache position that may be modified
     * @param dataCachePositionNumber the number of the data cache position
     * @param positionOffset the offset that marks the word that may be modified
     * @param value the value that should be stored
     * @return true if the value could be stored, false if not
     */
    @Override
    protected boolean handleStoreHit(int blockNumber, DataCachePosition dataCachePosition, int dataCachePositionNumber, int positionOffset, int value) {
        if (dataCachePosition.getState() == CachePositionState.MODIFIED) {
            dataCachePosition.getDataBlock().getWords()[positionOffset] = value;
            dataCachePosition.unlock();
            this.advanceClockCycle();
            return true;
        }

        // Data Cache Position is shared
        DataBus dataBus = this.dataCache.getDataBus();
        if (!dataBus.tryLock()) {
            dataCachePosition.unlock();
            this.advanceClockCycle();
            return false;
        }

        this.advanceClockCycle();

        int otherDataCachePositionNumber = this.calculateOtherDataCachePosition(dataCachePosition.getTag());
        DataCachePosition otherCachePosition = dataBus.getOtherCachePosition(this.coreNumber, otherDataCachePositionNumber);

        while (!otherCachePosition.tryLock()) {
            this.advanceClockCycle();
        }

        this.advanceClockCycle();

        if (otherCachePosition.getTag() == blockNumber && otherCachePosition.getState() != CachePositionState.SHARED)
            otherCachePosition.setState(CachePositionState.INVALID);

        dataCachePosition.setState(CachePositionState.MODIFIED);
        dataCachePosition.getDataBlock().getWords()[positionOffset] = value;

        otherCachePosition.unlock();
        dataBus.unlock();
        dataCachePosition.unlock();

        return true;
    }

    /**
     * Called when a store miss is detected, tries to solve it
     * @param blockNumber the number of the block that caused the miss
     * @param dataCachePosition the data cache position tha
     * @param positionOffset the offset that marks the word that may be modified
     * @param value the value that will be stored in the block
     * @return true if it could solve the miss, false if not
     */
    @Override
    protected boolean handleStoreMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int value) {
        DataBus dataBus = this.dataCache.getDataBus();

        if (!dataBus.tryLock()) {
            dataCachePosition.unlock();
            this.advanceClockCycle();
            return false;
        }
        this.advanceClockCycle();

        if (dataCachePosition.getTag() != blockNumber && dataCachePosition.getState() == CachePositionState.MODIFIED) {
            this.dataCache.writeBlockToMemory(dataCachePosition, this);
        }

        int otherDataCachePositionNumber = this.calculateOtherDataCachePosition(blockNumber);
        DataCachePosition otherDataCachePosition = dataBus
                .getOtherCachePosition(this.coreNumber, otherDataCachePositionNumber);
        while (!otherDataCachePosition.tryLock()) {
            this.advanceClockCycle();
        }
        this.advanceClockCycle();

        int dataCachePositionNumber = this.calculateCachePosition(blockNumber, this.coreNumber);
        if (otherDataCachePosition.getTag() != blockNumber || otherDataCachePosition.getState() == CachePositionState.INVALID) {
            this.dataCache.getBlockFromMemory(blockNumber, dataCachePositionNumber, this);
            dataCachePosition.setState(CachePositionState.MODIFIED);

        } else {
            switch (otherDataCachePosition.getState()) {
                case MODIFIED:
                    this.dataCache.writeBlockToMemory(otherDataCachePosition, this);
                    otherDataCachePosition.setState(CachePositionState.INVALID);

                    this.dataCache.setPositionFromAnother(dataCachePosition, otherDataCachePosition);
                    dataCachePosition.setState(CachePositionState.MODIFIED);
                    break;

                case SHARED:
                    otherDataCachePosition.setState(CachePositionState.INVALID);

                    this.dataCache.getBlockFromMemory(blockNumber, dataCachePositionNumber, this);
                    dataCachePosition.setState(CachePositionState.MODIFIED);
                    break;
            }
        }

        otherDataCachePosition.unlock();
        dataCachePosition.getDataBlock().getWords()[positionOffset] = value;
        dataBus.unlock();
        dataCachePosition.unlock();

        return true;
    }

    /**
     * Gets an instruction block from the instruction cache of core 1
     * @param nextInstructionBlockNumber the block number of the instruction block that is needed
     * @param nextInstructionCachePosition the cache position in which the instruction block is (it may not be there,
     *                                     in that case a miss occurs)
     * @return the instruction block with the provided block number
     */
    @Override
    protected InstructionBlock getInstructionBlockFromCache(int nextInstructionBlockNumber, int nextInstructionCachePosition) {
        InstructionCachePosition instructionCachePosition = this.instructionCache.getInstructionCachePosition(
                nextInstructionCachePosition);

        if (instructionCachePosition.getTag() != nextInstructionBlockNumber)
            this.instructionCache.getInstructionBlockFromMemory(nextInstructionBlockNumber, nextInstructionCachePosition,
                    this);

        return instructionCachePosition.getInstructionBlock();
    }
}
