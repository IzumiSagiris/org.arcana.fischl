package com.arcana.fischl.pool;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

@Component
public class ZkLocker {

    public void lock(String key, Runnable command) {
        ZkLockerWatcher watcher = ZkLockerWatcher.conn(key);
        try {
            if (watcher.getLock()) {
                command.run();
            }
        } finally {
            watcher.releaseLock();
        }
    }

    private static class ZkLockerWatcher implements Watcher {
        public static final String connAddr = "127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183";
        public static final int timeout = 6000;
        public static final String LOCKER_ROOT = "/locker";

        ZooKeeper zooKeeper;
        String parentLockPath;
        String childLockPath;
        Thread thread;

        public static ZkLockerWatcher conn(String key) {
            ZkLockerWatcher watcher = new ZkLockerWatcher();
            try {
                ZooKeeper zooKeeper = watcher.zooKeeper = new ZooKeeper(connAddr, timeout, watcher);
                watcher.thread = Thread.currentThread();
                LockSupport.park();
                if (zooKeeper.exists(LOCKER_ROOT, false) == null) {
                    try {
                        zooKeeper.create(LOCKER_ROOT, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    } catch (KeeperException e) {
                        System.out.println("创建节点失败:" + LOCKER_ROOT);
                    }
                }
                watcher.parentLockPath = LOCKER_ROOT + "/" + key;
                if (zooKeeper.exists(watcher.parentLockPath, false) == null) {
                    try {
                        zooKeeper.create(watcher.parentLockPath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    } catch (KeeperException e) {
                        System.out.println("创建节点失败" + watcher.parentLockPath);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("conn to zk error");
            }
            return watcher;
        }

        public boolean getLock() {
            try {
                this.childLockPath = zooKeeper.create(parentLockPath + "/", "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
                return getLockOrWatchLast();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("get lock error");
            } finally {
                System.out.println("getLock: " + childLockPath);
            }
        }

        public void releaseLock() {
            try {
                if (childLockPath != null) {
                    zooKeeper.delete(childLockPath, -1);
                }
                List<String> children = zooKeeper.getChildren(parentLockPath, false);
                if (children.isEmpty()) {
                    try {
                        zooKeeper.delete(parentLockPath, -1);
                    } catch (KeeperException e) {
                        System.out.println("delete failed, already has other child");
                    }
                }
                // 关闭zk连接
                if (zooKeeper != null) {
                    zooKeeper.close();
                }
            } catch (Exception e) {
                throw new RuntimeException("release lock error");
            } finally {
                System.out.println("releaseLock: " + childLockPath);
            }
        }

        private boolean getLockOrWatchLast() throws KeeperException, InterruptedException {
            List<String> children = zooKeeper.getChildren(parentLockPath, false);
            Collections.sort(children);
            if ((parentLockPath + "/" + children.get(0)).equals(childLockPath)) {
                return true;
            }
            String last = "";
            for (String child : children) {
                if ((parentLockPath + "/" + child).equals(childLockPath)) {
                    break;
                }
                last = child;
            }

            if (zooKeeper.exists(parentLockPath + "/" + last, true) != null) {
                this.thread = Thread.currentThread();
                LockSupport.park();
                return getLockOrWatchLast();
            } else {
                return getLockOrWatchLast();
            }
        }

        @Override
        public void process(WatchedEvent event) {
            if (this.thread != null) {
                LockSupport.unpark(this.thread);
                this.thread = null;
            }
        }
    }
}