package cr.ac.ucr.ecci.ci1323.cache;

import cr.ac.ucr.ecci.ci1323.memory.Bus;

/**
 * Represents the position of a data cache, with its respective block and tag.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public class DataCache {

    private DataCachePosition[] dataCachePositions;
    private Bus dataBus;

    /**
     * Class constructor, initializes the cache with the respective amount of positions and sets their states
     * to invalid.
     * @param dataBus
     * @param cacheSize
     */
    public DataCache(Bus dataBus, int cacheSize) {
        this.dataCachePositions = new DataCachePosition[cacheSize];
        for (int cachePosition = 0; cachePosition < this.dataCachePositions.length; cachePosition++) {
            this.dataCachePositions[cachePosition].setCachePositionState(CachePositionState.INVALID);
        }
        this.dataBus = dataBus;
    }

    public DataCachePosition[] getDataCachePositions() {
        return dataCachePositions;
    }

    public void setDataCachePositions(DataCachePosition[] dataCachePositions) {
        this.dataCachePositions = dataCachePositions;
    }

    public Bus getDataBus() {
        return dataBus;
    }

    public void setDataBus(Bus dataBus) {
        this.dataBus = dataBus;
    }
}
