package in.gbtsolutions.inventoryhub.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import in.gbtsolutions.inventoryhub.helpers.CategoryIconHelper;

@Entity(tableName = "categories", indices = {@Index(value = "category_name", unique = true)})
public class Category {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "category_id")
    public int categoryId;

    @ColumnInfo(name = "category_name")
    public String categoryName;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "icon", defaultValue = "'general'")
    public String icon;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
    public Category(String categoryName, String description, String icon) {
        this.categoryName = categoryName;
        this.description = description;
        this.icon = CategoryIconHelper.sanitizeIconKey(icon);
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    @Ignore
    public Category(String categoryName, String description) {
        this(categoryName, description, CategoryIconHelper.DEFAULT_ICON_KEY);
    }

    @Ignore
    public Category() {
    }
}
