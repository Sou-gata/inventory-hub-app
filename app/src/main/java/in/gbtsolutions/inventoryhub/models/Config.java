package in.gbtsolutions.inventoryhub.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "configs", indices = {@Index(value = {"config_key"}, unique = true)})
public class Config {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "config_id")
    public int configId;

    @ColumnInfo(name = "config_key")
    public String configKey;

    @ColumnInfo(name = "config_value")
    public String configValue;

    public Config() {
    }

    @Ignore
    public Config(String configKey, String configValue) {
        this.configKey = configKey;
        this.configValue = configValue;
    }

    public int getConfigId() {
        return configId;
    }

    public void setConfigId(int configId) {
        this.configId = configId;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
}
