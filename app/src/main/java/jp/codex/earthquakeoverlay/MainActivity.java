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
import android.widget.ScrollView;

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
    Spinner prefecture, city, detail, displayCondition, alertType, alertPrefecture, alertCity, alertScale;
    TextView status, summary;
    final ArrayList<String> prefectures = new ArrayList<>();
    final ArrayList<String> cities = new ArrayList<>();
    final ArrayList<String> alertCities = new ArrayList<>();
    final LinkedHashMap<String, ArrayList<String>> citiesByPref = new LinkedHashMap<>();
    ArrayAdapter<String> cityAdapter, alertCityAdapter;

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

        summary = new TextView(this);
        summary.setTextColor(Color.WHITE);
        summary.setTextSize(16);
        summary.setPadding(0, 14, 0, 18);
        box.addView(summary);

        TextView hint = new TextView(this);
        hint.setText("自宅地域を選択してください。自宅は震度1から、その他は震度3以上を表示します。");
        hint.setTextColor(Color.LTGRAY);
        hint.setPadding(0, 16, 0, 16);
        box.addView(hint);

        box.addView(section("基本設定"));
        box.addView(description("地震情報を表示する基本ルールを選びます。"));
        box.addView(label("通常の表示条件"));
        displayCondition = spinner(new String[]{"自宅：震度1以上／その他：震度3以上", "全国：震度1以上", "全国：震度3以上", "自宅地域のみ"});
        box.addView(displayCondition);
        displayCondition.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { updateSummary(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { updateSummary(); }
        });

        box.addView(section("自宅地域"));
        box.addView(description("自宅地域は震度1以上、その他の地域は震度3以上を表示します。"));
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

        city.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { updateSummary(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { updateSummary(); }
        });

        prefecture.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateCities(prefecture.getSelectedItem() == null ? "" : prefecture.getSelectedItem().toString());
                updateSummary();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        box.addView(section("特定地域の追加通知"));
        box.addView(description("通常の表示条件とは別に、指定地域で指定震度以上を表示します。"));
        box.addView(label("追加通知の種類"));
        alertType = spinner(new String[]{"追加通知なし", "都道府県", "市区町村"});
        box.addView(alertType);
        box.addView(label("通知対象の都道府県"));
        alertPrefecture = spinner(prefectures.toArray(new String[0]));
        box.addView(alertPrefecture);
        box.addView(label("通知対象の市区町村"));
        alertCity = new Spinner(this);
        alertCityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, alertCities);
        alertCityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        alertCity.setAdapter(alertCityAdapter);
        box.addView(alertCity);
        box.addView(label("通知する震度"));
        alertScale = spinner(new String[]{"震度1以上", "震度2以上", "震度3以上", "震度4以上", "震度5弱以上", "震度5強以上", "震度6弱以上", "震度6強以上", "震度7以上"});
        box.addView(alertScale);
        alertScale.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { updateSummary(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { updateSummary(); }
        });
        alertPrefecture.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateAlertCities(alertPrefecture.getSelectedItem() == null ? "" : alertPrefecture.getSelectedItem().toString());
                updateSummary();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        alertType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { updateAlertControls(); updateSummary(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { updateAlertControls(); updateSummary(); }
        });

        box.addView(section("表示方法"));
        box.addView(description("各地の震度をどこまで細かく表示するかを選びます。"));
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
            String alertPref = alertPrefecture.getSelectedItem() == null ? "" : alertPrefecture.getSelectedItem().toString();
            String alertCityName = alertCity.getSelectedItem() == null ? "" : alertCity.getSelectedItem().toString();
            getSharedPreferences("settings", 0).edit().putString("pref", pref).putString("city", selectedCity)
                    .putInt("detail", detail.getSelectedItemPosition())
                    .putInt("displayCondition", displayCondition.getSelectedItemPosition())
                    .putInt("alertType", alertType.getSelectedItemPosition())
                    .putString("alertPref", alertPref).putString("alertCity", alertCityName)
                    .putInt("alertScale", alertScale.getSelectedItemPosition()).apply();
            start();
            updateSummary();
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
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(box);
        setContentView(scroll);

        String savedPref = getSharedPreferences("settings", 0).getString("pref", "");
        String savedCity = getSharedPreferences("settings", 0).getString("city", "");
        int prefIndex = findIndex(prefectures, savedPref);
        if (prefIndex >= 0) prefecture.setSelection(prefIndex);
        updateCities(prefectures.isEmpty() ? "" : prefectures.get(Math.max(0, prefIndex)));
        int cityIndex = findIndex(cities, savedCity);
        if (cityIndex >= 0) city.setSelection(cityIndex);
        android.content.SharedPreferences settings = getSharedPreferences("settings", 0);
        displayCondition.setSelection(settings.getInt("displayCondition", 0));
        alertType.setSelection(settings.getInt("alertType", 0));
        int alertPrefIndex = findIndex(prefectures, settings.getString("alertPref", ""));
        if (alertPrefIndex >= 0) alertPrefecture.setSelection(alertPrefIndex);
        updateAlertCities(alertPrefecture.getSelectedItem() == null ? "" : alertPrefecture.getSelectedItem().toString());
        int alertCityIndex = findIndex(alertCities, settings.getString("alertCity", ""));
        if (alertCityIndex >= 0) alertCity.setSelection(alertCityIndex);
        alertScale.setSelection(settings.getInt("alertScale", 2));
        updateAlertControls();
        updateSummary();
        status.setText(Settings.canDrawOverlays(this) ? "権限：許可済み" : "権限：未許可");
    }

    TextView section(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(20);
        v.setPadding(0, 24, 0, 6);
        return v;
    }

    TextView description(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.LTGRAY);
        v.setPadding(0, 0, 0, 8);
        return v;
    }

    void updateAlertControls() {
        if (alertType == null) return;
        int type = alertType.getSelectedItemPosition();
        boolean prefEnabled = type >= 1;
        boolean cityEnabled = type == 2;
        alertPrefecture.setEnabled(prefEnabled);
        alertCity.setEnabled(cityEnabled);
        alertScale.setEnabled(prefEnabled);
        alertPrefecture.setAlpha(prefEnabled ? 1.0f : 0.45f);
        alertCity.setAlpha(cityEnabled ? 1.0f : 0.45f);
        alertScale.setAlpha(prefEnabled ? 1.0f : 0.45f);
    }

    String selected(Spinner spinner) {
        return spinner == null || spinner.getSelectedItem() == null ? "" : spinner.getSelectedItem().toString();
    }

    void updateSummary() {
        if (summary == null || displayCondition == null || alertType == null) return;
        String text = "現在の設定\n" + selected(displayCondition);
        text += "\n自宅：" + selected(prefecture) + " " + selected(city);
        if (alertType.getSelectedItemPosition() == 0) {
            text += "\n追加通知：なし";
        } else if (alertType.getSelectedItemPosition() == 1) {
            text += "\n追加通知：" + selected(alertPrefecture) + "・" + selected(alertScale);
        } else {
            text += "\n追加通知：" + selected(alertPrefecture) + " " + selected(alertCity) + "・" + selected(alertScale);
        }
        summary.setText(text);
    }

    Spinner spinner(String[] values) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        return s;
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

    void updateAlertCities(String pref) {
        alertCities.clear();
        ArrayList<String> list = citiesByPref.get(pref);
        if (list != null) alertCities.addAll(list);
        if (alertCityAdapter != null) alertCityAdapter.notifyDataSetChanged();
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
