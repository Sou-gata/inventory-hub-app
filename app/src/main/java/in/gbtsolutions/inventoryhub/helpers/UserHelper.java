package in.gbtsolutions.inventoryhub.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Configurations;
import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.models.User;

public class UserHelper {

    public static final String KEY_CURRENT_USER_ID = "current_session_user_id";

    /**
     * Resolves the active user ID:
     * 1. Checks in-memory GlobalStore.
     * 2. Checks current session user ID in SharedPreferences.
     * 3. Checks remembered user ID in SharedPreferences.
     * 4. Fallback to 1 (default admin).
     */
    public static long getCurrentUserId(@Nullable Context context) {
        User loggedInUser = GlobalStore.getInstance().getLoggedInUser();
        if (loggedInUser != null && loggedInUser.id > 0) {
            return loggedInUser.id;
        }

        if (context != null) {
            try {
                SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(Configurations.PREF_NAME, Context.MODE_PRIVATE);
                long sessionUid = prefs.getLong(KEY_CURRENT_USER_ID, -1);
                if (sessionUid > 0) {
                    return sessionUid;
                }
                long rememberedUid = prefs.getLong("user_id", -1);
                if (rememberedUid > 0) {
                    return rememberedUid;
                }
            } catch (Exception ignored) {
            }
        }
        return 1L;
    }

    public static void saveLoggedInUser(@Nullable Context context, @Nullable User user) {
        if (user == null) return;
        GlobalStore.getInstance().setLoggedInUser(user);
        USER_CACHE.put((long) user.id, user);
        if (context != null) {
            try {
                SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(Configurations.PREF_NAME, Context.MODE_PRIVATE);
                prefs.edit().putLong(KEY_CURRENT_USER_ID, user.id).apply();
            } catch (Exception ignored) {
            }
        }
    }

    public static void clearLoggedInUser(@Nullable Context context) {
        GlobalStore.getInstance().clearLoggedInUser();
        if (context != null) {
            try {
                SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(Configurations.PREF_NAME, Context.MODE_PRIVATE);
                prefs.edit().remove(KEY_CURRENT_USER_ID).apply();
            } catch (Exception ignored) {
            }
        }
    }

    @Nullable
    public static User getCurrentUser(@Nullable Context context) {
        User user = GlobalStore.getInstance().getLoggedInUser();
        if (user != null) {
            return user;
        }
        if (context != null) {
            long uid = getCurrentUserId(context);
            if (uid > 0) {
                User cached = USER_CACHE.get(uid);
                if (cached != null) {
                    GlobalStore.getInstance().setLoggedInUser(cached);
                    return cached;
                }
                try {
                    User dbUser = Database.getInstance(context.getApplicationContext()).userDao().getUserById(uid);
                    if (dbUser != null) {
                        GlobalStore.getInstance().setLoggedInUser(dbUser);
                        USER_CACHE.put(uid, dbUser);
                        return dbUser;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static final Map<Long, User> USER_CACHE = new ConcurrentHashMap<>();

    public interface UserCallback {
        void onUserLoaded(@Nullable User user);
    }

    public interface UsersCallback {
        void onUsersLoaded(List<User> users);
    }

    public static void getAllUsersAsync(@Nullable Context context, @NonNull UsersCallback callback) {
        if (context == null) {
            callback.onUsersLoaded(new ArrayList<>());
            return;
        }
        final Context appContext = context.getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            List<User> users = null;
            try {
                users = Database.getInstance(appContext).userDao().getAllUsersSync();
                if (users != null) {
                    for (User u : users) {
                        if (u != null && u.id > 0) {
                            USER_CACHE.put((long) u.id, u);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            final List<User> result = users != null ? users : new ArrayList<>();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onUsersLoaded(result));
        });
    }

    public static void getUserByIdAsync(@Nullable Context context, long userId, @NonNull UserCallback callback) {
        if (context == null || userId <= 0) {
            callback.onUserLoaded(null);
            return;
        }
        User cached = USER_CACHE.get(userId);
        if (cached != null) {
            callback.onUserLoaded(cached);
            return;
        }
        final Context appContext = context.getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            User user = null;
            try {
                user = Database.getInstance(appContext).userDao().getUserById(userId);
                if (user != null) {
                    USER_CACHE.put(userId, user);
                }
            } catch (Exception ignored) {
            }
            final User loadedUser = user;
            new Handler(Looper.getMainLooper()).post(() -> callback.onUserLoaded(loadedUser));
        });
    }

    public static String formatUserNameAndUsername(@Nullable User user, long fallbackUserId) {
        if (user == null) {
            return fallbackUserId > 0 ? "User #" + fallbackUserId : "System / Admin";
        }
        String name = user.name != null ? user.name.trim() : "";
        String username = user.username != null ? user.username.trim() : "";
        if (!name.isEmpty() && !username.isEmpty()) {
            return name + " (@" + username + ")";
        } else if (!name.isEmpty()) {
            return name;
        } else if (!username.isEmpty()) {
            return "@" + username;
        } else {
            return "User #" + user.id;
        }
    }

    public static String formatUserRole(@Nullable User user) {
        if (user != null && user.role != null && !user.role.trim().isEmpty()) {
            return user.role.trim().toUpperCase(java.util.Locale.getDefault());
        }
        return "STAFF";
    }

    public interface OnUserSelectedListener {
        void onUserSelected(@NonNull User selectedUser);
    }

    public static void showUserPickerDialog(
            @NonNull Context context,
            @NonNull String title,
            @Nullable Long currentSelectedUserId,
            @NonNull OnUserSelectedListener listener
    ) {
        getAllUsersAsync(context, users -> {
            if (users == null || users.isEmpty()) {
                android.widget.Toast.makeText(context, "No users found in database.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            String[] displayNames = new String[users.size()];
            int checkedItem = -1;
            for (int i = 0; i < users.size(); i++) {
                User u = users.get(i);
                displayNames[i] = formatUserNameAndUsername(u, u.id) + " [" + formatUserRole(u) + "]";
                if (currentSelectedUserId != null && u.id == currentSelectedUserId) {
                    checkedItem = i;
                }
            }
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                    .setTitle(title)
                    .setSingleChoiceItems(displayNames, checkedItem, (dialog, which) -> {
                        dialog.dismiss();
                        if (which >= 0 && which < users.size()) {
                            listener.onUserSelected(users.get(which));
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}
