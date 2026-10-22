package cz.rytmus.app;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;

/**
 * User-enabled accessibility service that remains bound by Android.
 * It listens to system unlock/screen broadcasts via a dynamically registered
 * receiver (manifest receivers don't work for ACTION_USER_PRESENT on modern Android).
 * No window content, text, passwords or user interactions are read or stored.
 */
public final class ActivePhoneService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean receiverRegistered = false;
    private long lastUnlockSignal = -10_000L;

    private final BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (Intent.ACTION_USER_PRESENT.equals(action)) {
                // Allow a moment for the keyguard state to settle after unlocking.
                handler.postDelayed(() -> remindOnUnlock(), 250);
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                // Fallback for devices which don't emit USER_PRESENT on simple wake.
                // Never alert while the lock screen is displayed.
                handler.postDelayed(() -> {
                    KeyguardManager keyguard = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                    PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
                    if ((keyguard == null || !keyguard.isKeyguardLocked()) &&
                            power != null && power.isInteractive()) {
                        remindOnUnlock();
                    }
                }, 1500);
            }
        }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        if (receiverRegistered) return;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(unlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(unlockReceiver, filter);
        }
        receiverRegistered = true;
    }

    private void remindOnUnlock() {
        long now = SystemClock.elapsedRealtime();
        // A single wake can emit SCREEN_ON and USER_PRESENT. Show one reminder.
        if (now - lastUnlockSignal < 3000) return;
        if (ReminderEngine.remindForUnlock(this)) lastUnlockSignal = now;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        // Deliberately ignore accessibility events, text, and app names.
        // The service is only a system-bound host for our unlock broadcast receiver.
    }

    @Override public void onInterrupt() { }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (receiverRegistered) {
            unregisterReceiver(unlockReceiver);
            receiverRegistered = false;
        }
        super.onDestroy();
    }
}
