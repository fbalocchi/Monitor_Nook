package cl.balocchi.nookpanel;

import android.app.*;
import android.content.*;
import android.os.*;
import org.json.*;

public class UpdateService extends IntentService {
 public static final String CHANGED="cl.balocchi.nookpanel.CHANGED";
 public UpdateService(){super("NookPanelUpdate");}
 static void schedule(Context c){
  PendingIntent p=PendingIntent.getService(c,0,new Intent(c,UpdateService.class),PendingIntent.FLAG_UPDATE_CURRENT);
  int minutes=c.getSharedPreferences("panel",0).getInt("interval_minutes",30);if(minutes!=15&&minutes!=30&&minutes!=60&&minutes!=180)minutes=30;
  ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).setInexactRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+minutes*60000L,minutes*60000L,p);
 }
 protected void onHandleIntent(Intent intent){
  PowerManager.WakeLock lock=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"NookPanel:Update");
  // API7 does not cancel acquire(timeout)'s callback after manual release.
  // A single-operation, non-reference-counted lock tolerates that stale release.
  lock.setReferenceCounted(false);
  lock.acquire(180000L);
  try{
   if(!Store.configured(this))return;
   JSONObject cfg=Store.json(this,"config.json"),meta=Store.json(this,"state.json");
   long lastAttempt=meta.optLong("attempt",0);
   if(!intent.getBooleanExtra("force",false)&&System.currentTimeMillis()-lastAttempt<5*60000L)return;
   meta.put("attempt",System.currentTimeMillis());Store.saveJson(this,"state.json",meta);
   SecureHttp http=new SecureHttp(Store.read(getAssets().open("ca_bundle.pem"),512*1024));
   try{
    http.certificateTrace="";
    byte[] data=http.get(Store.weatherUrl(cfg),128*1024);
    JSONObject w=new JSONObject(new String(data,"UTF-8"));
    w.getJSONObject("current").getDouble("temperature_2m");w.getJSONObject("daily").getJSONArray("time");
    Store.save(this,"weather.json",data);meta.put("weather_ok",System.currentTimeMillis());meta.put("weather_error","");
   }catch(Exception e){meta.put("weather_error",safe(e));Diagnostics.network(this,"weather",e);Diagnostics.publicTls(this,"weather",http.certificateTrace);}
   if(cfg.optString("calendar_url","").length()>0)try{
    http.certificateTrace="";
    byte[] data=http.get(Store.calendarUrl(cfg),Store.ICS_LIMIT);
    CalendarEngine test=new CalendarEngine(data,"America/Santiago");
    Store.save(this,"calendar.ics",data);meta.put("calendar_ok",System.currentTimeMillis());meta.put("calendar_error","");
   }catch(Exception e){meta.put("calendar_error",safe(e));Diagnostics.network(this,"calendar",e);Diagnostics.publicTls(this,"calendar",http.certificateTrace);}
   else meta.put("calendar_error","Falta configurar Google Calendar");
   Store.saveJson(this,"state.json",meta);
  }catch(Exception e){android.util.Log.e("NookPanel","Error de actualizacion: "+e.getClass().getSimpleName());}
  finally{if(lock.isHeld())lock.release();sendBroadcast(new Intent(CHANGED));}
 }
 private String safe(Exception e){
  String s=e.getMessage();
  if(s!=null&&s.matches("HTTP [0-9]{3}"))return s;
  if(e instanceof java.net.UnknownHostException)return "Sin conexion Wi-Fi o DNS";
  if(e instanceof org.spongycastle.tls.TlsFatalAlert)return "Error TLS (alerta "+((org.spongycastle.tls.TlsFatalAlert)e).getAlertDescription()+"): revisar diagnostico";
  if(e instanceof java.net.SocketTimeoutException)return "Tiempo de conexion agotado";
  if(e instanceof java.io.IOException&&s!=null&&s.equals("Datos demasiado grandes"))return "Calendario mayor a 4 MiB";
  return "No se pudo actualizar ("+e.getClass().getSimpleName()+")";
 }
}
