package cr.ac.ucr.ecci.ci1323.cache;

/**
 * Represents the position of a data cache, with its respective block and tag.
 *
 * @author Josué León Sarkis, Elías Calderón, Daniel Montes de Oca
 */
public class DataCache {

    private DataCachePosition[] dataCachePositions;
    private Bus dataBus;

    public DataCache(DataCachePosition[] dataCachePositions, Bus dataBus) {
        this.dataCachePositions = dataCachePositions;
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
