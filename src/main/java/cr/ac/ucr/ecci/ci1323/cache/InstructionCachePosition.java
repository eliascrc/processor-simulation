package cr.ac.ucr.ecci.ci1323.cache;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents the position of a instruction cache, with its respective block and tag.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public class InstructionCachePosition {

    private int tag;
    private InstructionBlock instructionBlock;
    private ReentrantLock cachePositionLock;

    public InstructionCachePosition(int tag, InstructionBlock instructionBlock) {
        this.tag = tag;
        this.instructionBlock = instructionBlock;
        this.cachePositionLock = new ReentrantLock();
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
