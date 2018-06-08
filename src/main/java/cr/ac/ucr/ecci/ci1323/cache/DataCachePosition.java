package main.java.cr.ac.ucr.ecci.ci1323.cache;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents the position of a cache, with its respective words and tag.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public class DataCachePosition {

    private int tag;
    private DataBlock dataBlock;
    private ReentrantLock cachePositionLock;
    private CachePositionState cachePositionState;

    public DataCachePosition(int tag, DataBlock dataBlock, CachePositionState cachePositionState) {
        this.tag = tag;
        this.dataBlock = dataBlock;
        this.cachePositionState = cachePositionState;
    }

    public int[] getWords() {
        return words;
    }

    public void setWords(int[] words) {
        this.words = words;
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
}
