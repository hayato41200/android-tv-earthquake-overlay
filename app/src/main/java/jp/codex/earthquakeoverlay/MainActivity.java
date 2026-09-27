package jp.codex.earthquakeoverlay;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    Spinner prefecture, city, detail;
    TextView status;
    final ArrayList<String> prefectures = new ArrayList<>();
    final ArrayList<String> cities = new ArrayList<>();
    final LinkedHashMap<String, ArrayList<String>> citiesByPref = new LinkedHashMap<>();
    ArrayAdapter<String> cityAdapter;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        loadLocations();
        buildUi();
    }

    void buildUi() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(48, 32, 48, 32);
        box.setBackgroundColor(Color.rgb(25, 25, 25));

        TextView title = new TextView(this);
        title.setText("地震テロップ設定");
        title.setTextSize(26);
        title.setTextColor(Color.WHITE);
        box.addView(title);

        TextView hint = new TextView(this);
        hint.setText("自宅地域を選択してください。自宅は震度1から、その他は震度3以上を表示します。");
        hint.setTextColor(Color.LTGRAY);
        hint.setPadding(0, 16, 0, 16);
        box.addView(hint);

        TextView prefLabel = label("都道府県");
        box.addView(prefLabel);
        prefecture = new Spinner(this);
        prefecture.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, prefectures));
        ((ArrayAdapter<?>) prefecture.getAdapter()).setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        box.addView(prefecture);

        TextView cityLabel = label("市区町村");
        box.addView(cityLabel);
        city = new Spinner(this);
        cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cities);
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        city.setAdapter(cityAdapter);
        box.addView(city);

        prefecture.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateCities(prefecture.getSelectedItem() == null ? "" : prefecture.getSelectedItem().toString());
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        TextView detailLabel = label("各地の震度の表示粒度");
        box.addView(detailLabel);
        detail = new Spinner(this);
        ArrayAdapter<String> detailAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"都道府県まで", "市区町村まで", "観測地点まで"});
        detailAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        detail.setAdapter(detailAdapter);
        detail.setSelection(getSharedPreferences("settings", 0).getInt("detail", 1));
        box.addView(detail);

        Button save = new Button(this);
        save.setText("保存して監視を開始");
        save.setOnClickListener(v -> {
            String pref = prefecture.getSelectedItem() == null ? "" : prefecture.getSelectedItem().toString();
            String selectedCity = city.getSelectedItem() == null ? "" : city.getSelectedItem().toString();
            getSharedPreferences("settings", 0).edit().putString("pref", pref).putString("city", selectedCity)
                    .putInt("detail", detail.getSelectedItemPosition()).apply();
            start();
            status.setText("保存しました　" + pref + " " + selectedCity);
        });
        box.addView(save);

        Button permission = new Button(this);
        permission.setText("オーバーレイ権限を設定");
        permission.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()))));
        box.addView(permission);

        Button test = new Button(this);
        test.setText("表示テスト");
        test.setOnClickListener(v -> { start(); sendBroadcast(new Intent(OverlayService.TEST_ACTION)); });
        box.addView(test);

        Button latest = new Button(this);
        latest.setText("最新APIでテスト");
        latest.setOnClickListener(v -> { start(); sendBroadcast(new Intent(OverlayService.TEST_LATEST_ACTION)); });
        box.addView(latest);

        Button update = new Button(this);
        update.setText("更新を確認してインストール");
        update.setOnClickListener(v -> {
            UpdateManager.openInstallPermission(this);
            UpdateManager.check(this, (ok, tag) -> runOnUiThread(() -> {
                if (ok) UpdateManager.downloadAndInstall(this);
                else Toast.makeText(this, "新しい更新はありません", Toast.LENGTH_SHORT).show();
            }));
        });
        box.addView(update);

        status = new TextView(this);
        status.setTextColor(Color.LTGRAY);
        status.setPadding(0, 20, 0, 0);
        box.addView(status);
        setContentView(box);

        String savedPref = getSharedPreferences("settings", 0).getString("pref", "");
        String savedCity = getSharedPreferences("settings", 0).getString("city", "");
        int prefIndex = findIndex(prefectures, savedPref);
        if (prefIndex >= 0) prefecture.setSelection(prefIndex);
        updateCities(prefectures.isEmpty() ? "" : prefectures.get(Math.max(0, prefIndex)));
        int cityIndex = findIndex(cities, savedCity);
        if (cityIndex >= 0) city.setSelection(cityIndex);
        status.setText(Settings.canDrawOverlays(this) ? "権限：許可済み" : "権限：未許可");
    }

    TextView label(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.LTGRAY);
        v.setPadding(0, 10, 0, 4);
        return v;
    }

    void loadLocations() {
        try (InputStream in = getAssets().open("locations.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) text.append(line);
            JSONArray data = new JSONArray(text.toString());
            for (int i = 0; i < data.length(); i++) {
                JSONObject row = data.getJSONObject(i);
                String pref = row.optString("pref", "").trim();
                String cityName = row.optString("city", "").trim();
                if (pref.isEmpty() || cityName.isEmpty()) continue;
                ArrayList<String> list = citiesByPref.get(pref);
                if (list == null) { list = new ArrayList<>(); citiesByPref.put(pref, list); prefectures.add(pref); }
                if (!list.contains(cityName)) list.add(cityName);
            }
        } catch (Exception e) {
            Toast.makeText(this, "自治体リストを読み込めませんでした", Toast.LENGTH_LONG).show();
        }
    }

    void updateCities(String pref) {
        cities.clear();
        ArrayList<String> list = citiesByPref.get(pref);
        if (list != null) cities.addAll(list);
        cityAdapter.notifyDataSetChanged();
    }

    int findIndex(ArrayList<String> values, String wanted) {
        String normalized = normalize(wanted);
        if (normalized.isEmpty()) return -1;
        for (int i = 0; i < values.size(); i++) if (normalize(values.get(i)).equals(normalized)) return i;
        return -1;
    }

    static String normalize(String value) { return value == null ? "" : value.replace(" ", "").replace("　", "").trim(); }

    void start() {
        Intent i = new Intent(this, OverlayService.class);
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }
}
