package net.zyuiop.crosspermissions.api.permissions;

import net.zyuiop.crosspermissions.api.database.IDatabaseManager;

import java.util.*;

public abstract class PermissionEntity {

	/**
	 * Les parents sont triés par ordre d'importance
	 */
	protected TreeSet<PermissionGroup> parents = new TreeSet<>(
			(o2, o1) -> {
				// Tri à l'envers du swag.
				if (o2 == null && o1 == null) {
					return 0;
				}

				if (o2 == null) {
					return -1;
				}

				if (o1 == null) {
					return 1;
				}


				return Integer.compare(o1.getLadder(), o2.getLadder());
			});

	protected UUID entityid;

	protected int databaseId = -1; // sql only

	protected IDatabaseManager manager;

	protected Map<String, Boolean> permissions = new HashMap<>();

	protected Map<String, String> properties = new HashMap<>();

	protected String type;

	public PermissionEntity(IDatabaseManager manager, UUID entityId) {
		this.manager = manager;
		this.entityid = entityId;
	}

	public PermissionEntity(IDatabaseManager manager, UUID entityId, Map<String, Boolean> permissions, Map<String, String> properties) {
		this.permissions = permissions;
		this.properties = properties;
		this.manager = manager;
		this.entityid = entityId;
	}

	public PermissionEntity(IDatabaseManager manager, UUID entityId, Collection<PermissionGroup> parents, Map<String, Boolean> permissions, Map<String, String> properties) {
		this.permissions = permissions;
		this.properties = properties;
		this.parents.addAll(parents);
		this.manager = manager;
		this.entityid = entityId;
	}

	public TreeSet<PermissionGroup> getParents() {
		return parents;
	}

	/**
	 * Attention risque de boucle infinie... Eviter les multi héritages xD
	 * Récupère toutes les permissions, dont les permissions héritées
	 *
	 * @return
	 */
	public HashMap<String, Boolean> getPermissions() {
		HashMap<String, Boolean> ret = new HashMap<>();
		for (PermissionGroup g : getParents())
			ret.putAll(g.getPermissions());
		ret.putAll(permissions);
		return ret;
	}

	/**
	 * Retourne toutes les permissions propres au groupe (sans inclure les perms' des parents)
	 *
	 * @return
	 */
	public Map<String, Boolean> getEntityPermissions() {
		return permissions;
	}

	public void setPermission(String permname, Boolean value) {
		this.permissions.put(permname, value);
		this.manager.setPermission(this, permname, value);
	}

	public void deletePermission(String permname) {
		this.properties.remove(permname);
		this.manager.deletePermission(this, permname);
	}

	/**
	 * Attention risque de boucle infinie... Eviter les multi héritages xD
	 * Retourne toutes les propriétés du groupe (parents inclus)
	 *
	 * @return
	 */
	public HashMap<String, String> getProperties() {
		HashMap<String, String> ret = new HashMap<>();

		for (PermissionGroup g : getParents())
			ret.putAll(g.getProperties());
		ret.putAll(properties);
		return ret;
	}

	/**
	 * Retourne les propriétés du groupe (parents non inclus)
	 *
	 * @return
	 */
	public Map<String, String> getEntityProperties() {
		return properties;
	}

	/**
	 * Retourne la valeur d'une option de l'entité ou de ses parents
	 *
	 * @param name Le nom de la propriété
	 * @return la valeur de la propriété ou <b>null</b> si elle n'existe pas
	 */
	public String getProperty(String name) {
		return getProperties().get(name);
	}

	public void setProperty(String name, String value) {
		this.properties.put(name, value);
		this.manager.setProperty(this, name, value);
	}

	public void deleteProperty(String name) {
		this.properties.remove(name);
		this.manager.deleteProperty(this, name);
	}

	public boolean hasPermission(String name) {
		HashMap<String, Boolean> perms = getPermissions();
		if (perms.containsKey("*") && perms.get("*"))
			return true;
		Boolean val = perms.get(name);
		return (val != null) ? val : false;
	}

	public void addParent(PermissionGroup g) {
		this.parents.add(g);
		this.manager.addParent(this, g);
	}

	public void removeParent(PermissionGroup g) {
		this.parents.remove(g);
		this.manager.removeParent(this, g);
	}

	public UUID getEntityID() {
		return this.entityid;
	}

	public String getDbPrefix() {
		return this.type + ":";
	}

	public String getType() {
		return type;
	}

	public int getDatabaseId() {
		return databaseId;
	}

	public void setDatabaseId(int databaseId) {
		this.databaseId = databaseId;
	}
}
