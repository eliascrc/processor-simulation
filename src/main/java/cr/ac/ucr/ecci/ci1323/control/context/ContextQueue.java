package cr.ac.ucr.ecci.ci1323.control.context;

import cr.ac.ucr.ecci.ci1323.support.ContextQueueLockException;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Queue of contexts that the cores will use for getting the next context to execute.
 *
 * @author Elias Calderon
 */
public class ContextQueue {

    /**
     * Lock for the context queue.
     */
    private volatile ReentrantLock contextQueueLock;

    /**
     * The queue of contexts.
     */
    private volatile Queue<Context> contextQueue;

    /**
     * Constructor that initializes the lock and the context queue
     */
    public ContextQueue() {
        this.contextQueueLock = new ReentrantLock();
        this.contextQueue = new LinkedList<Context>();
    }

    /**
     * Synchronized method for trying to lock the context queue
     * @return false if not locked, true if locked
     */
    public synchronized boolean tryLock() {
        if (this.contextQueueLock.isHeldByCurrentThread())
            throw new ContextQueueLockException("The current thread already holds the context queue lock.");

        return contextQueueLock.tryLock();
    }

    /**
     * Synchronized method for trying to unlock the context queue
     */
    public synchronized void unlock() {
        if (!this.contextQueueLock.isHeldByCurrentThread())
            throw new ContextQueueLockException("The current thread cannot unlock the queue without holding the lock.");

        contextQueueLock.unlock();
    }

    /**
     * Synchronized method for getting the next context in the queue
     * @return
     */
    public synchronized Context getNextContext() {
        if (!this.contextQueueLock.isHeldByCurrentThread())
            throw new ContextQueueLockException("The current thread tried to get a new context without locking the queue");

        return this.contextQueue.poll();
    }

    public void pushContext(Context context) {
        this.contextQueue.add(context);
    }

}