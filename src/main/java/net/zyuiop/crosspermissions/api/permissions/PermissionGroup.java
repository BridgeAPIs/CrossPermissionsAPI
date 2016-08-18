package net.zyuiop.crosspermissions.api.permissions;

import net.zyuiop.crosspermissions.api.database.IDatabaseManager;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class PermissionGroup extends PermissionEntity {
	protected Integer ladder = 9999; // Par défaut pour éviter les fails
	protected String groupName;

	public PermissionGroup(IDatabaseManager manager, UUID groupIdentifier, Integer ladder, String groupName) {
		super(manager, groupIdentifier);
		this.groupName = groupName;
		this.ladder = ladder;
		this.type = "group";
	}

	public PermissionGroup(IDatabaseManager manager, Collection<PermissionGroup> parents,
						   Map<String, Boolean> perms, Map<String, String> options,
						   String gname, Integer ladder, UUID groupIdentifier) {
		super(manager, groupIdentifier, parents, perms, options);
		this.groupName = gname;
		this.ladder = ladder;
		this.type = "group";
	}

	public PermissionGroup(IDatabaseManager manager,
						   Map<String, Boolean> perms, Map<String, String> options,
						   String gname, Integer ladder, UUID groupIdentifier) {
		super(manager, groupIdentifier, perms, options);
		this.groupName = gname;
		this.ladder = ladder;
		this.type = "group";
	}

	public String getDbPrefix() {
		return "groups:";
	}

	public void create() {
		this.manager.createGroup(this);
	}

	public void remove() {
		this.manager.removeGroup(this);
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
		manager.updateGroupLadder(this);
	}
}
