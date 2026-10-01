package in.gbtsolutions.inventoryhub.online.config;

public enum AppMode {
    OFFLINE("offline", "Offline Mode (Local Database)"),
    ONLINE("online", "Online Mode (Cloud Server)");

    private final String key;
    private final String displayName;

    AppMode(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AppMode fromKey(String key) {
        if ("online".equalsIgnoreCase(key)) {
            return ONLINE;
        }
        return OFFLINE;
    }
}
