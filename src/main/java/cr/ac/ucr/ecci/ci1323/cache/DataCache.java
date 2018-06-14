package cr.ac.ucr.ecci.ci1323.cache;

import cr.ac.ucr.ecci.ci1323.memory.Bus;
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

    public DataCachePosition[] getDataCachePositions() {
        return dataCachePositions;
    }

    public void setDataCachePositions(DataCachePosition[] dataCachePositions) {
        this.dataCachePositions = dataCachePositions;
    }

    public DataCachePosition getDataCachePosition(int position) {
        return this.dataCachePositions[position];
    }

    public Bus getDataBus() {
        return dataBus;
    }

    public void setDataBus(DataBus dataBus) {
        this.dataBus = dataBus;
    }
}
