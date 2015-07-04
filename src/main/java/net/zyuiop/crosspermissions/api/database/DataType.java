package net.zyuiop.crosspermissions.api.database;

public enum DataType {
	
	OPTION("options"),
	PERMISSION("perms"),
	PARENT("parents");
	
	private String dbkey;
	private DataType(String dbkey) {
		this.dbkey = dbkey;
	}
	
	public String getKey() {
		return dbkey;
	}
} 
