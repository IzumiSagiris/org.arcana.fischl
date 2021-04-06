package com.arcana.fischl.pool;

public interface ConnectionPool<T> {

    void init(Integer maxActive, Long maxWait);

    T getResource() throws Exception;

    void release(T connection) throws Exception;

    void close();
}