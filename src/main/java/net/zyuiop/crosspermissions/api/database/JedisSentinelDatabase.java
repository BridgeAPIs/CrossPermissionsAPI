package net.zyuiop.crosspermissions.api.database;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

import java.util.Set;

/**
 * This file is licensed under MIT License
 * A copy of the license is provided with the source
 * (C) zyuiop 2015
 */
public class JedisSentinelDatabase implements Database {

    private final JedisSentinelPool sentinel;

    public JedisSentinelDatabase(Set<String> sentinels, String masterName, String password) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(1024);
        config.setMaxWaitMillis(5000);

        this.sentinel = new JedisSentinelPool(masterName, sentinels, config, password);
    }

    @Override
    public Jedis getJedis() {
        return sentinel.getResource();
    }
}
