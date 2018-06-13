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
    private volatile ReentrantLock cachePositionLock;

    public InstructionCachePosition(int tag, InstructionBlock instructionBlock) {
        this.tag = tag;
        this.instructionBlock = instructionBlock;
        this.cachePositionLock = new ReentrantLock();
    }

    /**
     * Synchronized method for trying to lock the instruction cache position lock
     * @return false if not locked, true if locked
     */
    public synchronized boolean tryLock() {
        if (this.cachePositionLock.isHeldByCurrentThread())
            throw new TryLockException("The instruction cache position is already hold by this thread.");

        return this.cachePositionLock.tryLock();
    }

    /**
     * Synchronized method for trying to unlock the instruction cache position
     */
    public synchronized void unlock() {
        if (!this.cachePositionLock.isHeldByCurrentThread())
            throw new TryLockException("The current thread cannot unlock the instruction cache position without holding the lock.");

        this.cachePositionLock.unlock();
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
