package cr.ac.ucr.ecci.ci1323.cache;

import cr.ac.ucr.ecci.ci1323.core.AbstractCore;
import cr.ac.ucr.ecci.ci1323.core.AbstractThread;
import cr.ac.ucr.ecci.ci1323.core.CoreZero;
import cr.ac.ucr.ecci.ci1323.core.MissHandler;
import cr.ac.ucr.ecci.ci1323.memory.Bus;
import cr.ac.ucr.ecci.ci1323.memory.Instruction;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBus;

/**
 * Instruction cache used by both cores, which contains its positions and instruction bus for communication
 * with the main memory.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public class InstructionCache {

    private volatile InstructionCachePosition[] instructionCachePositions;
    private volatile InstructionBus instructionBus;

    public InstructionCache(InstructionBus instructionBus, int cacheSize) {
        this.instructionBus = instructionBus;

        this.instructionCachePositions = new InstructionCachePosition[cacheSize];

        for (int i = 0; i < this.instructionCachePositions.length; i++) {
            this.instructionCachePositions[i] = new InstructionCachePosition(-1, null);
        }
    }

    public InstructionCachePosition[] getInstructionCachePositions() {
        return instructionCachePositions;
    }

    public void setInstructionCachePositions(InstructionCachePosition[] instructionCachePositions) {
        this.instructionCachePositions = instructionCachePositions;
    }

    public InstructionCachePosition getInstructionCachePosition(int position) {
        return this.instructionCachePositions[position];
    }

    public InstructionBus getInstructionBus() {
        return instructionBus;
    }

    public void setInstructionBus(InstructionBus instructionBus) {
        this.instructionBus = instructionBus;
    }

    public void getInstructionBlockFromMemory(int nextInstructionBlockNumber, int nextInstructionPositionNumber,
                                              AbstractThread callingThread) {

        while (!this.instructionBus.tryLock()) {
            callingThread.advanceClockCycle();
        }
        callingThread.advanceClockCycle();
        // Advances 40 clock cycles
        for (int i = 0; i < 40; i++) {
            callingThread.advanceClockCycle();
        }

        this.instructionCachePositions[nextInstructionPositionNumber].setInstructionBlock(
                this.instructionBus.getInstructionBlock(nextInstructionBlockNumber));
        this.instructionCachePositions[nextInstructionPositionNumber].setTag(nextInstructionBlockNumber);

        this.instructionBus.unlock();
    }

}
