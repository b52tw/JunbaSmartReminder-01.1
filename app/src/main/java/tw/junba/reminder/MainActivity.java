package tw.junba.reminder;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.content.ContentUris;
import android.os.*;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.net.*;
import java.io.*;

public class MainActivity extends Activity {
    LinearLayout root, content; TextView status; Handler main = new Handler(Looper.getMainLooper());
    final SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.TAIWAN);
    int ahead = 10; String repeat="不重複";
    int calendarCount=0, eventCount=0, alarmCount=0;
    @Override public void onCreate(Bundle b){super.onCreate(b);AlarmReceiver.ensureChannel(this);build();askPermissions();sync();}
    @Override protected void onResume(){super.onResume();if(status!=null)sync();}
    int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    TextView text(String s,int sp){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(Color.rgb(29,38,50));t.setPadding(dp(5),dp(7),dp(5),dp(7));return t;}
    void add(LinearLayout l,String s){l.addView(text(s,15));}
    Button button(LinearLayout l,String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);l.addView(b);b.setOnClickListener(v->action.run());return b;}
    void build(){
        ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setPadding(dp(14),dp(12),dp(14),dp(25));root.setOrientation(1);scroll.addView(root);setContentView(scroll);
        TextView name=text("Junba 智慧提醒  v0.1",23);root.addView(name);
        status=text("正在檢查已授權來源…",13);root.addView(status);
        button(root,"立即重新同步",this::sync);
        LinearLayout menu=new LinearLayout(this);menu.setOrientation(LinearLayout.HORIZONTAL);root.addView(menu);
        for(String label:new String[]{"總覽","記事","日曆","設定"}){Button b=new Button(this);b.setText(label);b.setTextSize(12);b.setAllCaps(false);menu.addView(b,new LinearLayout.LayoutParams(0,dp(50),1));b.setOnClickListener(v->showPage(label));}
        content=new LinearLayout(this);content.setOrientation(1);root.addView(content);showPage("總覽");
    }
    void askPermissions(){ArrayList<String> req=new ArrayList<>();if(checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED)req.add(Manifest.permission.READ_CALENDAR);if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)req.add(Manifest.permission.POST_NOTIFICATIONS);if(!req.isEmpty())requestPermissions(req.toArray(new String[0]),7);}
    @Override public void onRequestPermissionsResult(int request,String[] p,int[] r){super.onRequestPermissionsResult(request,p,r);sync();}
    void showPage(String page){content.removeAllViews();switch(page){case "記事":notesPage();break;case "日曆":calendarPage();break;case "設定":settingsPage();break;default:homePage();}}
    void sync(){
        int records=ReminderStore.reschedule(this);calendarCount=0;eventCount=0;
        if(checkSelfPermission(Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED){
            try {Cursor c=getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,new String[]{CalendarContract.Calendars._ID},null,null,null);if(c!=null){calendarCount=c.getCount();c.close();}}catch(Exception ignored){}
        }
        AlarmManager.AlarmClockInfo next=((AlarmManager)getSystemService(ALARM_SERVICE)).getNextAlarmClock();
        String nextStr=next==null?"系統未提供下一次鬧鐘":"下一次系統鬧鐘："+fmt.format(new Date(next.getTriggerTime()));
        status.setText("檢查時間："+fmt.format(new Date())+"\n手機日曆："+calendarCount+" 個｜已重新排程記事："+records+" 筆\n"+nextStr+"\n注意：無法讀取 vivo 全部原廠鬧鐘、稍後提醒次數。");
    }
    void homePage(){
        add(content,"【整合來源】手機記事："+ReminderStore.all(this).length()+" 筆；手機日曆："+calendarCount+" 個。");
        button(content,"開啟 vivo 原廠時鐘核對",()->{try{startActivity(new Intent(AlarmClock.ACTION_SHOW_ALARMS));}catch(Exception e){toast("原廠時鐘無法由此開啟");}});
        button(content,"建立原廠鬧鐘（須核對）",()->showCreateNative());
        button(content,"測試 1 分鐘後 App 提醒",()->{
            try{ReminderStore.put(this,"Junba 測試提醒",System.currentTimeMillis()+60000,0,"不重複");
            toast(ReminderStore.canExact(this)?"測試已排程；請等待實際響鈴驗證":"已存入記事，但精確鬧鐘權限尚未開啟");sync();}catch(Exception e){toast("建立測試失敗");}
        });
        button(content,"顯示系統精確鬧鐘權限",()->{if(Build.VERSION.SDK_INT>=31)try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}catch(Exception e){toast("無法開啟權限設定");}else toast("Android 版本不需此設定");});
        add(content,"【驗證原則】App 排程成功 ≠ 已響鈴；原廠鬧鐘建立要求 ≠ 已確認原廠設定。\n僅列出取得授權的日曆資訊，原廠所有鬧鐘不可完整匯入。");
    }
    void showCreateNative(){EditText title=new EditText(this);title.setHint("鬧鐘名稱");new AlertDialog.Builder(this).setTitle("建立原廠鬧鐘").setView(title).setPositiveButton("下一步",(d,w)->{
        Calendar cal=Calendar.getInstance();new TimePickerDialog(this,(v,h,m)->{
           Intent i=new Intent(AlarmClock.ACTION_SET_ALARM);i.putExtra(AlarmClock.EXTRA_HOUR,h);i.putExtra(AlarmClock.EXTRA_MINUTES,m);i.putExtra(AlarmClock.EXTRA_MESSAGE,title.getText().toString());i.putExtra(AlarmClock.EXTRA_SKIP_UI,false);
           try{startActivity(i);toast("已送出請求，請在 vivo 時鐘確認時間、重複及啟用狀態");}catch(Exception e){toast("手機不支援公開鬧鐘建立介面");}
        },cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE),true).show();
    }).setNegativeButton("取消",null).show();}
    void notesPage(){button(content,"新增記事與提醒",this::addNoteDialog);JSONArray a=ReminderStore.all(this);
        for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;int id=o.optInt("id");String repeat=o.optString("repeat","不重複");
            add(content,"● "+o.optString("title")+"\n"+fmt.format(new Date(o.optLong("time")))+"｜提前 "+o.optInt("ahead")+" 分鐘｜"+repeat);
            button(content,"刪除此記事 / 取消 App 排程",()->new AlertDialog.Builder(this).setTitle("確認刪除？").setPositiveButton("刪除",(d,w)->{ReminderStore.remove(this,id);sync();showPage("記事");}).setNegativeButton("取消",null).show());
        }
    }
    void addNoteDialog(){LinearLayout form=new LinearLayout(this);form.setPadding(dp(16),0,dp(16),0);form.setOrientation(1);EditText input=new EditText(this);input.setHint("記事內容");form.addView(input);
        Calendar cal=Calendar.getInstance();cal.add(Calendar.DAY_OF_MONTH,1);TextView when=text(fmt.format(cal.getTime()),16);form.addView(when);
        button(form,"選日期",()->new DatePickerDialog(this,(v,y,m,d)->{cal.set(y,m,d);when.setText(fmt.format(cal.getTime()));},cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)).show());
        button(form,"選時間",()->new TimePickerDialog(this,(v,h,m)->{cal.set(Calendar.HOUR_OF_DAY,h);cal.set(Calendar.MINUTE,m);cal.set(Calendar.SECOND,0);when.setText(fmt.format(cal.getTime()));},cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE),true).show());
        Spinner adv=new Spinner(this);String[] minutes={"準時","5 分鐘前","10 分鐘前","30 分鐘前","60 分鐘前"};ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,minutes);adv.setAdapter(ad);adv.setSelection(2);form.addView(adv);
        Spinner rep=new Spinner(this);String[] opts={"不重複","每日","每週"};rep.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,opts));form.addView(rep);
        new AlertDialog.Builder(this).setTitle("新增自主管理記事提醒").setView(form).setPositiveButton("儲存",(d,w)->{
            String s=input.getText().toString().trim();if(s.isEmpty()){toast("記事內容不得空白");return;}
            try{ReminderStore.put(this,s,cal.getTimeInMillis(),new int[]{0,5,10,30,60}[adv.getSelectedItemPosition()],opts[rep.getSelectedItemPosition()]);sync();showPage("記事");if(!ReminderStore.canExact(this))toast("已儲存，但尚未取得精確鬧鐘權限，提醒未排程");else toast("已儲存並要求排程，請進行響鈴測試");}catch(Exception e){toast("儲存失敗");}
        }).setNegativeButton("取消",null).show();
    }
    void calendarPage(){if(checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED){add(content,"尚未授權讀取手機日曆，請授權後重新同步。");button(content,"要求日曆權限",this::askPermissions);return;}
        add(content,"勾選要讀取的日曆，包含完整帳號名稱。已同步至手機的 Google 日曆活動才會顯示。");
        try(Cursor c=getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,new String[]{CalendarContract.Calendars._ID,CalendarContract.Calendars.ACCOUNT_NAME,CalendarContract.Calendars.CALENDAR_DISPLAY_NAME},null,null,CalendarContract.Calendars.ACCOUNT_NAME+" ASC")){
            if(c==null){add(content,"無法查詢日曆");return;}
            while(c.moveToNext()){long id=c.getLong(0);String email=c.getString(1),name=c.getString(2);String key="cal_"+id;boolean selected=getPreferences(0).getBoolean(key,true);CheckBox box=new CheckBox(this);box.setText((email==null?"（未提供帳號）":email)+"\n"+name);box.setChecked(selected);content.addView(box);box.setOnCheckedChangeListener((b,on)->{getPreferences(0).edit().putBoolean(key,on).apply();showPage("日曆");});if(selected)eventsFor(id);}
        }catch(Exception ex){add(content,"日曆讀取失敗：請檢查權限或同步設定");}
    }
    void eventsFor(long id){long start=System.currentTimeMillis(),end=start+60L*86400000;Uri.Builder builder=CalendarContract.Instances.CONTENT_URI.buildUpon();ContentUris.appendId(builder,start);ContentUris.appendId(builder,end);
        String[] cols={CalendarContract.Instances.EVENT_ID,CalendarContract.Instances.TITLE,CalendarContract.Instances.BEGIN,CalendarContract.Instances.RRULE};
        try(Cursor c=getContentResolver().query(builder.build(),cols,CalendarContract.Instances.CALENDAR_ID+"=?",new String[]{String.valueOf(id)},CalendarContract.Instances.BEGIN+" ASC")){
            if(c==null)return;int count=0;while(c.moveToNext() && count<35){long eid=c.getLong(0);String title=c.getString(1);long begin=c.getLong(2);String rrule=c.getString(3);add(content,"• "+title+"\n  "+fmt.format(new Date(begin))+" ｜提醒 "+getReminderInfo(eid)+" ｜重複 "+(rrule==null?"無":rrule));count++;}
            if(count>=35)add(content,"僅顯示此日曆未來 60 天前 35 筆活動。");
        }catch(Exception e){add(content,"此日曆活動讀取失敗");}
    }
    String getReminderInfo(long id){ArrayList<String> a=new ArrayList<>();try(Cursor c=getContentResolver().query(CalendarContract.Reminders.CONTENT_URI,new String[]{CalendarContract.Reminders.MINUTES},CalendarContract.Reminders.EVENT_ID+"=?",new String[]{String.valueOf(id)},null)){
        if(c!=null)while(c.moveToNext())a.add(c.getInt(0)+" 分鐘前");}catch(Exception ignored){}return a.isEmpty()?"未提供":android.text.TextUtils.join("、",a);}
    void settingsPage(){add(content,"資料只在手機本機顯示。日曆採唯讀；不會自動改動原廠鬧鐘。");
        button(content,"顯示／收折 Gemini API 設定",()->geminiDialog());
        button(content,"開啟 App 通知設定",()->{Intent i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);i.putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(i);});
        button(content,"檢查系統鬧鐘權限",()->toast(ReminderStore.canExact(this)?"可要求精確鬧鐘排程":"權限尚未核准，記事只能先儲存"));
    }
    void geminiDialog(){LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(15),0,dp(15),0);
        EditText key=new EditText(this);key.setHint("Gemini API Key（不寫入專案）");key.setSingleLine(true);box.addView(key);
        add(box,"僅測試連線，不會因此取得語音助理歷史或 vivo 原廠鬧鐘的權限。此版不永久儲存金鑰。");
        button(box,"測試連線",()->{String k=key.getText().toString().trim();if(k.isEmpty()){toast("先填 API Key");return;}toast("正在測試 Gemini 連線");new Thread(()->{
           String result="";try{URL url=new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent");HttpURLConnection conn=(HttpURLConnection)url.openConnection();conn.setRequestMethod("POST");conn.setRequestProperty("x-goog-api-key",k);conn.setRequestProperty("Content-Type","application/json");conn.setConnectTimeout(12000);conn.setReadTimeout(15000);conn.setDoOutput(true);byte[] data="{\"contents\":[{\"parts\":[{\"text\":\"Reply OK\"}]}]}".getBytes("UTF-8");try(OutputStream out=conn.getOutputStream()){out.write(data);}int code=conn.getResponseCode();result=code==200?"連線成功（HTTP 200）":"連線失敗：HTTP "+code;conn.disconnect();}catch(Exception e){result="連線失敗："+e.getClass().getSimpleName();}String r=result;runOnUiThread(()->new AlertDialog.Builder(this).setMessage(r).setPositiveButton("確定",null).show());
        }).start();});new AlertDialog.Builder(this).setTitle("Gemini API｜本次使用").setView(box).setPositiveButton("關閉",(d,w)->key.setText("")).show();
    }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
