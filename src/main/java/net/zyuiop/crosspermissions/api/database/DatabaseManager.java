package net.zyuiop.crosspermissions.api.database;

import net.zyuiop.crosspermissions.api.PermissionsAPI;
import net.zyuiop.crosspermissions.api.permissions.PermissionEntity;
import net.zyuiop.crosspermissions.api.permissions.PermissionGroup;
import net.zyuiop.crosspermissions.api.permissions.PermissionUser;
import net.zyuiop.crosspermissions.api.rawtypes.RawPlayer;
import net.zyuiop.crosspermissions.api.rawtypes.RawPlugin;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DatabaseManager implements IDatabaseManager {

	protected ConcurrentHashMap<UUID, PermissionGroup> groupsCache = new ConcurrentHashMap<>();
	protected ConcurrentHashMap<UUID, PermissionUser> usersCache = new ConcurrentHashMap<>();
	protected ConcurrentHashMap<String, UUID> groupsTables = new ConcurrentHashMap<>();

	protected RawPlugin pl;
	protected PermissionsAPI api;
    protected final RedisDatabase database;

	public Jedis getJedis() {
		return database.getJedis();
	}
	
	public DatabaseManager(PermissionsAPI api, RedisDatabase database) {
        this.database = database;
        this.pl = api.getPlugin();
		this.api = api;

        pl.logInfo("[DBMANAGER] Initialising task...");

		pl.runRepeatedTaskAsync(this::refresh, 5*60*20L, 5*20L);

        pl.logInfo("[DBMANAGER] Database manager loaded !");
	}

	@Override
	public void checkDefaultGroup(String defaultGroup) {
		refreshGroups();
		PermissionGroup group = getGroupFromDB(defaultGroup);
		if (group == null) {
			pl.logInfo("Default Group doesn't exist, creating it.");
			PermissionGroup def = new PermissionGroup(this, UUID.randomUUID(), 1000, defaultGroup);
			def.create();
			refreshGroups();
			pl.logInfo("Default Group created with ladder 1000 and UUID " + def.getEntityID());
		}
	}

    @Override
	public ConcurrentHashMap<UUID, PermissionGroup> getGroupsCache() {
        return groupsCache;
    }
	
	@Override
	public void refreshGroups() {
		Date begin = new Date();
		pl.logInfo("Refreshing groups (Forced operation)");
		Jedis j = getJedis();
		Map<String, String> groups = j.hgetAll("groups:list");
		j.close();
		
		groupsTables.clear();
		for (String gpeName : groups.keySet())
			groupsTables.put(gpeName, UUID.fromString(groups.get(gpeName)));
		
		int i = 0, s = groups.size();
		for (UUID group : groupsTables.values()) {
			getGroupFromDB(group);
			i++;
			pl.logInfo("[FORCED] Refreshed group "+group+" ("+i+"/"+s+")");
		}
		
		pl.logInfo("Done in "+(new Date().getTime()-begin.getTime())+"ms");
	}
	
	@Override
	public void refresh() {
		pl.logInfo("#==========================#");
		pl.logInfo("# Refreshing Permissions ! #");
		pl.logInfo("#==========================#");
		Date begin = new Date();
		// Refreshing groups //
		
		pl.logInfo("Refreshing groups (1/4)");
		Jedis j = getJedis();
		Map<String, String> groups = j.hgetAll("groups:list");
		j.close();
		
		groupsTables.clear();
		for (String gpeName : groups.keySet())
			groupsTables.put(gpeName, UUID.fromString(groups.get(gpeName)));
		
		int i = 0, s = groups.size();
		for (UUID group : groupsTables.values()) {
			getGroupFromDB(group);
			i++;
		}
		
		Date step = new Date();
		pl.logInfo("Done in "+(step.getTime()-begin.getTime())+"ms");
		
		// Cleaning users //
		pl.logInfo("Cleaning offline users (2/4)");
		for (UUID user : usersCache.keySet()) {
			if (!pl.isOnline(user)) {
				usersCache.remove(user);
			}
		}		
		
		pl.logInfo("Done in "+(new Date().getTime()-step.getTime())+"ms");
		step = new Date();
		
		// Updating users//
		pl.logInfo("Updating online users (3/4)");
		i = 0;
		s = usersCache.size();
		for (UUID user : usersCache.keySet()) {
			getUserFromDB(user);
            refreshPerms(user);
			i++;
		}

		pl.logInfo("Calling hooks (4/4)");
		pl.onRefreshHook();

		pl.logInfo("Done in "+(new Date().getTime()-step.getTime())+"ms");
		step = new Date();
		pl.logInfo("TOTAL EXECUTION TIME : "+(new Date().getTime() - step.getTime())+"ms");
		
		pl.logInfo("#==========================#");
		pl.logInfo("# Refreshed Permissions :) #");
		pl.logInfo("#==========================#");
		
	}

    @Override
	public void refreshPerms(UUID user) {
        RawPlayer p = pl.getPlayer(user);
        PermissionUser u = this.usersCache.get(user);
        if (u != null) {
            p.clearPermissions();
            for (String perm : u.getPermissions().keySet())
                p.setPermission(perm, u.getPermissions().get(perm));
        }
    }
	
	@Override
	public PermissionUser getUserFromDB(UUID id) {
		String db_prefix = "user:"+id.toString()+":";
		Jedis j = getJedis();
		
		// Récupération des données
		List<String> DBparents = j.lrange(db_prefix + "parents", 0, -1);
		Map<String, String> DBoptions = j.hgetAll(db_prefix + "options");
		Map<String, String> DBperms = j.hgetAll(db_prefix + "perms");
		j.close();
		
		// Formatage des données
		ArrayList<PermissionGroup> parents = new ArrayList<>();
		if (DBparents != null && DBparents.size() > 0)
			for (String gid : DBparents)
				parents.add(getGroup(UUID.fromString(gid)));
		else
			parents.add(api.getDefGroup()); // On ne sauvegardera pas ce joueur avant qu'il ne change
		
		HashMap<String, String> options = new HashMap<>();
		if (DBoptions != null)
			options.putAll(DBoptions);
		
		HashMap<String, Boolean> perms = new HashMap<>();
		if (DBperms != null)
			for (String permName : DBperms.keySet())
				perms.put(permName, Boolean.parseBoolean(DBperms.get(permName)));
		

		// Génération de l'objet
		PermissionUser u = new PermissionUser(this, parents, perms, options, id);
		
		// Retour
		usersCache.put(id, u);
		return u;
	}
	
	@Override
	public PermissionGroup getGroupFromDB(String groupName) {
		UUID val = groupsTables.get(groupName);
		if (val == null) 
			return null;
		else
			return getGroupFromDB(val);
	}
	
	@Override
	public PermissionGroup getGroupFromDB(UUID groupId) {
		String db_prefix = "groups:"+groupId+":";
		Jedis j = getJedis();
		
		// Récupération des données
		List<String> DBparents = j.lrange(db_prefix + "parents", 0, - 1);
		Map<String, String> DBoptions = j.hgetAll(db_prefix + "options");
		Map<String, String> DBperms = j.hgetAll(db_prefix + "perms");
		String DBladder = j.get(db_prefix + "ladder");
		String DBgname = j.get(db_prefix + "name");
		j.close();
		
		if (DBladder == null) 
			return null;
		
		Integer ladder = null;
		try {
			ladder = Integer.parseInt(DBladder);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return null;
		}
		
		// Formatage des données
		ArrayList<PermissionGroup> parents = new ArrayList<>();
		if (DBparents != null && DBparents.size() > 0)
			parents.addAll(DBparents.stream().map(gpe -> getGroup(UUID.fromString(gpe))).collect(Collectors.toList()));
		
		HashMap<String, String> options = new HashMap<>();
		if (DBoptions != null)
			options.putAll(DBoptions);
		
		HashMap<String, Boolean> perms = new HashMap<>();
		if (DBperms != null)
			for (String permName : DBperms.keySet())
				perms.put(permName, Boolean.parseBoolean(DBperms.get(permName)));
		

		// Génération de l'objet
		PermissionGroup g = new PermissionGroup(this, parents, perms, options, DBgname, ladder, groupId);
		
		// Retour
		groupsCache.put(groupId, g);
		return g;
	}

    @Override
	public PermissionGroup getGroupWithoutParentsFromDB(UUID groupId) {
        String db_prefix = "groups:"+groupId+":";
        Jedis j = getJedis();

        // Récupération des données
        Map<String, String> DBoptions = j.hgetAll(db_prefix + "options");
        Map<String, String> DBperms = j.hgetAll(db_prefix + "perms");
        String DBladder = j.get(db_prefix + "ladder");
        String DBgname = j.get(db_prefix + "name");
        j.close();

        if (DBladder == null)
            return null;

        Integer ladder = null;
        try {
            ladder = Integer.parseInt(DBladder);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }



        HashMap<String, String> options = new HashMap<>();
        if (DBoptions != null)
            options.putAll(DBoptions);

        HashMap<String, Boolean> perms = new HashMap<>();
        if (DBperms != null)
            for (String permName : DBperms.keySet())
                perms.put(permName, Boolean.parseBoolean(DBperms.get(permName)));


        // Génération de l'objet
        PermissionGroup g = new PermissionGroup(this, perms, options, DBgname, ladder, groupId);

        groupsCache.put(groupId, g);
        return g;
    }
	
	@Override
	public void moveGroup(String oldName, String newName) {
		Jedis j = getJedis();
		String val = j.hget("groups:list", oldName);
		this.groupsTables.remove(oldName);
		this.groupsTables.put(newName, UUID.fromString(val));
		if (val != null) {
			j.hdel("groups:list", oldName);
			j.hset("groups:list", newName, val);
		}
		j.close();
	}
	
	// Si le groupe n'existe pas c'est que le refresh n'est pas passé.
	@Override
	public PermissionGroup getGroup(String name) {
		UUID groupId = this.groupsTables.get(name);
		if (groupId == null)
			return null;
		return getGroup(groupId);
	}
	
	@Override
	public PermissionGroup getGroup(UUID groupId) {
		PermissionGroup r = groupsCache.get(groupId);
		if (r == null)
			r = getGroupFromDB(groupId);
		return r;
	}
	
	@Override
	public PermissionUser getUser(UUID name) {
		if (!usersCache.containsKey(name))
			return getUserFromDB(name);
		return usersCache.get(name);
	}

	@Override
	public PermissionUser getUserFromCache(UUID name) {
		if (!usersCache.containsKey(name))
			return null;
		return usersCache.get(name);
	}

	@Override
	public void createGroup(PermissionGroup group) {
		this.set(group, "ladder", "" + group.getLadder());
		this.set(group, "name", group.getGroupName());
		Jedis j = this.getJedis();
		j.hset("groups:list", group.getGroupName(), group.getEntityID().toString());
		j.close();
	}

	@Override
	public void updateGroupLadder(PermissionGroup group) {
		this.set(group, "ladder", "" + group.getLadder());
	}

	@Override
	public void setProperty(PermissionEntity entity, String name, String value) {
		this.hset(entity, DataType.OPTION, name, value);
	}

	@Override
	public void deleteProperty(PermissionEntity entity, String name) {
		this.hdel(entity, DataType.OPTION, name);
	}

	@Override
	public void setPermission(PermissionEntity entity, String name, Boolean value) {
		this.hset(entity, DataType.PERMISSION, name, value.toString());
	}

	@Override
	public void deletePermission(PermissionEntity entity, String name) {
		this.hdel(entity, DataType.PERMISSION, name);
	}

	@Override
	public void addParent(PermissionEntity entity, PermissionGroup parent) {
		this.lput(entity, DataType.PARENT, parent.getEntityID().toString());
	}

	@Override
	public void removeParent(PermissionEntity entity, PermissionGroup parent) {
		this.lrem(entity, DataType.PARENT, parent.getEntityID().toString());
	}

	@Override
	public void removeGroup(PermissionGroup group) {
		// Remove data
		del(group, "ladder");
		del(group, "name");
		del(group, "parents");
		del(group, "perms");
		del(group, "options");

		// Remove in list
		Jedis j = this.getJedis();
		j.hdel("groups:list", group.getGroupName());
		j.close();
	}



	/** Opérations dans la DB **/
	private void set(PermissionEntity e, String key, final String data) {
		final String datakey = e.getDbPrefix()+e.getEntityID()+":"+key;
		pl.runAsync(() -> {
			Jedis j = getJedis();
			try {
				j.set(datakey, data);
			} finally {
				j.close();
			}
		});
	}

	private void del(PermissionEntity e, String key) {
        final String datakey = e.getDbPrefix()+e.getEntityID()+":"+key;
        pl.runAsync(() -> {
			Jedis j = getJedis();
			try {
				j.del(datakey);
			} finally {
				j.close();
			}
		});
    }

	private void lput(PermissionEntity e, DataType type, final String data) {
		final String datakey = e.getDbPrefix()+e.getEntityID()+":"+type.getKey();
		pl.runAsync(new Runnable() {
			public void run() {
				Jedis j = getJedis();
				try {
					j.lpush(datakey, data);
				} finally {
					j.close();
				}
			}
		});
	}

	private void lrem(PermissionEntity e, DataType type, final String data) {
		final String datakey = e.getDbPrefix()+e.getEntityID()+":"+type.getKey();
		pl.runAsync(new Runnable() {
			public void run() {
				Jedis j = getJedis();
				try {
					j.lrem(datakey, 0, data);
				} finally {
					j.close();
				}
			}
		});
	}

	private void lrename(PermissionEntity e, DataType type, String oldData, String newData) {
		this.lrem(e, type, oldData);
		this.lput(e, type, newData);
	}
	
	/*** Opérations sur les hashmaps ***/
	private void hset(PermissionEntity e, DataType type, final String hashkey, final String data) {
		final String datakey = e.getDbPrefix()+e.getEntityID()+":"+type.getKey();
		pl.runAsync(new Runnable() {
			public void run() {
				Jedis j = getJedis();
				try {
					j.hset(datakey, hashkey, data);
				} finally {
					j.close();
				}
			}
		});
	}
	
	private void hdel(PermissionEntity e, DataType type, final String hashkey) {
		final String datakey = e.getDbPrefix()+e.getEntityID()+":"+type.getKey();
		pl.runAsync(() -> {
			Jedis j = getJedis();
			try {
				j.hdel(datakey, hashkey);
			} finally {
				j.close();
			}
		});
	}
	
	private String hget(PermissionEntity e, DataType type, String hashkey) {
		String datakey = e.getDbPrefix()+e.getEntityID()+":"+type.getKey();
		Jedis j = getJedis();
		String ret = null;
		try {
			ret = j.hget(datakey, hashkey);
		} finally {
			j.close();
		}
		return ret;
	}
	
	public void hrename(PermissionEntity e, DataType type, String oldKey, String newKey) {
		String val = hget(e, type, oldKey);
		hdel(e, type, oldKey);
		hset(e, type, newKey, val);
	}
	
	
}
