package cr.ac.ucr.ecci.ci1323.memory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Bus {

    private ReentrantLock busLock;

    Bus() {
        this.busLock = new ReentrantLock();
    }

    protected boolean tryLock() {
        return true;
    }

    protected Lock getLock() {
        return this.busLock;
    }

}
