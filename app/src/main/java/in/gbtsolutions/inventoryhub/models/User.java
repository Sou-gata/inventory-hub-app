package in.gbtsolutions.inventoryhub.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = {
        @Index(value = "contact", unique = true),
        @Index(value = "email", unique = true),
})
public class User {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "username")
    public String username;

    @ColumnInfo(name = "password")
    public String password;

    @ColumnInfo(name = "email")
    public String email;

    @ColumnInfo(name = "contact")
    public String contact;

    @ColumnInfo(name = "role")
    public String role;

    @ColumnInfo(name = "is_active")
    public boolean isActive;

    @androidx.room.Ignore
    public User() {
    }

    public User(String name, String username, String password, String email, String contact, String role, boolean isActive) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.email = email;
        this.contact = contact;
        this.role = role;
        this.isActive = isActive;
    }
}
