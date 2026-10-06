package in.gbtsolutions.inventoryhub.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "units_of_measure", indices = {@Index(value = "name", unique = true)})
public class UnitOfMeasure {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "uom_id")
    public int uomId;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "is_default", defaultValue = "0")
    public boolean isDefault;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public UnitOfMeasure(String name, String description, boolean isDefault) {
        this.name = name != null ? name.trim() : "";
        this.description = description != null ? description.trim() : "";
        this.isDefault = isDefault;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    @Ignore
    public UnitOfMeasure(String name, String description) {
        this(name, description, false);
    }

    @Ignore
    public UnitOfMeasure() {
    }
}
