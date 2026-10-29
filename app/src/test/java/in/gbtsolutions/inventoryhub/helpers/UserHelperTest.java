package in.gbtsolutions.inventoryhub.helpers;

import com.google.gson.Gson;

import org.junit.Test;

import in.gbtsolutions.inventoryhub.models.User;

import static org.junit.Assert.*;

public class UserHelperTest {

    @Test
    public void testUserSerializationRoundtrip() {
        User user = new User("John Doe", "johndoe", "hashed_pwd", "john@example.com", "9876543210", "admin", true);
        user.id = 42;

        Gson gson = new Gson();
        String json = gson.toJson(user);
        assertNotNull(json);
        assertTrue(json.contains("\"id\":42"));
        assertTrue(json.contains("\"username\":\"johndoe\""));

        User deserialized = gson.fromJson(json, User.class);
        assertNotNull(deserialized);
        assertEquals(42, deserialized.id);
        assertEquals("John Doe", deserialized.name);
        assertEquals("johndoe", deserialized.username);
        assertEquals("john@example.com", deserialized.email);
        assertEquals("9876543210", deserialized.contact);
        assertEquals("admin", deserialized.role);
        assertTrue(deserialized.isActive);
    }

    @Test
    public void testFormatUserNameAndUsername() {
        User user = new User("Alice Smith", "alice", null, null, null, "user", true);
        user.id = 7;

        String formatted = UserHelper.formatUserNameAndUsername(user, 7);
        assertEquals("Alice Smith (@alice)", formatted);

        User noName = new User("", "bob", null, null, null, "user", true);
        noName.id = 8;
        assertEquals("@bob", UserHelper.formatUserNameAndUsername(noName, 8));

        assertEquals("System / Admin", UserHelper.formatUserNameAndUsername(null, 0));
        assertEquals("User #99", UserHelper.formatUserNameAndUsername(null, 99));
    }

    @Test
    public void testFormatUserRole() {
        User admin = new User();
        admin.role = "admin";
        assertEquals("ADMIN", UserHelper.formatUserRole(admin));

        User staff = new User();
        staff.role = "staff";
        assertEquals("STAFF", UserHelper.formatUserRole(staff));

        User noRole = new User();
        assertEquals("STAFF", UserHelper.formatUserRole(noRole));
        assertEquals("STAFF", UserHelper.formatUserRole(null));
    }
}

