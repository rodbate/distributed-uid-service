package com.github.rodbate.uid.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: jiangsongsong
 * Date: 2019/1/4
 * Time: 16:06
 */
public abstract class AbstractServiceThread implements Runnable {

    private static final long JOIN_TIME = 90 * 1000;
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final Thread thread;
    private final ResetableCountdownLatch waitPoint = new ResetableCountdownLatch(1);
    private final AtomicBoolean hasNotified = new AtomicBoolean(false);
    private volatile boolean stopped = false;

    public AbstractServiceThread() {
        this.thread = new Thread(this, this.getServiceName());
    }

    /**
     * start service thread
     */
    public void start() {
        this.thread.start();
    }

    /**
     * shutdown service thread
     */
    public void shutdown() {
        this.shutdown(false);
    }

    /**
     * shutdown service thread
     *
     * @param interrupt interrupt or not
     */
    public void shutdown(final boolean interrupt) {
        this.stopped = true;
        log.info("shutdown thread " + this.getServiceName() + " interrupt " + interrupt);

        if (hasNotified.compareAndSet(false, true)) {
            waitPoint.countDown(); // notify
        }

        try {
            if (interrupt) {
                this.thread.interrupt();
            }

            long beginTime = System.currentTimeMillis();
            if (!this.thread.isDaemon()) {
                this.thread.join(this.getJointime());
            }
            long elapsedTime = System.currentTimeMillis() - beginTime;
            log.info("join thread " + this.getServiceName() + " elapsed time(ms) " + elapsedTime + " " + this.getJointime());
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
        }
    }

    /**
     * get thread join time
     *
     * @return thread join time
     */
    public long getJointime() {
        return JOIN_TIME;
    }

    /**
     * stop service thread
     */
    public void stop() {
        this.stop(false);
    }

    /**
     * stop service thread
     *
     * @param interrupt interrupt or not
     */
    public void stop(final boolean interrupt) {
        this.stopped = true;
        log.info("stop thread " + this.getServiceName() + " interrupt " + interrupt);

        if (hasNotified.compareAndSet(false, true)) {
            waitPoint.countDown(); // notify
        }

        if (interrupt) {
            this.thread.interrupt();
        }
    }

    /**
     * make stop signal true
     */
    public void makeStop() {
        this.stopped = true;
        log.info("makestop thread " + this.getServiceName());
    }

    /**
     * wake up service thread
     */
    public void wakeup() {
        if (hasNotified.compareAndSet(false, true)) {
            waitPoint.countDown(); // notify
        }
    }

    protected void waitForRunning(long intervalMills) {
        if (hasNotified.compareAndSet(true, false)) {
            this.onWaitEnd();
            return;
        }

        //entry to wait
        waitPoint.reset();

        try {
            waitPoint.await(intervalMills, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
        } finally {
            hasNotified.set(false);
            this.onWaitEnd();
        }
    }

    protected void onWaitEnd() {
    }

    /**
     * the service thread is stopped or not
     *
     * @return stopped
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * service name
     *
     * @return service name
     */
    public abstract String getServiceName();
}
