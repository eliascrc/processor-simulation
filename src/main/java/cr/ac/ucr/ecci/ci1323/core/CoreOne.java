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

    public CoreOne(Phaser simulationBarrier, int maxQuantum, Context startingContext,
                   SimulationController simulationController, InstructionBus instructionBus,
                   DataBus dataBus, int coreNumber) {
        super(simulationBarrier, maxQuantum, startingContext, simulationController,
                SimulationConstants.TOTAL_FIRST_CORE_CACHE_POSITIONS, instructionBus, dataBus, coreNumber);
    }

    @Override
    public void run() {
        super.executeCore();
    }

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

    @Override
    protected void executeLW(Instruction instruction) {
        super.executeLW(instruction);

    }

    @Override
    protected boolean handleLoadMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int dataCachePositionNumber, int finalRegister) {

        DataBus dataBus = this.dataCache.getDataBus();

        if (!dataBus.tryLock()) {
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
            dataCachePosition.setDataBlock(otherDataCachePosition.getDataBlock().clone());
            otherDataCachePosition.setState(CachePositionState.SHARED);
        } else {
            this.dataCache.getBlockFromMemory(blockNumber, dataCachePositionNumber, this);
        }

        otherDataCachePosition.unlock();
        dataCachePosition.setTag(blockNumber);
        this.currentContext.getRegisters()[finalRegister] = dataCachePosition.getDataBlock().getWord(positionOffset);
        dataBus.unlock();
        return true;
    }

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
            dataCachePosition.setState(CachePositionState.SHARED);
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
        otherDataCachePosition.unlock();
        dataCachePosition.setTag(blockNumber);
        dataBus.unlock();

        return true;
    }

    @Override
    protected void blockDataCachePosition(int dataCachePosition) {
        DataCachePosition cachePosition = this.dataCache.getDataCachePosition(dataCachePosition);
        while (!cachePosition.tryLock()) {
            this.advanceClockCycle();
        }
    }


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
