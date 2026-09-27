package jp.codex.earthquakeoverlay;

import android.app.*; import android.content.*; import android.graphics.Color; import android.net.Uri; import android.os.*; import android.provider.Settings; import android.view.*; import android.widget.*;

public class MainActivity extends Activity {
    EditText prefecture, city; TextView status; Spinner detail;
    @Override public void onCreate(Bundle b) { super.onCreate(b); buildUi(); }
    void buildUi() {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(48,32,48,32); box.setBackgroundColor(Color.rgb(25,25,25));
        TextView title=new TextView(this); title.setText("地震テロップ設定"); title.setTextSize(26); title.setTextColor(Color.WHITE); box.addView(title);
        TextView hint=new TextView(this); hint.setText("自宅地域は市区町村単位で入力してください。自宅は震度1から、その他は震度3以上を表示します。"); hint.setTextColor(Color.LTGRAY); hint.setPadding(0,16,0,16); box.addView(hint);
        prefecture=field("都道府県（例：東京都）"); city=field("市区町村（例：千代田区）"); box.addView(prefecture); box.addView(city);
        TextView detailLabel=new TextView(this); detailLabel.setText("各地の震度の表示粒度"); detailLabel.setTextColor(Color.LTGRAY); detailLabel.setPadding(0,16,0,4); box.addView(detailLabel);
        detail=new Spinner(this); detail.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"都道府県まで","市区町村まで","観測地点まで"})); detail.setSelection(getSharedPreferences("settings",0).getInt("detail",1)); box.addView(detail);
        Button save=new Button(this); save.setText("保存して監視を開始"); save.setOnClickListener(v->{getSharedPreferences("settings",0).edit().putString("pref",prefecture.getText().toString().trim()).putString("city",city.getText().toString().trim()).putInt("detail",detail.getSelectedItemPosition()).apply(); start();}); box.addView(save);
        Button permission=new Button(this); permission.setText("オーバーレイ権限を設定"); permission.setOnClickListener(v->{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())));}); box.addView(permission);
        Button test=new Button(this); test.setText("表示テスト"); test.setOnClickListener(v->{start(); sendBroadcast(new Intent(OverlayService.TEST_ACTION));}); box.addView(test);
        Button latest=new Button(this); latest.setText("最新APIでテスト"); latest.setOnClickListener(v->{start(); sendBroadcast(new Intent(OverlayService.TEST_LATEST_ACTION));}); box.addView(latest);
        status=new TextView(this); status.setTextColor(Color.LTGRAY); status.setPadding(0,20,0,0); box.addView(status); setContentView(box);
        prefecture.setText(getSharedPreferences("settings",0).getString("pref","")); city.setText(getSharedPreferences("settings",0).getString("city","")); status.setText(Settings.canDrawOverlays(this)?"権限：許可済み":"権限：未許可");
    }
    EditText field(String s){ EditText e=new EditText(this); e.setHint(s); e.setTextColor(Color.WHITE); e.setHintTextColor(Color.GRAY); e.setSingleLine(); return e; }
    void start(){ Intent i=new Intent(this,OverlayService.class); if(Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i); }
}
