package cr.ac.ucr.ecci.ci1323.memory;

import cr.ac.ucr.ecci.ci1323.cache.DataCache;
import cr.ac.ucr.ecci.ci1323.cache.DataCachePosition;

public class DataBus extends Bus {

    private volatile DataCache coreZeroCache;
    private volatile DataCache coreOneCache;
    private volatile DataBlock dataMemory[];

    public DataBus(DataBlock[] dataMemory) {
        super();
        this.dataMemory = dataMemory;
    }

    public DataCachePosition getCachePosition(int coreNum, int position) {
        if (coreNum == 0) {
            return this.coreZeroCache.getDataCachePositions()[position];
        } else if (coreNum == 1) {
            return this.coreOneCache.getDataCachePositions()[position];
        } else {
            throw new IllegalArgumentException("Invalid core number: " + coreNum);
        }
    }

    public DataBlock getMemoryBlock(int index) {
        return this.dataMemory[index];
    }

    public int getMemoryBlockData(int blockNumber, int offset) {
        return this.getMemoryBlock(blockNumber).getWord(offset);
    }

    public void setCoreZeroCache(DataCache coreZeroCache) {
        this.coreZeroCache = coreZeroCache;
    }

    public void setCoreOneCache(DataCache coreOneCache) {
        this.coreOneCache = coreOneCache;
    }

    public DataBlock[] getDataMemory() {
        return dataMemory;
    }

}
