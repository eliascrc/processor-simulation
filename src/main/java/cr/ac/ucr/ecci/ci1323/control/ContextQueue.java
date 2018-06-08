package cr.ac.ucr.ecci.ci1323.control;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class ContextQueue {

    private ReentrantLock contextQueueLock;
    private Queue<Context> contextQueue;

    public ContextQueue() {
        this.contextQueueLock = new ReentrantLock();
        this.contextQueue = new LinkedList<Context>();
    }

    public synchronized boolean tryLock() {
        return contextQueueLock.tryLock();
    }

    public synchronized Context getNextContext() {
        return this.contextQueue.poll();
    }

    public void pushContext(Context context) {
        this.contextQueue.add(context);
    }

}
