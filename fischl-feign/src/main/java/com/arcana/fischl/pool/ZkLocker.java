package com.arcana.fischl.pool;

import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
public class ZkLocker {
    public static final String LOCKER_ROOT = "/locker";

    public Object lock(String key, ZooKeeper poolZK, Function<String, Object> func) {

        String parentLockPath = LOCKER_ROOT + "/" + key;

        String childLockPath = StringUtils.EMPTY;

        try {
            createRootNode(parentLockPath, poolZK);
            childLockPath = poolZK.create(parentLockPath + "/", "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL);
            if (getLockOrWatchLast(parentLockPath, childLockPath, poolZK)) {
                System.out.println("getLock: " + childLockPath);
                return func.apply(childLockPath);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            releaseLock(parentLockPath, childLockPath, poolZK);
        }

        return null;
    }

    private boolean getLockOrWatchLast(String parentLockPath, String childLockPath, ZooKeeper zooKeeper)
            throws KeeperException, InterruptedException {
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

        CountDownLatch recoveryLatch = new CountDownLatch(1);
        if (zooKeeper.exists(parentLockPath + "/" + last, (watchedEvent -> {
            recoveryLatch.countDown();
        })) != null) {
            recoveryLatch.await(60000L, TimeUnit.MILLISECONDS);
            return getLockOrWatchLast(parentLockPath, childLockPath, zooKeeper);
        } else {
            return getLockOrWatchLast(parentLockPath, childLockPath, zooKeeper);
        }
    }

    private void createRootNode(String parentLockPath, ZooKeeper zooKeeper) throws Exception {
        if (zooKeeper.exists(LOCKER_ROOT, false) == null) {
            try {
                zooKeeper.create(LOCKER_ROOT, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } catch (KeeperException e) {
                System.out.println("create znode failed : " + LOCKER_ROOT);
            }
        }
        if (zooKeeper.exists(parentLockPath, false) == null) {
            try {
                zooKeeper.create(parentLockPath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } catch (KeeperException e) {
                System.out.println("parentLockPath already exists : " + parentLockPath);
            }
        }
    }

    public void releaseLock(String parentLockPath, String childLockPath, ZooKeeper zooKeeper) {
        try {
            if (childLockPath != null) {
                zooKeeper.delete(childLockPath, -1);
            }
            List<String> children = zooKeeper.getChildren(parentLockPath, false);
            if (children.isEmpty()) {
                try {
                    zooKeeper.delete(parentLockPath, -1);
                } catch (KeeperException e) {
                    System.out.println("lock node already has other children, please ignore the exception: " + parentLockPath);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("release lock error");
        } finally {
            System.out.println("releaseLock: " + childLockPath);
        }
    }
}