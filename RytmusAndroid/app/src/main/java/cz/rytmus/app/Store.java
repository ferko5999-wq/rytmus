package cz.rytmus.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.content.ComponentName;
import android.app.NotificationManager;
import android.os.Build;

import org.json.JSONObject;
import java.time.LocalDate;
import java.util.Map;

public final class Store {
    private static final String PREFS = "rytmus_private_v1";
    private Store() {}

    static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String today() { return LocalDate.now().toString(); }

    static void initialize(Context c) {
        SharedPreferences p = prefs(c);
        if (!p.contains("firstDay")) {
            p.edit().putString("firstDay", today()).putBoolean("reminders", true)
                    .putInt("frequency", 10).apply();
        }
    }

    /** The daily reminder stops only when ALL four habits have been completed. */
    static int todayMask(Context c) {
        return prefs(c).getInt("day_" + today(), 0) & 15;
    }

    static boolean allDoneToday(Context c) {
        return todayMask(c) == 15;
    }

    static boolean remindersEnabled(Context c) {
        return prefs(c).getBoolean("reminders", true);
    }

    static int frequency(Context c) {
        int value = prefs(c).getInt("frequency", 10);
        return value == 5 || value == 20 ? value : 10;
    }

    static void setReminderConfig(Context c, boolean enabled, int frequency) {
        if (frequency != 5 && frequency != 10 && frequency != 20) frequency = 10;
        prefs(c).edit().putBoolean("reminders", enabled).putInt("frequency", frequency).apply();
        if (enabled) PulseReceiver.schedule(c);
        else {
            PulseReceiver.cancel(c);
            ReminderEngine.cancel(c);
        }
    }

    static void saveDay(Context c, String iso, int mask) {
        try {
            LocalDate d = LocalDate.parse(iso);
            if (d.isAfter(LocalDate.now()) || mask < 0 || mask > 15) return;
            SharedPreferences p = prefs(c);
            p.edit().putInt("day_" + d, mask).apply();
            if (d.equals(LocalDate.now())) {
                // Clear stale text after any edit. At the next unlock the reminder
                // will reappear with the CURRENT missing habits, unless 4/4 is done.
                ReminderEngine.cancel(c);
            }
        } catch (Exception ignored) { }
    }

    static boolean accessibilityEnabled(Context c) {
        try {
            String enabled = Settings.Secure.getString(c.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (enabled == null) return false;
            String id = new ComponentName(c, ActivePhoneService.class).flattenToString();
            for (String item : enabled.split(":")) {
                if (id.equalsIgnoreCase(item)) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    static String snapshot(Context c) {
        initialize(c);
        SharedPreferences p = prefs(c);
        try {
            JSONObject days = new JSONObject();
            for (Map.Entry<String, ?> item : p.getAll().entrySet()) {
                if (item.getKey().startsWith("day_") && item.getValue() instanceof Integer) {
                    days.put(item.getKey().substring(4), item.getValue());
                }
            }
            JSONObject response = new JSONObject();
            response.put("days", days);
            response.put("firstDay", p.getString("firstDay", today()));
            response.put("reminders", remindersEnabled(c));
            response.put("frequency", frequency(c));
            response.put("accessibility", accessibilityEnabled(c));
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            boolean notificationsAllowed = nm != null && nm.areNotificationsEnabled();
            if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) notificationsAllowed = false;
            response.put("notificationGranted", notificationsAllowed);
            response.put("completedToday", allDoneToday(c));
            response.put("todayMask", todayMask(c));
            return response.toString();
        } catch (Exception ignored) { return "{}"; }
    }
}
