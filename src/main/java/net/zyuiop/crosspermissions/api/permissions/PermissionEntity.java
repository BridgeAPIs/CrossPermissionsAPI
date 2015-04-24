package net.zyuiop.crosspermissions.api.permissions;

import net.zyuiop.crosspermissions.api.database.DataType;
import net.zyuiop.crosspermissions.api.database.DatabaseManager;

import java.util.*;

public abstract class PermissionEntity {
	
	/**
	 * Les parents sont triés par ordre d'importance
	 */
	protected TreeSet<PermissionGroup> parents = new TreeSet<>(
			new Comparator<PermissionGroup>(){
			public int compare(PermissionGroup o2, PermissionGroup o1) { 
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
			}
	});

	protected UUID entityid;
	
	protected DatabaseManager manager;
	
	protected HashMap<String, Boolean> permissions = new HashMap<>();
	
	protected HashMap<String, String> properties = new HashMap<>();
	
	protected String db_prefix;
	
	public PermissionEntity(DatabaseManager manager, UUID entityId) {
		this.manager = manager;
		this.entityid = entityId;
	}
	
	public PermissionEntity(DatabaseManager manager,  UUID entityId, HashMap<String, Boolean> permissions, HashMap<String, String> properties) {
		this.permissions = permissions;
		this.properties = properties;
		this.manager = manager;
		this.entityid = entityId;
	}

    public PermissionEntity(DatabaseManager manager,  UUID entityId, List<PermissionGroup> parents, HashMap<String, Boolean> permissions, HashMap<String, String> properties) {
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
	 * @return
	 */
	public HashMap<String, Boolean> getEntityPermissions() {
		return permissions;
	}
	
	public void setPermission(String permname, Boolean value) {
		this.permissions.put(permname, value);
		this.manager.hset(this, DataType.PERMISSION, permname, value.toString());
	}
	
	public void deletePermission(String permname) {
		this.properties.remove(permname);
		this.manager.hdel(this, DataType.PERMISSION, permname);
	}
	
	/**
	 * Attention risque de boucle infinie... Eviter les multi héritages xD
	 * Retourne toutes les propriétés du groupe (parents inclus)
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
	 * @return
	 */
	public HashMap<String, String> getEntityProperties() {
		return properties;
	}
	
	/**
	 * Retourne la valeur d'une option de l'entité ou de ses parents
	 * @param name Le nom de la propriété
	 * @return la valeur de la propriété ou <b>null</b> si elle n'existe pas
	 */
	public String getProperty(String name) {
		return getProperties().get(name);
	}
	
	public void setProperty(String name, String value) {
		this.properties.put(name, value);
		this.manager.hset(this, DataType.OPTION, name, value);
	}
	
	public void deleteProperty(String name) {
		this.properties.remove(name);
		this.manager.hdel(this, DataType.OPTION, name);
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
		this.manager.lput(this, DataType.PARENT, g.getEntityID().toString());
	}
	
	public void removeParent(PermissionGroup g) {
		this.parents.remove(g);
		this.manager.lrem(this, DataType.PARENT, g.getEntityID().toString());
	}
	
	public UUID getEntityID() {
		return this.entityid;
	}
	
	public String getDbPrefix() {
		return this.db_prefix;
	}
}
