package cr.ac.ucr.ecci.ci1323.cache;

import cr.ac.ucr.ecci.ci1323.exceptions.TryLockException;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBlock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents the position of a instruction cache, with its respective block and tag.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public class InstructionCachePosition {

    private volatile int tag;
    private volatile InstructionBlock instructionBlock;

    public InstructionCachePosition(int tag, InstructionBlock instructionBlock) {
        this.tag = tag;
        this.instructionBlock = instructionBlock;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public InstructionBlock getInstructionBlock() {
        return instructionBlock;
    }

    public void setInstructionBlock(InstructionBlock instructionBlock) {
        this.instructionBlock = instructionBlock;
    }
}
