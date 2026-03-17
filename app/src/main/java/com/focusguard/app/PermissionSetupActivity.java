package com.focusguard.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.widget.TextView;

public class PermissionSetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_setup);

        findViewById(R.id.btnAccessibility).setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });
        findViewById(R.id.btnUsageStats).setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        });
        findViewById(R.id.btnOverlay).setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermStatus();
    }

    private void updatePermStatus() {
        TextView tvA = findViewById(R.id.tvAccessStatus);
        TextView tvU = findViewById(R.id.tvUsageStatus);
        TextView tvO = findViewById(R.id.tvOverlayStatus);

        boolean accessOk = isAccessibilityEnabled();
        tvA.setText(accessOk ? "✅ Granted" : "❌ Not granted – TAP TO ENABLE");
        tvA.setTextColor(accessOk ? 0xFF4CAF50 : 0xFFFF5722);

        boolean usageOk = hasUsageAccess();
        tvU.setText(usageOk ? "✅ Granted" : "❌ Not granted – TAP TO ENABLE");
        tvU.setTextColor(usageOk ? 0xFF4CAF50 : 0xFFFF5722);

        boolean overlayOk = Settings.canDrawOverlays(this);
        tvO.setText(overlayOk ? "✅ Granted" : "❌ Not granted – TAP TO ENABLE");
        tvO.setTextColor(overlayOk ? 0xFF4CAF50 : 0xFFFF5722);
    }

    private boolean isAccessibilityEnabled() {
        try {
            int e = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
            String s = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return e == 1 && s != null &&
                   s.contains(getPackageName() + "/.FocusAccessibilityService");
        } catch (Exception e) { return false; }
    }

    private boolean hasUsageAccess() {
        try {
            android.app.AppOpsManager aom =
                    (android.app.AppOpsManager) getSystemService(APP_OPS_SERVICE);
            int mode = aom.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), getPackageName());
            return mode == android.app.AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) { return false; }
    }
}
