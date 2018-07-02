package cr.ac.ucr.ecci.ci1323.cache;

import cr.ac.ucr.ecci.ci1323.core.AbstractThread;
import cr.ac.ucr.ecci.ci1323.memory.DataBus;

/**
 * Represents the position of a data cache, with its respective block and tag.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public class DataCache {

    private volatile DataCachePosition[] dataCachePositions;
    private volatile DataBus dataBus;

    /**
     * Class constructor, initializes the cache with the respective amount of positions and sets their states
     * to invalid.
     * @param dataBus
     * @param cacheSize
     */
    public DataCache(DataBus dataBus, int cacheSize) {
        this.dataCachePositions = new DataCachePosition[cacheSize];

        for (int i = 0; i < this.dataCachePositions.length; i++) {
            this.dataCachePositions[i] = new DataCachePosition(-1, null, CachePositionState.INVALID);
        }

        this.dataBus = dataBus;
    }

    /**
     * Writes a data block to memory, includes the wait for the 40 clock ticks
     * @param dataCachePosition the data cache position that has the block
     * @param callingThread the thread that is calling this method
     */
    public void writeBlockToMemory (DataCachePosition dataCachePosition, AbstractThread callingThread) {

        // Advances 40 clock cycles
        for (int i = 0; i < 40; i++) {
            callingThread.advanceClockCycle();
        }

        this.getDataBus().writeBlockToMemory(dataCachePosition.getDataBlock(), dataCachePosition.getTag());
        dataCachePosition.setState(CachePositionState.SHARED);

    }

    /**
     * Gets a block from memory, includes the wait for the 40 clock ticks
     * @param dataBlockNumber the number of the memory block that will be retrieved
     * @param dataPositionNumber the number of data cache position that will hold the retrieved block
     * @param callingThread the thread that is calling this method
     */
    public void getBlockFromMemory(int dataBlockNumber, int dataPositionNumber, AbstractThread callingThread) {

        // Advances 40 clock cycles
        for (int i = 0; i < 40; i++) {
            callingThread.advanceClockCycle();
        }

        this.dataCachePositions[dataPositionNumber].setDataBlock(this.dataBus.getMemoryBlock(dataBlockNumber).clone());
        this.dataCachePositions[dataPositionNumber].setState(CachePositionState.SHARED);
        this.dataCachePositions[dataPositionNumber].setTag(dataBlockNumber);
    }

    /**
     * Gets a cache position from the cache of the other core
     * @param dataCachePosition the data cache position of the current core, its data block will be overwritten
     * @param otherDataCachePosition the data cache position of the other core
     */
    public void setPositionFromAnother(DataCachePosition dataCachePosition, DataCachePosition otherDataCachePosition) {
        dataCachePosition.setDataBlock(otherDataCachePosition.getDataBlock().clone());
        dataCachePosition.setState(CachePositionState.SHARED);
        dataCachePosition.setTag(otherDataCachePosition.getTag());
    }

    public DataCachePosition[] getDataCachePositions() {
        return dataCachePositions;
    }

    public void setDataCachePositions(DataCachePosition[] dataCachePositions) {
        this.dataCachePositions = dataCachePositions;
    }

    public DataCachePosition getDataCachePosition(int position) {
        return this.dataCachePositions[position];
    }

    public DataBus getDataBus() {
        return dataBus;
    }

    public void setDataBus(DataBus dataBus) {
        this.dataBus = dataBus;
    }
}
