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

    /**
     * Class constructor
     * @param tag the tag of the cache position
     * @param dataBlock the data block of the cache position
     * @param cachePositionState the state of the cache position
     */
    public DataCachePosition(int tag, DataBlock dataBlock, CachePositionState cachePositionState) {
        this.tag = tag;
        this.dataBlock = dataBlock;
        this.cachePositionState = cachePositionState;
        this.cachePositionLock = new ReentrantLock();
    }

    /**
     * Used for trying to get the lock of the cache position throws an exception if it already had it
     * @return true if it did, false if not
     */
    public synchronized boolean tryLock() {
        if (this.cachePositionLock.isHeldByCurrentThread())
            throw new TryLockException("The current thread already holds the data cache position queue lock.");

        return this.cachePositionLock.tryLock();
    }

    /**
     * Unlocks the lock of the data cache position
     */
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

    public CachePositionState getState() {
        return cachePositionState;
    }

    public void setState(CachePositionState cachePositionState) {
        this.cachePositionState = cachePositionState;
    }

    /**
     * Prints the information of the data cache position
     */
    public void print() {
        System.out.print("Etiqueta " + this.tag + ", Estado: " + this.cachePositionState + ", Bloque de Datos: { ");

        if (this.dataBlock == null)
            System.out.print("vacio");
        else
            this.dataBlock.printBlock();
        System.out.println(" }");
    }
}
