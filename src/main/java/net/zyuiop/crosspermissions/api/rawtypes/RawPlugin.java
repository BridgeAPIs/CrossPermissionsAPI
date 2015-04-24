package net.zyuiop.crosspermissions.api.rawtypes;

import net.zyuiop.crosspermissions.api.database.Database;
import redis.clients.jedis.Jedis;

import java.util.UUID;

public interface RawPlugin {
	public void logSevere(String log);
	public void logWarning(String log);
	public void logInfo(String log);
	
	public void runRepeatedTaskAsync(Runnable task, long delay, long timeBeforeRun);
	
	public void runAsync(Runnable task);
	
	public boolean isOnline(UUID player);

    public RawPlayer getPlayer(UUID player);

    public UUID getPlayerId(String name);
    public String getPlayerName(UUID id);
}
