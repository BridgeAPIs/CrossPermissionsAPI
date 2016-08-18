package net.zyuiop.crosspermissions.api.database;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.zyuiop.crosspermissions.api.PermissionsAPI;
import net.zyuiop.crosspermissions.api.permissions.PermissionEntity;
import net.zyuiop.crosspermissions.api.permissions.PermissionGroup;
import net.zyuiop.crosspermissions.api.permissions.PermissionUser;
import net.zyuiop.crosspermissions.api.rawtypes.RawPlayer;
import net.zyuiop.crosspermissions.api.rawtypes.RawPlugin;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SQLDatabaseManager implements IDatabaseManager {

	protected ConcurrentHashMap<UUID, PermissionGroup> groupsCache = new ConcurrentHashMap<>();
	protected ConcurrentHashMap<UUID, PermissionUser> usersCache = new ConcurrentHashMap<>();
	protected ConcurrentHashMap<String, UUID> groupsTables = new ConcurrentHashMap<>();

	protected RawPlugin pl;
	protected PermissionsAPI api;
	protected final SQLDatabase database;

	public Connection getConnection() throws SQLException {
		return database.getConnection();
	}

	public SQLDatabaseManager(PermissionsAPI api, SQLDatabase database) {
		this.database = database;
		this.pl = api.getPlugin();
		this.api = api;

		pl.logInfo("[DBMANAGER] Initialising task...");

		pl.runRepeatedTaskAsync(this::refresh, 5 * 60 * 20L, 5 * 20L);

		pl.logInfo("[DBMANAGER] Database manager loaded !");
	}

	@Override
	public void checkDefaultGroup(String defaultGroup) {
		refreshGroups();
		PermissionGroup group = getGroupFromDB(defaultGroup);
		if (group == null) {
			pl.logInfo("Default Group doesn't exist, creating it.");
			PermissionGroup def = new PermissionGroup(this, UUID.randomUUID(), 1000, defaultGroup);
			createGroup(def);
			refreshGroups();
			pl.logInfo("Default Group created with ladder 1000 and UUID " + def.getEntityID());
		}
	}

	@Override
	public ConcurrentHashMap<UUID, PermissionGroup> getGroupsCache() {
		return groupsCache;
	}

	private Map<String, UUID> getGroups() {
		return database.query(connection -> {
			Statement statement = connection.createStatement();
			ResultSet set = statement.executeQuery("SELECT entity_name, entity_uuid FROM crosspermissions_entities " +
					"WHERE entity_type = 'group'");

			Map<String, UUID> gr = new HashMap<>();
			while (set.next()) {
				gr.put(set.getString("entity_name"), UUID.fromString(set.getString("entity_uuid")));
			}

			return gr;
		});
	}

	@Override
	public void refreshGroups() {
		Date begin = new Date();
		pl.logInfo("Refreshing groups...");

		groupsTables.clear();
		groupsTables.putAll(getGroups());

		int i = 0, s = groupsTables.size();
		for (UUID group : groupsTables.values()) {
			getGroupFromDB(group);
			i++;
			pl.logInfo("... Refreshed group " + group + " (" + i + "/" + s + ")");
		}

		pl.logInfo("Done in " + (new Date().getTime() - begin.getTime()) + "ms");
	}


	@Override
	public void refresh() {
		pl.logInfo("#==========================#");
		pl.logInfo("# Refreshing Permissions ! #");
		pl.logInfo("#==========================#");
		// Refreshing groups //

		refreshGroups();

		// Cleaning users //
		Date step = new Date();
		pl.logInfo("Cleaning offline users (2/4)");
		usersCache.keySet().stream().filter(user -> !pl.isOnline(user)).forEach(user -> usersCache.remove(user));

		pl.logInfo("Done in " + (new Date().getTime() - step.getTime()) + "ms");
		step = new Date();

		// Updating users//
		pl.logInfo("Updating online users (3/4)");

		int i = 0;
		int s = usersCache.size();
		for (UUID user : usersCache.keySet()) {
			getUserFromDB(user);
			refreshPerms(user);
			i++;
			pl.logInfo("... Updated user " + user + " (" + i + "/" + s + ")");
		}

		pl.logInfo("Calling hooks (4/4)");
		pl.onRefreshHook();

		pl.logInfo("Done in " + (new Date().getTime() - step.getTime()) + "ms");
		step = new Date();
		pl.logInfo("TOTAL EXECUTION TIME : " + (new Date().getTime() - step.getTime()) + "ms");

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
		PermissionUser u = database.query(connection -> {
			PreparedStatement statement = connection.prepareStatement("SELECT crosspermissions_entities.entity_id, option_name, option_value, permission_name, permission_value, parent.entity_uuid AS parent_uuid " +
					"FROM crosspermissions_entities " +
					"NATURAL LEFT JOIN crosspermissions_options " +
					"NATURAL LEFT JOIN crosspermissions_parents " +
					"NATURAL LEFT JOIN crosspermissions_permissions " +
					"JOIN crosspermissions_entities AS parent ON parent.entity_id = crosspermissions_parents.parent_id " +
					"WHERE entity_type = 'user' AND entity_uuid = ?");

			statement.setString(1, id.toString());
			ResultSet set = statement.executeQuery();

			int entityId = -1;
			Set<String> dbparents = Sets.newHashSet();
			Map<String, String> options = Maps.newHashMap();
			Map<String, Boolean> perms = Maps.newHashMap();

			while (set.next()) {
				entityId = set.getInt("entity_id");
				dbparents.add(set.getString("parent_uuid"));
				options.put(set.getString("option_name"), set.getString("option_value"));
				perms.put(set.getString("permission_name"), set.getBoolean("permission_value"));
			}

			// Formatage des données
			ArrayList<PermissionGroup> parents = new ArrayList<>();
			if (dbparents.size() > 0)
				parents.addAll(dbparents.stream().map(gid -> getGroup(UUID.fromString(gid))).collect(Collectors.toList()));
			else
				parents.add(api.getDefGroup()); // On ne sauvegardera pas ce joueur avant qu'il ne change

			// Génération de l'objet
			PermissionUser user = new PermissionUser(this, parents, perms, options, id);
			if (entityId > -1)
				user.setDatabaseId(entityId);
			return user;
		});

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
		PermissionGroup g = database.query(connection -> {
			PreparedStatement statement = connection.prepareStatement("SELECT crosspermissions_entities.entity_id, crosspermissions_entities.entity_name, crosspermissions_entities.entity_ladder, option_name, option_value, permission_name, permission_value, parent.entity_uuid AS parent_uuid " +
					"FROM crosspermissions_entities " +
					"NATURAL LEFT JOIN crosspermissions_options " +
					"NATURAL LEFT JOIN crosspermissions_parents " +
					"NATURAL LEFT JOIN crosspermissions_permissions " +
					"JOIN crosspermissions_entities AS parent ON parent.entity_id = crosspermissions_parents.parent_id " +
					"WHERE entity_type = 'group' AND entity_uuid = ?");

			statement.setString(1, groupId.toString());
			ResultSet set = statement.executeQuery();

			if (!set.first())
				return null;

			int id = set.getInt("entity_id");
			int ladder = set.getInt("entity_ladder");
			String name = set.getString("entity_name");

			Set<String> dbparents = Sets.newHashSet();
			Map<String, String> options = Maps.newHashMap();
			Map<String, Boolean> perms = Maps.newHashMap();

			do {
				dbparents.add(set.getString("parent_uuid"));
				options.put(set.getString("option_name"), set.getString("option_value"));
				perms.put(set.getString("permission_name"), set.getBoolean("permission_value"));
			} while (set.next());

			// Formatage des données
			ArrayList<PermissionGroup> parents = new ArrayList<>();
			if (dbparents.size() > 0)
				parents.addAll(dbparents.stream().map(gid -> getGroup(UUID.fromString(gid))).collect(Collectors.toList()));

			// Génération de l'objet
			PermissionGroup group = new PermissionGroup(this, parents, perms, options, name, ladder, groupId);
			group.setDatabaseId(id);
			return group;
		});

		// Retour
		groupsCache.put(groupId, g);
		return g;
	}

	@Override
	public PermissionGroup getGroupWithoutParentsFromDB(UUID groupId) {
		PermissionGroup g = database.query(connection -> {
			PreparedStatement statement = connection.prepareStatement("SELECT crosspermissions_entities.entity_id, entity_name, entity_ladder, option_name, option_value, permission_name, permission_value " +
					"FROM crosspermissions_entities " +
					"NATURAL LEFT JOIN crosspermissions_options " +
					"NATURAL LEFT JOIN crosspermissions_permissions " +
					"JOIN crosspermissions_entities AS parent ON parent.entity_id = crosspermissions_parents.parent_id " +
					"WHERE entity_type = 'group' AND entity_uuid = ?");

			statement.setString(1, groupId.toString());
			ResultSet set = statement.executeQuery();

			if (!set.first())
				return null;

			int id = set.getInt("entity_id");
			int ladder = set.getInt("entity_ladder");
			String name = set.getString("entity_name");

			Map<String, String> options = Maps.newHashMap();
			Map<String, Boolean> perms = Maps.newHashMap();

			do {
				options.put(set.getString("option_name"), set.getString("option_value"));
				perms.put(set.getString("permission_name"), set.getBoolean("permission_value"));
			} while (set.next());

			// Génération de l'objet
			PermissionGroup group = new PermissionGroup(this, perms, options, name, ladder, groupId);
			group.setDatabaseId(id);
			return group;
		});

		groupsCache.put(groupId, g);
		return g;
	}

	@Override
	public void moveGroup(String oldName, String newName) {
		database.execute(connection -> {
			PreparedStatement statement = connection.prepareStatement("UPDATE crosspermissions_entities SET entity_name = ? WHERE entity_name = ? AND entity_type = 'group'");
			statement.setString(1, newName);
			statement.setString(2, oldName);
			statement.executeUpdate();
		});

		UUID val = this.groupsTables.remove(oldName);
		this.groupsTables.put(newName, val);
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
		database.execute(connection -> {
			PreparedStatement statement = connection.prepareStatement("INSERT INTO crosspermissions_entities(entity_uuid, entity_type, entity_ladder, entity_name) VALUES (?, 'group', ?, ?)");
			statement.setString(1, group.getEntityID().toString());
			statement.setInt(2, group.getLadder());
			statement.setString(3, group.getGroupName());
			statement.execute();
		});
	}

	@Override
	public void updateGroupLadder(PermissionGroup group) {
		database.execute(connection -> {
			PreparedStatement statement = connection.prepareStatement("UPDATE crosspermissions_entities SET entity_ladder = ? WHERE entity_uuid = ? AND entity_type = 'group'");
			statement.setInt(1, group.getLadder());
			statement.setString(2, group.getEntityID().toString());
			statement.executeUpdate();
		});
	}

	@Override
	public void setProperty(PermissionEntity entity, String name, String value) {
		int id = getEntityID(entity);

		if (id > -1) {
			database.execute(connection -> {
				PreparedStatement statement = connection.prepareStatement("INSERT INTO crosspermissions_options(entity_id, option_name, option_value) VALUES (?, ?, ?)");
				statement.setInt(1, id);
				statement.setString(2, name);
				statement.setString(3, value);
				statement.executeUpdate();
			});
		}
	}

	@Override
	public void deleteProperty(PermissionEntity entity, String name) {
		int id = getEntityID(entity);

		if (id > -1) {
			database.execute(connection -> {
				PreparedStatement statement = connection.prepareStatement("DELETE FROM crosspermissions_options WHERE entity_id = ? AND option_name = ?");
				statement.setInt(1, id);
				statement.setString(2, name);
				statement.executeUpdate();
			});
		}
	}

	@Override
	public void setPermission(PermissionEntity entity, String name, Boolean value) {
		int id = getEntityID(entity);

		if (id > -1) {
			database.execute(connection -> {
				PreparedStatement statement = connection.prepareStatement("INSERT INTO crosspermissions_permissions(entity_id, permission_name, permission_value) VALUES (?, ?, ?)");
				statement.setInt(1, id);
				statement.setString(2, name);
				statement.setBoolean(3, value);
				statement.executeUpdate();
			});
		}
	}

	@Override
	public void deletePermission(PermissionEntity entity, String name) {
		int id = getEntityID(entity);

		if (id > -1) {
			database.execute(connection -> {
				PreparedStatement statement = connection.prepareStatement("DELETE FROM crosspermissions_permissions WHERE entity_id = ? AND permission_name = ?");
				statement.setInt(1, id);
				statement.setString(2, name);
				statement.executeUpdate();
			});
		}
	}

	@Override
	public void addParent(PermissionEntity entity, PermissionGroup parent) {
		int id = getEntityID(entity);
		int otherId = getEntityID(parent);

		if (id > -1 && otherId > -1) {
			database.execute(connection -> {
				PreparedStatement statement = connection.prepareStatement("INSERT INTO crosspermissions_parents(entity_id, parent_id) VALUES (?, ?)");
				statement.setInt(1, id);
				statement.setInt(2, otherId);
				statement.executeUpdate();
			});
		}
	}

	@Override
	public void removeParent(PermissionEntity entity, PermissionGroup parent) {
		int id = getEntityID(entity);
		int otherId = getEntityID(parent);

		if (id > -1 && otherId > -1) {
			database.execute(connection -> {
				PreparedStatement statement = connection.prepareStatement("DELETE FROM crosspermissions_parents WHERE entity_id = ? AND parent_id = ?");
				statement.setInt(1, id);
				statement.setInt(2, otherId);
				statement.executeUpdate();
			});
		}
	}

	private int getEntityID(PermissionEntity entity) {
		if (entity.getDatabaseId() > -1)
			return entity.getDatabaseId();

		return database.query(connection -> {
			PreparedStatement statement = connection.prepareStatement("SELECT entity_id FROM crosspermissions_entities WHERE entity_type = ? AND entity_uuid = ?");
			statement.setString(1, entity.getType());
			statement.setString(2, entity.getEntityID().toString());

			ResultSet set = statement.executeQuery();
			if (set.first()) {
				int id = set.getInt("entity_id");
				entity.setDatabaseId(id);
				return id;
			}
			return -1;
		});
	}

	@Override
	public void removeGroup(PermissionGroup group) {
		database.execute(connection -> {
			PreparedStatement statement = connection.prepareStatement("DELETE FROM crosspermissions_entities WHERE entity_type = 'group' AND entity_uuid = ?");
			statement.setString(1, group.getEntityID().toString());
			statement.executeUpdate();
		});
	}
}
