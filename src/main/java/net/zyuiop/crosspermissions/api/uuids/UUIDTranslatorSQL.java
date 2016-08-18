/**
 * Copyright © 2013 tuxed <write@imaginarycode.com>
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.
 * <p>
 * Updated by zyuiop to be used with Bukkit API
 */
package net.zyuiop.crosspermissions.api.uuids;

import net.zyuiop.crosspermissions.api.PermissionsAPI;
import net.zyuiop.crosspermissions.api.database.SQLDatabase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public final class UUIDTranslatorSQL extends UUIDTranslator {
	private final SQLDatabase manager;

	public UUIDTranslatorSQL(PermissionsAPI api, SQLDatabase manager) {
		super(api);
		this.manager = manager;
	}

	public void persistInfo(String name, UUID uuid) {
		addToMaps(name, uuid);

		manager.execute(connection -> {
			PreparedStatement statement = connection.prepareStatement("INSERT INTO crosspermissions_uuidcache(player_uuid, player_name) VALUES (?, ?) ON DUPLICATE KEY UPDATE player_name = ?, player_uuid = ?");
			statement.setString(1, name);
			statement.setString(2, uuid.toString());
		});
	}

	public UUID getUUID(String name, boolean allowMojangCheck) {
		UUID sup = super.getUUID(name, allowMojangCheck);
		if (sup != null)
			return sup;

		// Let's try SQL.
		UUID id = manager.query(connection -> {
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM crosspermissions_uuidcache WHERE player_name = ?");
			statement.setString(1, name);

			ResultSet set = statement.executeQuery();
			if (set.first()) {
				Date date = set.getDate("last_update");

				Calendar calendar = new GregorianCalendar();
				calendar.setTime(date);
				calendar.add(Calendar.DAY_OF_WEEK, 3);

				CachedUUIDEntry entry = new CachedUUIDEntry(set.getString("player_name"), UUID.fromString(set.getString("player_uuid")), calendar);

				// Check for expiry:
				if (entry.expired()) {
					statement = connection.prepareStatement("DELETE FROM crosspermissions_uuidcache WHERE player_name = ?");
					statement.setString(1, name);
					statement.executeUpdate();

					return null;
				} else {
					nameToUuidMap.put(name.toLowerCase(), entry);
					uuidToNameMap.put(entry.getUuid(), entry);
					return entry.getUuid();
				}
			}
			return null;
		});

		if (id != null)
			return id;

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
				persistInfo(entry.getKey(), entry.getValue());
				return entry.getValue();
			}
		}

		return null; // Nope, game over!
	}

	public String getName(UUID uuid, boolean allowMojangCheck) {
		String sup = super.getName(uuid, allowMojangCheck);
		if (sup != null)
			return sup;

		// Let's try SQL.
		String val = manager.query(connection -> {
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM crosspermissions_uuidcache WHERE player_uuid = ?");
			statement.setString(1, uuid.toString());

			ResultSet set = statement.executeQuery();
			if (set.first()) {
				Date date = set.getDate("last_update");

				Calendar calendar = new GregorianCalendar();
				calendar.setTime(date);
				calendar.add(Calendar.DAY_OF_WEEK, 3);

				CachedUUIDEntry entry = new CachedUUIDEntry(set.getString("player_name"), UUID.fromString(set.getString("player_uuid")), calendar);

				// Check for expiry:
				if (entry.expired()) {
					statement = connection.prepareStatement("DELETE FROM crosspermissions_uuidcache WHERE player_uuid = ?");
					statement.setString(1, uuid.toString());
					statement.executeUpdate();

					return null;
				} else {
					nameToUuidMap.put(entry.getName(), entry);
					uuidToNameMap.put(entry.getUuid(), entry);
					return entry.getName();
				}
			}
			return null;
		});

		if (val != null)
			return val;

		// That didn't work. Let's ask Mojang.
		if (!allowMojangCheck)
			return null;

		// Okay, it wasn't locally cached. Let's try Redis.
		String name;
		try {
			name = NameFetcher.nameHistoryFromUuid(uuid).get(0);
		} catch (Exception e) {
			api.getPlugin().logSevere("Unable to fetch name from Mojang for " + uuid + " : " + e.getMessage());
			return null;
		}

		if (name != null) {
			persistInfo(name, uuid);
			return name;
		}

		return null;
	}
}
