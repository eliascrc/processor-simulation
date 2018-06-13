package cr.ac.ucr.ecci.ci1323.memory;

import cr.ac.ucr.ecci.ci1323.exceptions.TryLockException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Bus {

    private ReentrantLock busLock;

    Bus() {
        this.busLock = new ReentrantLock();
    }

    protected synchronized boolean tryLock() {
        if(this.busLock.isHeldByCurrentThread())
            throw new TryLockException();

        return this.busLock.tryLock();
    }

}
