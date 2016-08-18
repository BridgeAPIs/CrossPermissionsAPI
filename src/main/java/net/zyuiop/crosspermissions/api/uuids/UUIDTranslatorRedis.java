/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 * <p>
 * Updated by zyuiop to be used with Bukkit API
 */
package net.zyuiop.crosspermissions.api.uuids;

import com.google.gson.Gson;
import net.zyuiop.crosspermissions.api.PermissionsAPI;
import net.zyuiop.crosspermissions.api.database.DatabaseManager;
import net.zyuiop.crosspermissions.api.database.RedisDatabase;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public final class UUIDTranslatorRedis extends UUIDTranslator {
	private final RedisDatabase manager;

	public UUIDTranslatorRedis(PermissionsAPI api, RedisDatabase manager) {
		super(api);
		this.manager = manager;
	}

	public void persistInfo(String name, UUID uuid, Jedis jedis) {
		addToMaps(name, uuid);
		jedis.hset("uuid-cache", name.toLowerCase(), new Gson().toJson(uuidToNameMap.get(uuid)));
		jedis.hset("uuid-cache", uuid.toString(), new Gson().toJson(uuidToNameMap.get(uuid)));
	}


	public UUID getUUID(String name, boolean allowMojangCheck) {
		UUID sup = super.getUUID(name, allowMojangCheck);
		if (sup != null)
			return sup;

		// Let's try Redis.
		try (Jedis jedis = manager.getJedis()) {
			String stored = jedis.hget("uuid-cache", name.toLowerCase());
			if (stored != null) {
				// Found an entry value. Deserialize it.
				CachedUUIDEntry entry = new Gson().fromJson(stored, CachedUUIDEntry.class);

				// Check for expiry:
				if (entry.expired()) {
					jedis.hdel("uuid-cache", name.toLowerCase());
				} else {
					nameToUuidMap.put(name.toLowerCase(), entry);
					uuidToNameMap.put(entry.getUuid(), entry);
					return entry.getUuid();
				}
			}

			// That didn't work. Let's ask Mojang.
			if (!allowMojangCheck)
				return null;

			Map<String, UUID> uuidMap1;
			try {
				uuidMap1 = new UUIDFetcher(Collections.singletonList(name)).call();
			} catch (Exception e) {
				api.getPlugin().logSevere("Unable to fetch UUID from Mojang for " + name + " : " + e.getMessage());
				return null;
			}
			for (Map.Entry<String, UUID> entry : uuidMap1.entrySet()) {
				if (entry.getKey().equalsIgnoreCase(name)) {
					persistInfo(entry.getKey(), entry.getValue(), jedis);
					return entry.getValue();
				}
			}
		} catch (JedisException e) {
			api.getPlugin().logSevere("Unable to fetch UUID from Mojang for " + name + " : " + e.getMessage());
		}

		return null; // Nope, game over!
	}

	public String getName(UUID uuid, boolean allowMojangCheck) {
		String sup = super.getName(uuid, allowMojangCheck);
		if (sup != null)
			return sup;

		// Okay, it wasn't locally cached. Let's try Redis.
		try (Jedis jedis = manager.getJedis()) {
			String stored = jedis.hget("uuid-cache", uuid.toString());
			if (stored != null) {
				// Found an entry value. Deserialize it.
				CachedUUIDEntry entry = new Gson().fromJson(stored, CachedUUIDEntry.class);

				// Check for expiry:
				if (entry.expired()) {
					jedis.hdel("uuid-cache", uuid.toString());
				} else {
					nameToUuidMap.put(entry.getName().toLowerCase(), entry);
					uuidToNameMap.put(uuid, entry);
					return entry.getName();
				}
			}

			if (!allowMojangCheck)
				return null;

			// That didn't work. Let's ask Mojang. This call may fail, because Mojang is insane.
			String name;
			try {
				name = NameFetcher.nameHistoryFromUuid(uuid).get(0);
			} catch (Exception e) {
				api.getPlugin().logSevere("Unable to fetch name from Mojang for " + uuid + " : " + e.getMessage());
				return null;
			}

			if (name != null) {
				persistInfo(name, uuid, jedis);
				return name;
			}

			return null;
		} catch (JedisException e) {
			api.getPlugin().logSevere("Unable to fetch name from Mojang for " + uuid + " : " + e.getMessage());
			return null;
		}
	}
}
