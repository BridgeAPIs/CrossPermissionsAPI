package net.zyuiop.crosspermissions.api.permissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import net.zyuiop.crosspermissions.api.database.DatabaseManager;
import redis.clients.jedis.Jedis;

public class PermissionGroup extends PermissionEntity { 
	protected Integer ladder = 9999; // Par défaut pour éviter les fails
	protected String groupName;
	
	public PermissionGroup(DatabaseManager manager, UUID groupIdentifier, Integer ladder, String groupName) {
		super(manager, groupIdentifier);
		this.groupName = groupName;
		this.ladder = ladder;
		this.db_prefix = "groups:";
	}
	
	public PermissionGroup(DatabaseManager manager, ArrayList<PermissionGroup> parents,
			HashMap<String, Boolean> perms, HashMap<String, String> options,
			String gname, Integer ladder, UUID groupIdentifier) {
		super(manager, groupIdentifier, parents, perms, options);
		this.groupName = gname;
		this.ladder = ladder;
		this.db_prefix = "groups:";
	}

    public PermissionGroup(DatabaseManager manager,
                           HashMap<String, Boolean> perms, HashMap<String, String> options,
                           String gname, Integer ladder, UUID groupIdentifier) {
        super(manager, groupIdentifier, perms, options);
        this.groupName = gname;
        this.ladder = ladder;
        this.db_prefix = "groups:";
    }

    public void create() {
        this.manager.set(this, "ladder", ""+ladder);
        this.manager.set(this, "name", this.getGroupName());
        Jedis j = this.manager.getJedis();
        j.hset("groups:list", this.getGroupName(), this.getEntityID().toString());
        j.close();
    }

    public void remove() {
        this.manager.del(this, "ladder");
        this.manager.del(this, "name");
        this.manager.del(this, "parents");
        this.manager.del(this, "perms");
        this.manager.del(this, "options");
        Jedis j = this.manager.getJedis();
        j.hdel("groups:list", this.getGroupName());
        j.close();
    }

	public String getGroupName() {
		return groupName;
	}
	
	public Integer getLadder() {
		return ladder;
	}
	
	public void setGroupName(String name) {
		String oldName = this.groupName;
		this.groupName = name;
		manager.moveGroup(oldName, name);
	}
	
	public void setLadder(Integer ladder) {
		this.ladder = ladder;
        this.manager.set(this, "ladder", ""+ladder);
	}
	
	
}
