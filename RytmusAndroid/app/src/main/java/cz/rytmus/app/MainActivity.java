package cz.rytmus.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.graphics.Color;

public final class MainActivity extends Activity {
    private static final int NOTIF_PERMISSION_REQUEST = 410;
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Store.initialize(this);
        ReminderEngine.ensureChannel(this);
        PulseReceiver.schedule(this);
        getWindow().setStatusBarColor(Color.rgb(11,18,33));
        getWindow().setNavigationBarColor(Color.rgb(11,18,33));
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(11,18,33));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(false);
        webView.addJavascriptInterface(new Bridge(), "RytmusAndroid");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return !url.startsWith("file:///android_asset/");
            }
        });
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                !Store.prefs(this).getBoolean("permissionPrompted", false)) {
            new Handler(Looper.getMainLooper()).postDelayed(this::askForNotifications, 1200);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        // Opening the app does NOT fulfill the habits or disable reminders.
        // Clear an old notification; it returns on the next unlock until 4/4.
        ReminderEngine.cancel(this);
        if (webView != null) webView.postDelayed(() -> webView.evaluateJavascript(
                "window.refreshFromAndroid && window.refreshFromAndroid()", null), 250);
    }

    @Override protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("RytmusAndroid");
            webView.destroy();
        }
        super.onDestroy();
    }

    private void askForNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Store.prefs(this).edit().putBoolean("permissionPrompted", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIF_PERMISSION_REQUEST);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == NOTIF_PERMISSION_REQUEST && webView != null) {
            webView.evaluateJavascript("window.refreshFromAndroid && window.refreshFromAndroid()", null);
        }
    }

    private final class Bridge {
        @JavascriptInterface public String getData() { return Store.snapshot(MainActivity.this); }

        @JavascriptInterface public void setDay(String date, int mask) {
            Store.saveDay(MainActivity.this, date, mask);
        }

        @JavascriptInterface public void setReminders(boolean enabled, int minutes) {
            Store.setReminderConfig(MainActivity.this, enabled, minutes);
        }

        @JavascriptInterface public void openAccessibilitySettings() {
            runOnUiThread(() -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        }

        @JavascriptInterface public void requestNotificationPermission() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 33) askForNotifications();
                else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
            });
        }

        @JavascriptInterface public void openNotificationSettings() {
            runOnUiThread(() -> startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())));
        }

        @JavascriptInterface public void testNotification() {
            runOnUiThread(() -> ReminderEngine.show(MainActivity.this, true));
        }
    }
}
