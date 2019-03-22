package com.github.rodbate.uid.common;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * User: jiangsongsong
 * Date: 2019/1/4
 * Time: 16:06
 */
public final class ResetableCountdownLatch {

    private final Sync sync;

    public ResetableCountdownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        }
        this.sync = new Sync(count);
    }

    /**
     * await
     *
     * @throws InterruptedException ex
     */
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * await for timeout
     *
     * @param timeout timeout
     * @param unit    unit
     * @return true or false
     * @throws InterruptedException e
     */
    public boolean await(long timeout, TimeUnit unit)
        throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * count down
     */
    public void countDown() {
        sync.releaseShared(1);
    }

    /**
     * @return count
     */
    public long getCount() {
        return sync.getCount();
    }

    /**
     * reset
     */
    public void reset() {
        sync.reset();
    }

    /**
     * @return to string
     */
    public String toString() {
        return super.toString() + "[Count = " + sync.getCount() + "]";
    }

    private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -1L;

        private final int initialCount;

        Sync(int count) {
            this.initialCount = count;
            setState(count);
        }

        int getCount() {
            return getState();
        }

        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }

        protected boolean tryReleaseShared(int releases) {
            // Decrement count; signal when transition to zero
            for (; ; ) {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c - 1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }

        void reset() {
            setState(this.initialCount);
        }
    }
}
