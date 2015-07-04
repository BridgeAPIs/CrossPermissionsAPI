package net.zyuiop.crosspermissions.api.rawtypes;

/**
 * @author zyuiop
 */
public interface RefreshHook {
    /**
     * Called after the refresh
     * May be usefull if you want to do some stuff AFTER the refresh
     */
    public default void onRefreshHook() {
        // Do nothing
    }
}
