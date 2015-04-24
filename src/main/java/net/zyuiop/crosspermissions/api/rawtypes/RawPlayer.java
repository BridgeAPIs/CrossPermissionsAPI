package net.zyuiop.crosspermissions.api.rawtypes;

import java.util.UUID;

public interface RawPlayer {
	public void setPermission(String permission, boolean value);
	
	public UUID getUniqueId();

    public void clearPermissions();
}
