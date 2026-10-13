package tw.junba.reminder;

import android.app.*;
import android.content.*;
import android.os.Build;
import org.json.*;
import java.util.*;

public final class ReminderStore {
    private ReminderStore(){}
    static JSONArray all(Context c) {
        try { return new JSONArray(c.getSharedPreferences("junba",0).getString("notes","[]")); }
        catch(Exception ex) { return new JSONArray(); }
    }
    static void save(Context c, JSONArray notes) { c.getSharedPreferences("junba",0).edit().putString("notes",notes.toString()).apply(); }
    static void put(Context c, String title, long timestamp, int advanceMin, String repeat) throws JSONException {
        JSONArray a = all(c); JSONObject o = new JSONObject();
        o.put("id",(int)(System.currentTimeMillis() & 0x7fffffff));o.put("title",title);
        o.put("time",timestamp);o.put("ahead",advanceMin);o.put("repeat",repeat);a.put(o);save(c,a);schedule(c,o);
    }
    static void remove(Context c,int id) {
        JSONArray a=all(c), b=new JSONArray();
        for(int i=0;i<a.length();i++){ JSONObject o=a.optJSONObject(i);if(o!=null){if(o.optInt("id") == id)cancel(c,id);else b.put(o);} }
        save(c,b);
    }
    static PendingIntent intent(Context c,JSONObject o) {
        int id=o.optInt("id");Intent i=new Intent(c,AlarmReceiver.class);
        i.putExtra("id",id);i.putExtra("title",o.optString("title"));
        return PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static void cancel(Context c,int id){Intent i=new Intent(c,AlarmReceiver.class);PendingIntent p=PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);if(p!=null){((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).cancel(p);p.cancel();}}
    static boolean canExact(Context c){AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);return Build.VERSION.SDK_INT<31 || a.canScheduleExactAlarms();}
    static boolean schedule(Context c,JSONObject o){
        AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        long when=o.optLong("time")-o.optInt("ahead")*60000L;
        String repeat=o.optString("repeat","不重複");
        if(when<=System.currentTimeMillis()){
           if("每日".equals(repeat)){while(when<=System.currentTimeMillis())when+=86400000L;}
           else if("每週".equals(repeat)){while(when<=System.currentTimeMillis())when+=604800000L;}
           else return false;
        }
        try {
            if(!canExact(c))return false;
            a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,intent(c,o));
            return true;
        }catch(SecurityException e){return false;}
    }
    static int reschedule(Context c){JSONArray a=all(c);int count=0;for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null && schedule(c,o))count++;}return count;}
}
