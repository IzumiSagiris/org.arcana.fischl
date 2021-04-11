package com.arcana.fischl.pool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ZookeeperConnComponent {
    private ZookeeperConnectionPool zookeeperConnectionPool;

    public ZookeeperConnectionPool getZookeeperConnectionPool() {
        return zookeeperConnectionPool;
    }

    @Autowired
    public ZookeeperConnComponent() {
        zookeeperConnectionPool = new ZookeeperConnectionPool(10, 10000L);
    }
}
