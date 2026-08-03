package com.ascend.core.api.backend.redis;

import org.slf4j.Logger;
import redis.clients.jedis.*;

public final class RedisConnect implements AutoCloseable {

    private final Logger logger;
    private final String host;
    private final int port;
    private final String password;

    private JedisPool pool;

    public RedisConnect(Logger logger, String host, int port, String password) {
        this.logger = logger;
        this.host = host;
        this.port = port;
        this.password = password;
    }

    public void connect() {
        logger.info("Inicializando pool de conexões em {}:{}...", host, port);

        JedisPoolConfig config = new JedisPoolConfig();

        config.setMaxTotal(16);
        config.setMaxIdle(8);
        config.setMinIdle(2);

        config.setTestOnBorrow(false);
        config.setTestWhileIdle(true);
        config.setTestOnReturn(false);

        if (password == null || password.isBlank()) {
            pool = new JedisPool(config, host, port, 5000);
        } else {
            pool = new JedisPool(config, host, port, 5000, password);
        }

        try (Jedis jedis = pool.getResource()) {
            jedis.ping();
        }

        logger.info("Pool ativo e pronto → {}:{}", host, port);
    }

    public Jedis getResource() {
        if (pool == null || pool.isClosed()) {
            throw new IllegalStateException("Redis não está conectado.");
        }

        return pool.getResource();
    }

    public boolean isConnected() {
        return pool != null && !pool.isClosed();
    }

    @Override
    public void close() {
        if (pool == null || pool.isClosed()) {
            return;
        }

        logger.info("Fechando pool de conexões...");

        pool.close();
        pool = null;

        logger.info("Pool encerrado com sucesso.");
    }
}
