package net.zyuiop.crosspermissions.api.uuids;

import net.zyuiop.crosspermissions.api.PermissionsAPI;

import java.util.Calendar;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * @author zyuiop
 */
public abstract class UUIDTranslator {
	protected final PermissionsAPI api;
	protected final Pattern UUID_PATTERN = Pattern.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
	protected final Pattern MOJANGIAN_UUID_PATTERN = Pattern.compile("[a-fA-F0-9]{32}");
	protected final Map<String, CachedUUIDEntry> nameToUuidMap = new ConcurrentHashMap<>(128, 0.5f, 4);
	protected final Map<UUID, CachedUUIDEntry> uuidToNameMap = new ConcurrentHashMap<>(128, 0.5f, 4);

	protected UUIDTranslator(PermissionsAPI api) {
		this.api = api;
	}

	public UUID getUUID(String name, boolean allowMojangCheck) {
		// If the player is online, give them their UUID.
		// Remember, local data > remote data.
		if (PermissionsAPI.permissionsAPI.getPlugin().getPlayerId(name) != null)
			return PermissionsAPI.permissionsAPI.getPlugin().getPlayerId(name);

		// Check if it exists in the map
		CachedUUIDEntry cachedUUIDEntry = nameToUuidMap.get(name.toLowerCase());
		if (cachedUUIDEntry != null) {
			if (!cachedUUIDEntry.expired())
				return cachedUUIDEntry.getUuid();
			else
				nameToUuidMap.remove(name);
		}

		// Check if we can exit early
		if (UUID_PATTERN.matcher(name).find()) {
			return UUID.fromString(name);
		}

		if (MOJANGIAN_UUID_PATTERN.matcher(name).find()) {
			// Reconstruct the UUID
			return UUIDFetcher.getUUID(name);
		}

		return null;
	}

	public String getName(UUID uuid, boolean allowMojangCheck) {
		if (PermissionsAPI.permissionsAPI.getPlugin().getPlayerName(uuid) != null)
			return PermissionsAPI.permissionsAPI.getPlugin().getPlayerName(uuid);

		// Check if it exists in the map
		CachedUUIDEntry cachedUUIDEntry = uuidToNameMap.get(uuid);
		if (cachedUUIDEntry != null) {
			if (!cachedUUIDEntry.expired())
				return cachedUUIDEntry.getName();
			else
				uuidToNameMap.remove(uuid);
		}
		return null;
	}

	protected void addToMaps(String name, UUID uuid) {
		// This is why I like LocalDate...

		// Cache the entry for three days.
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_WEEK, 3);

		// Create the entry and populate the local maps
		CachedUUIDEntry entry = new CachedUUIDEntry(name, uuid, calendar);
		nameToUuidMap.put(name.toLowerCase(), entry);
		uuidToNameMap.put(uuid, entry);
	}

	protected static class CachedUUIDEntry {
		private final String name;
		private final UUID uuid;
		private final Calendar expiry;

		public boolean expired() {
			return Calendar.getInstance().after(expiry);
		}

		public CachedUUIDEntry(String name, UUID uuid, Calendar expiry) {
			this.name = name;
			this.uuid = uuid;
			this.expiry = expiry;
		}

		public String getName() {
			return name;
		}

		public UUID getUuid() {
			return uuid;
		}

		public Calendar getExpiry() {
			return expiry;
		}
	}
}
