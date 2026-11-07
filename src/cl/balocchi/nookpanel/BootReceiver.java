package cl.balocchi.nookpanel;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){
  UpdateService.schedule(c);
  if(Store.configured(c)){
   c.startService(new Intent(c,UpdateService.class));
   if(c.getSharedPreferences("panel",0).getBoolean("autostart",Store.json(c,"config.json").optBoolean("autostart",true))){
    android.app.PendingIntent launch=android.app.PendingIntent.getActivity(c,10,new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP),android.app.PendingIntent.FLAG_UPDATE_CURRENT);
    ((android.app.AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).set(android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,android.os.SystemClock.elapsedRealtime()+30000L,launch);
   }
  }
 }
}
