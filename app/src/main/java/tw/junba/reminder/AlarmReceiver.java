package tw.junba.reminder;

import android.app.*;
import android.content.*;
import android.os.Build;

public class AlarmReceiver extends android.content.BroadcastReceiver {
    static final String CHANNEL = "junba_reminders";
    static void ensureChannel(Context c) {
        NotificationManager n = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL,"Junba 提醒響鈴", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Junba 自行管理的記事提醒及測試");
            ch.enableVibration(true);
            n.createNotificationChannel(ch);
        }
    }
    @Override public void onReceive(Context c, Intent intent) {
        ensureChannel(c);
        String name = intent.getStringExtra("title");
        if (name == null) name = "Junba 提醒";
        int id = intent.getIntExtra("id", 7000);
        Intent open = new Intent(c,MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(c,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
          .setContentTitle("提醒時間到了").setContentText(name).setAutoCancel(true).setContentIntent(pi)
          .setCategory(Notification.CATEGORY_ALARM).build();
        try { ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id,n); }
        catch (SecurityException ignored) { /* permission denied */ }
        org.json.JSONArray notes=ReminderStore.all(c);
        for(int j=0;j<notes.length();j++){org.json.JSONObject o=notes.optJSONObject(j);if(o!=null&&o.optInt("id")==id){String r=o.optString("repeat");if("每日".equals(r)||"每週".equals(r))ReminderStore.schedule(c,o);break;}}
    }
}
