package net.zyuiop.crosspermissions.api;

import net.zyuiop.crosspermissions.api.database.*;
import net.zyuiop.crosspermissions.api.permissions.PermissionGroup;
import net.zyuiop.crosspermissions.api.permissions.PermissionUser;
import net.zyuiop.crosspermissions.api.rawtypes.RawPlayer;
import net.zyuiop.crosspermissions.api.rawtypes.RawPlugin;
import net.zyuiop.crosspermissions.api.uuids.UUIDTranslator;

import java.util.UUID;

public class PermissionsAPI {

	protected RawPlugin plugin;
	protected String defGroup;
	protected IDatabaseManager dbmanager = null;
	public static PermissionsAPI permissionsAPI;
    protected UUIDTranslator translator;

	public PermissionsAPI(RawPlugin plugin, String defGroupn, RedisDatabase database) {
		this(plugin, defGroupn);
		this.dbmanager = new DatabaseManager(this, database);

		plugin.logInfo("Trying to recover default group " + defGroup);
		dbmanager.checkDefaultGroup(defGroup);
	}

	public PermissionsAPI(RawPlugin plugin, String defGroupn, SQLDatabase database) {
		this(plugin, defGroupn);
		this.dbmanager = new SQLDatabaseManager(this, database);

		plugin.logInfo("Trying to recover default group " + defGroup);
		dbmanager.checkDefaultGroup(defGroup);
	}

	private PermissionsAPI(RawPlugin plugin, String defGroup) {
		this.plugin = plugin;
		plugin.logInfo("Loading PermissionsAPI");
        plugin.logInfo("Loading DBManager");
		this.defGroup = defGroup;
        this.translator = new UUIDTranslator(this);
		permissionsAPI = this;
        plugin.logInfo("Loaded PermissionsAPI successfully !");
	}

    public UUIDTranslator getTranslator() {
        return translator;
    }

    public PermissionGroup getDefGroup() {
		return dbmanager.getGroup(defGroup);
	}
	
	public RawPlugin getPlugin() {
		return plugin;
	}

	public void onPlayerJoin(RawPlayer p) {
		PermissionUser u = getUser(p.getUniqueId());
		for (String perm : u.getPermissions().keySet()) 
			p.setPermission(perm, u.getPermissions().get(perm));
		plugin.logInfo("Loaded permissions for player "+p.getUniqueId());
	}
	
	public PermissionUser getUser(UUID u) {
		return dbmanager.getUser(u);
	}
	
	public PermissionGroup getGroup(String name) {
		return dbmanager.getGroup(name);
	}

    public IDatabaseManager getManager() {
        return dbmanager;
    }
}
