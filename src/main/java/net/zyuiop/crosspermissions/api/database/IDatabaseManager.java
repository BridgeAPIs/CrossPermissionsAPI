package net.zyuiop.crosspermissions.api.database;

import net.zyuiop.crosspermissions.api.permissions.PermissionEntity;
import net.zyuiop.crosspermissions.api.permissions.PermissionGroup;
import net.zyuiop.crosspermissions.api.permissions.PermissionUser;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zyuiop
 */
public interface IDatabaseManager {
	void checkDefaultGroup(String defaultGroup);

	ConcurrentHashMap<UUID, PermissionGroup> getGroupsCache();

	void refreshGroups();

	void refresh();

	void refreshPerms(UUID user);

	PermissionUser getUserFromDB(UUID id);

	PermissionGroup getGroupFromDB(String groupName);

	PermissionGroup getGroupFromDB(UUID groupId);

	PermissionGroup getGroupWithoutParentsFromDB(UUID groupId);

	void moveGroup(String oldName, String newName);

	// Si le groupe n'existe pas c'est que le refresh n'est pas passé.
	PermissionGroup getGroup(String name);

	PermissionGroup getGroup(UUID groupId);

	PermissionUser getUser(UUID name);

	PermissionUser getUserFromCache(UUID name);

	void createGroup(PermissionGroup group);

	void updateGroupLadder(PermissionGroup group);

	void setProperty(PermissionEntity entity, String name, String value);

	void deleteProperty(PermissionEntity entity, String name);

	void setPermission(PermissionEntity entity, String name, Boolean value);

	void deletePermission(PermissionEntity entity, String name);

	void addParent(PermissionEntity entity, PermissionGroup parent);

	void removeParent(PermissionEntity entity, PermissionGroup parent);

	void removeGroup(PermissionGroup group);
}
