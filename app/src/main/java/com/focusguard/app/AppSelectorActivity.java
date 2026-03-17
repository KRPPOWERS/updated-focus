package com.focusguard.app;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.focusguard.app.models.AppInfo;
import com.focusguard.app.utils.PrefsManager;
import java.util.*;

public class AppSelectorActivity extends AppCompatActivity {

    private PrefsManager prefs;
    private List<AppInfo> allApps = new ArrayList<>();
    private List<AppInfo> filtered = new ArrayList<>();
    private AppAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_selector);
        prefs = new PrefsManager(this);

        RecyclerView rv = findViewById(R.id.rvApps);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppAdapter();
        rv.setAdapter(adapter);

        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { filter(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });

        loadApps();

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            Set<String> sel = new HashSet<>();
            for (AppInfo a : allApps) if (a.selected) sel.add(a.packageName);
            prefs.saveBlockedApps(sel);
            Toast.makeText(this, sel.size() + " apps blocked", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void loadApps() {
        Set<String> blocked = prefs.getBlockedApps();
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> installed = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            List<AppInfo> list = new ArrayList<>();
            for (ApplicationInfo ai : installed) {
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                if (ai.packageName.equals(getPackageName())) continue;
                AppInfo info = new AppInfo(ai.packageName,
                        pm.getApplicationLabel(ai).toString(), pm.getApplicationIcon(ai));
                info.selected = blocked.contains(ai.packageName);
                list.add(info);
            }
            list.sort((a, b) -> a.appName.compareToIgnoreCase(b.appName));
            runOnUiThread(() -> {
                allApps.clear(); allApps.addAll(list);
                filtered.clear(); filtered.addAll(list);
                adapter.notifyDataSetChanged();
                findViewById(R.id.progressBar).setVisibility(android.view.View.GONE);
            });
        }).start();
    }

    private void filter(String q) {
        filtered.clear();
        for (AppInfo a : allApps)
            if (q.isEmpty() || a.appName.toLowerCase().contains(q.toLowerCase()))
                filtered.add(a);
        adapter.notifyDataSetChanged();
    }

    class AppAdapter extends RecyclerView.Adapter<AppAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            ImageView ivIcon; TextView tvName, tvPkg; CheckBox cbSelect;
            VH(View v) {
                super(v);
                ivIcon   = v.findViewById(R.id.ivAppIcon);
                tvName   = v.findViewById(R.id.tvAppName);
                tvPkg    = v.findViewById(R.id.tvAppPkg);
                cbSelect = v.findViewById(R.id.cbSelect);
            }
        }
        @Override public VH onCreateViewHolder(ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_app, p, false));
        }
        @Override public void onBindViewHolder(VH h, int pos) {
            AppInfo a = filtered.get(pos);
            h.ivIcon.setImageDrawable(a.icon);
            h.tvName.setText(a.appName);
            h.tvPkg.setText(a.packageName);
            h.cbSelect.setChecked(a.selected);
            View.OnClickListener toggle = v -> {
                a.selected = !a.selected;
                h.cbSelect.setChecked(a.selected);
            };
            h.cbSelect.setOnClickListener(v -> a.selected = h.cbSelect.isChecked());
            h.itemView.setOnClickListener(toggle);
        }
        @Override public int getItemCount() { return filtered.size(); }
    }
}
