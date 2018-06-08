package cr.ac.ucr.ecci.ci1323.memory;

import java.util.concurrent.locks.Lock;

public abstract class Bus {

    private Lock busLock;

    protected boolean tryLock() {
        return true;
    }

}
