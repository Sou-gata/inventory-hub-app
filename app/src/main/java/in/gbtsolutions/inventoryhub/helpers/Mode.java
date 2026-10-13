package in.gbtsolutions.inventoryhub.helpers;

public enum Mode {
    ONLINE,
    OFFLINE;

    public enum OnlineTier {
        SMALL,
        LARGE
    }

    public enum OfflineTier {
        STANDARD,
        UNLIMITED
    }
}
