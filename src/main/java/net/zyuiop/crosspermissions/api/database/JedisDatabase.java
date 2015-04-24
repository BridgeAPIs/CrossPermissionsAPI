package net.zyuiop.crosspermissions.api.database;

import redis.clients.jedis.Jedis;

/**
 * This file is licensed under MIT License
 * A copy of the license is provided with the source
 * (C) zyuiop 2015
 */
public class JedisDatabase implements Database {
    private final String host;
    private final int port;
    private final String password;

    public JedisDatabase(String host, int port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
    }

    @Override
    public Jedis getJedis() {
        Jedis j =  new Jedis(this.host, this.port, 5000);
        if (password != null)
            j.auth(password);
        return j;
    }
}
