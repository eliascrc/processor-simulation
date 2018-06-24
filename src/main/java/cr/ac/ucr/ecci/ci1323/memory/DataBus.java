package cr.ac.ucr.ecci.ci1323.memory;

import cr.ac.ucr.ecci.ci1323.cache.DataCache;
import cr.ac.ucr.ecci.ci1323.cache.DataCachePosition;
import cr.ac.ucr.ecci.ci1323.commons.SimulationConstants;

public class DataBus extends Bus {

    private volatile DataCache coreZeroCache;
    private volatile DataCache coreOneCache;
    private volatile DataBlock dataMemory[];

    public DataBus(DataBlock[] dataMemory) {
        super();
        this.dataMemory = dataMemory;
    }

    public DataCachePosition getOtherCachePosition(int coreNum, int position) {
        if (coreNum == 1) {
            return this.coreZeroCache.getDataCachePositions()[position];
        } else if (coreNum == 0) {
            return this.coreOneCache.getDataCachePositions()[position];
        } else {
            throw new IllegalArgumentException("Invalid core number: " + coreNum);
        }
    }

    public DataBlock getMemoryBlock(int index) {
        return this.dataMemory[index];
    }

    public void writeBlockToMemory(DataBlock newDataBlock, int tag) {

        DataBlock dataBlock = this.dataMemory[tag];
        for (int i = 0; i < SimulationConstants.WORDS_PER_DATA_BLOCK; i++) {
            dataBlock.getWords()[i] = newDataBlock.getWords()[i];
        }

    }

    public void printMemory() {
        System.out.print("{ ");
        for (DataBlock dataBlock : this.dataMemory) {
            dataBlock.printBlock();
        }
        System.out.println(" }");
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
