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
    protected void executeSW(Instruction instruction) {

    }

    @Override
    protected void executeLW(Instruction instruction) {
        super.executeLW(instruction);

    }

    @Override
    protected boolean handleLoadMiss(int blockNumber, DataCachePosition dataCachePosition, int positionOffset) {
        if (!this.dataCache.getDataBus().tryLock()) {
            dataCachePosition.unlock();
            this.advanceClockCycle();
            return false;
        }

        this.advanceClockCycle();
        if ()

        return true;

    }

    @Override
    protected boolean handleStoreHit(int blockNumber, DataCachePosition dataCachePosition, int positionOffset, int value) {
        if (dataCachePosition.getCachePositionState() == CachePositionState.MODIFIED) {
            dataCachePosition.getDataBlock().getWords()[positionOffset] = value;
            return true;
        }

        // dataCachePosition is shared
        if (!this.dataCache.getDataBus().tryLock()) {
            dataCachePosition.unlock();
            return false;
        }

        while ()

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

    public void changeContext() {

    }
}
