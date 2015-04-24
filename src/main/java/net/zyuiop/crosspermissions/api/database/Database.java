package net.zyuiop.crosspermissions.api.database;

import redis.clients.jedis.Jedis;

/**
 * This file is licensed under MIT License
 * A copy of the license is provided with the source
 * (C) zyuiop 2015
 */
public interface Database {

    public Jedis getJedis();

}
