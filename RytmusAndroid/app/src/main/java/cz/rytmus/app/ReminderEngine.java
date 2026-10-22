package cz.rytmus.app;

import android.Manifest;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;

public final class ReminderEngine {
    private static final String CHANNEL = "rytmus_unlock_reminders_v2";
    private static final int ID = 4007;
    private ReminderEngine() {}

    private static NotificationManager manager(Context c) {
        return (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    static void ensureChannel(Context c) {
        NotificationManager nm = manager(c);
        if (nm == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL, "Připomínky při používání telefonu", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Připomínky při odemčení, dokud nejsou splněné všechny čtyři denní úkoly.");
        nm.createNotificationChannel(channel);
    }

    static void cancel(Context c) {
        NotificationManager nm = manager(c);
        if (nm != null) nm.cancel(ID);
    }

    /** User explicitly requested an alert at EVERY unlock, even if it is soon after the last one. */
    static boolean remindForUnlock(Context c) {
        Store.initialize(c);
        if (!Store.remindersEnabled(c) || Store.allDoneToday(c)) {
            cancel(c);
            return false;
        }
        if (!phoneUnlocked(c)) return false;
        if (!notificationsAllowed(c)) return false;
        show(c, false);
        Store.prefs(c).edit().putLong("lastReminderAt", System.currentTimeMillis()).apply();
        return true;
    }

    private static boolean phoneUnlocked(Context c) {
        PowerManager power = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        KeyguardManager keyguard = (KeyguardManager) c.getSystemService(Context.KEYGUARD_SERVICE);
        return power != null && power.isInteractive() &&
                (keyguard == null || !keyguard.isKeyguardLocked());
    }

    private static boolean notificationsAllowed(Context c) {
        NotificationManager nm = manager(c);
        if (nm == null || !nm.areNotificationsEnabled()) return false;
        return Build.VERSION.SDK_INT < 33 || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    static void maybeRemind(Context c) {
        Store.initialize(c);
        if (!Store.remindersEnabled(c) || Store.allDoneToday(c)) {
            cancel(c);
            return;
        }
        if (!phoneUnlocked(c) || !notificationsAllowed(c)) return;
        long now = System.currentTimeMillis();
        long last = Store.prefs(c).getLong("lastReminderAt", 0L);
        if (now - last < Store.frequency(c) * 60_000L && now >= last) return;
        show(c, false);
        Store.prefs(c).edit().putLong("lastReminderAt", now).apply();
    }

    private static String unfinishedHabits(int mask) {
        String[] names = {"modlitba", "běh/procházka", "studium", "cvičení"};
        StringBuilder todo = new StringBuilder();
        int remaining = 4 - Integer.bitCount(mask & 15);
        for (int i = 0, written = 0; i < names.length; i++) {
            if ((mask & (1 << i)) != 0) continue;
            if (written > 0) todo.append(written == remaining - 1 ? " a " : ", ");
            todo.append(names[i]);
            written++;
        }
        return remaining == 0 ? "Dnes máš hotovo!" : "Ještě zbývá: " + todo + ". Dokonči svůj Rytmus.";
    }

    static void show(Context c, boolean test) {
        ensureChannel(c);
        NotificationManager nm = manager(c);
        if (!notificationsAllowed(c)) return;
        Intent intent = new Intent(c, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(c, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        int mask = Store.todayMask(c);
        String title = test ? "Rytmus · test připomínky"
                : "Rytmus · zbývá " + (4 - Integer.bitCount(mask)) + " ze 4 ✦";
        String message = test ? "Takto tě Rytmus upozorní, když používáš telefon."
                : unfinishedHabits(mask);
        Notification notification = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setOnlyAlertOnce(false)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setColor(0xFFB8EF91)
                .setShowWhen(true)
                .build();
        // Recreate rather than just update: a notification already visible from a
        // previous unlock should have a fresh chance to alert on the next unlock.
        nm.cancel(ID);
        nm.notify(ID, notification);
    }
}
