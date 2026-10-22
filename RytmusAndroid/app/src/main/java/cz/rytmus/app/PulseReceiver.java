package cz.rytmus.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public final class PulseReceiver extends BroadcastReceiver {
    static PendingIntent pending(Context c) {
        return PendingIntent.getBroadcast(c, 210, new Intent(c, PulseReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void schedule(Context c) {
        if (!Store.remindersEnabled(c)) return;
        AlarmManager alarm = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        alarm.cancel(pending(c));
        // Inexact check: may be delayed by the OS, does not wake a sleeping phone.
        alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES, pending(c));
    }

    static void cancel(Context c) {
        AlarmManager alarm = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (alarm != null) alarm.cancel(pending(c));
    }

    @Override public void onReceive(Context context, Intent intent) {
        ReminderEngine.maybeRemind(context);
    }
}
