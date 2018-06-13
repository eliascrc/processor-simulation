package cr.ac.ucr.ecci.ci1323.cache;

import cr.ac.ucr.ecci.ci1323.exceptions.TryLockException;
import cr.ac.ucr.ecci.ci1323.memory.DataBlock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents the position of a cache, with its respective words and tag.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public class DataCachePosition {

    private volatile int tag;
    private volatile DataBlock dataBlock;
    private volatile ReentrantLock cachePositionLock;
    private volatile CachePositionState cachePositionState;

    public DataCachePosition(int tag, DataBlock dataBlock, CachePositionState cachePositionState) {
        this.tag = tag;
        this.dataBlock = dataBlock;
        this.cachePositionState = cachePositionState;
        this.cachePositionLock = new ReentrantLock();
    }

    public synchronized boolean tryLock() {
        if (this.cachePositionLock.isHeldByCurrentThread())
            throw new TryLockException("The current thread already holds the data cache position queue lock.");

        return this.cachePositionLock.tryLock();
    }

    public synchronized void unlock() {
        if (!this.cachePositionLock.isHeldByCurrentThread())
            throw new TryLockException("The current thread cannot unlock the data cache position without holding the lock.");

        this.cachePositionLock.unlock();
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public DataBlock getDataBlock() {
        return dataBlock;
    }

    public void setDataBlock(DataBlock dataBlock) {
        this.dataBlock = dataBlock;
    }

    public ReentrantLock getCachePositionLock() {
        return cachePositionLock;
    }

    public void setCachePositionLock(ReentrantLock cachePositionLock) {
        this.cachePositionLock = cachePositionLock;
    }

    public CachePositionState getCachePositionState() {
        return cachePositionState;
    }

    public void setCachePositionState(CachePositionState cachePositionState) {
        this.cachePositionState = cachePositionState;
    }
}
