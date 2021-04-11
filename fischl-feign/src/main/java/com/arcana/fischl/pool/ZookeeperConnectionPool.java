package com.arcana.fischl.pool;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ZookeeperConnectionPool implements ConnectionPool<ZooKeeper> {

    private Integer maxActive;

    private Long maxWait;

    private LinkedBlockingQueue<ZooKeeper> idle = new LinkedBlockingQueue<>();

    private LinkedBlockingQueue<ZooKeeper> busy = new LinkedBlockingQueue<>();

    private AtomicInteger activeSize = new AtomicInteger(0);

    private AtomicBoolean isClosed = new AtomicBoolean(false);

    private AtomicInteger createCount = new AtomicInteger(0);

    private static ThreadLocal<CountDownLatch> latchThreadLocal = ThreadLocal.withInitial(() -> new CountDownLatch(1));

    public ZookeeperConnectionPool(Integer maxActive, Long maxWait) {
        this.init(maxActive, maxWait);
    }

    @Override
    public void init(Integer maxActive, Long maxWait) {
        this.maxActive = maxActive;
        this.maxWait = maxWait;
    }

    @Override
    public ZooKeeper getResource() throws Exception {
        ZooKeeper zooKeeper;
        Long nowTime = System.currentTimeMillis();
        final CountDownLatch countDownLatch = latchThreadLocal.get();

        if ((zooKeeper = idle.poll()) == null) {

            if (activeSize.get() < maxActive) {

                if (activeSize.incrementAndGet() <= maxActive) {

                    zooKeeper = new ZooKeeper("8.129.88.114:2181,8.129.88.114:2182,8.129.88.114:2183", 5000, (watch) -> {
                        if (watch.getState() == Watcher.Event.KeeperState.SyncConnected) {
                            countDownLatch.countDown();
                        }
                    });
                    countDownLatch.await(60000L, TimeUnit.MILLISECONDS);
                    System.out.println("Thread:" + Thread.currentThread().getId() + "get connection:" + createCount.incrementAndGet());
                    busy.offer(zooKeeper);
                    return zooKeeper;
                } else {

                    activeSize.decrementAndGet();
                }
            }

            try {
                System.out.println("Thread:" + Thread.currentThread().getId() + "wait idle resource");
                Long waitTime = maxWait - (System.currentTimeMillis() - nowTime);
                zooKeeper = idle.poll(waitTime, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new Exception("wait resource error");
            }

            if (zooKeeper != null) {
                System.out.println("Thread:" + Thread.currentThread().getId() + "get connection:" + createCount.incrementAndGet());
                busy.offer(zooKeeper);
                return zooKeeper;
            } else {
                System.out.println("Thread:" + Thread.currentThread().getId() + "get connection timeout, try again!");
                throw new Exception("Thread:" + Thread.currentThread().getId() + "get connection timeout, try again!");
            }
        }
        if (!zooKeeper.getState().isConnected()) {
            CountDownLatch recoveryLatch = new CountDownLatch(1);
            zooKeeper = new ZooKeeper("8.129.88.114:2181,8.129.88.114:2182,8.129.88.114:2183", 5000, (watch) -> {
                if (watch.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    recoveryLatch.countDown();
                }
            });
            recoveryLatch.await(60000L, TimeUnit.MILLISECONDS);
        }
        busy.offer(zooKeeper);
        return zooKeeper;
    }

    @Override
    public void release(ZooKeeper connection) throws Exception {
        if (connection == null) {
            System.out.println("connection is null");
            return;
        }
        if (busy.remove(connection)) {
            idle.offer(connection);
        } else {
            activeSize.decrementAndGet();
            throw new Exception("release resource failed");
        }
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            idle.forEach((zooKeeper) -> {
                try {
                    zooKeeper.close();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            busy.forEach((zooKeeper) -> {
                try {
                    zooKeeper.close();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
