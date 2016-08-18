package net.zyuiop.crosspermissions.api.permissions;

import net.zyuiop.crosspermissions.api.PermissionsAPI;
import net.zyuiop.crosspermissions.api.database.IDatabaseManager;

import java.util.*;

public class PermissionUser extends PermissionEntity {
	public PermissionUser(IDatabaseManager manager, Collection<PermissionGroup> parents,
						  Map<String, Boolean> perms, Map<String, String> options, UUID uuid) {
		super(manager, uuid, parents, perms, options);
		this.type = "user";
		cleanDisabledParents();
	}

	public PermissionUser(IDatabaseManager manager,
						  Map<String, Boolean> perms, Map<String, String> options, UUID uuid) {
		super(manager, uuid, perms, options);
		this.type = "user";
		cleanDisabledParents();
	}

	public boolean isGroupTemporary(String groupName) {
		return (this.getEntityProperties().get(groupName + "-until") != null);
	}

	/**
	 * Obtient la date de fin du groupe.
	 *
	 * @param groupName Nom du groupe
	 * @return Date() de fin
	 */
	public Date getGroupEnd(String groupName) {
		String data = this.getEntityProperties().get(groupName + "-until");
		if (data == null)
			return null; // Pas de fin

		try {
			return new Date(Long.parseLong(data));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public boolean isGroupActive(String groupName) {
		Date end = getGroupEnd(groupName);
		if (end == null)
			return true;
		return new Date().before(end);
	}

	@Override
	public TreeSet<PermissionGroup> getParents() {
		cleanDisabledParents();
		return super.getParents();
	}

	public boolean inGroup(String groupName) {
		for (PermissionGroup g : getParents()) {
			if (g.getGroupName().equals(groupName)) return true;
		}
		return false;
	}

	public void addParent(PermissionGroup parent, Date end) {
		super.addParent(parent);
		this.setProperty(parent.getGroupName() + "-until", "" + end.getTime());
	}

	public void cleanDisabledParents() {
		ArrayList<PermissionGroup> remove = new ArrayList<>();
		for (PermissionGroup g : parents)
			if (!isGroupActive(g.getGroupName())) remove.add(g);

		this.parents.removeAll(remove);

		if (this.parents.size() == 0) {
			addParent(PermissionsAPI.permissionsAPI.getDefGroup());
		}
	}

	public void addParent(PermissionGroup parent, int durationInSeconds) {
		super.addParent(parent);
		Date end = new Date();
		end.setTime(end.getTime() + (durationInSeconds * 1000));
		this.setProperty(parent.getGroupName() + "-until", "" + end.getTime());
	}
}
