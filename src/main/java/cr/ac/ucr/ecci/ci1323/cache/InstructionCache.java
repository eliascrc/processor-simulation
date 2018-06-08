package main.java.cr.ac.ucr.ecci.ci1323.cache;

/**
 * Instruction cache used by both cores, which contains its positions and instruction bus for communication
 * with the main memory.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public class InstructionCache {

    private InstructionCachePosition[] instructionCachePositions;
    private Bus instructionBus;

    public InstructionCache(InstructionCachePosition[] instructionCachePositions, Bus instructionBus) {
        this.instructionCachePositions = instructionCachePositions;
        this.instructionBus = instructionBus;
    }

    public InstructionCachePosition[] getInstructionCachePositions() {
        return instructionCachePositions;
    }

    public void setInstructionCachePositions(InstructionCachePosition[] instructionCachePositions) {
        this.instructionCachePositions = instructionCachePositions;
    }

    public Bus getInstructionBus() {
        return instructionBus;
    }

    public void setInstructionBus(Bus instructionBus) {
        this.instructionBus = instructionBus;
    }
}
