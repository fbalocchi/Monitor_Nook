package cl.balocchi.nookpanel;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import java.util.*;
import org.json.JSONObject;

public class SettingsActivity extends BaseScreen {
 private LinearLayout list;
 public void onCreate(Bundle state){super.onCreate(state);ScrollView scroll=new ScrollView(this);list=AppUi.column(this);scroll.addView(list);AppUi.attach(this,scroll,3);}
 protected void onResume(){super.onResume();draw();}
 private void add(String name,final int action){list.addView(AppUi.button(this,name,new View.OnClickListener(){public void onClick(View v){act(action);}}));}
 private void draw(){list.removeAllViews();list.addView(AppUi.text(this,"Ajustes del NOOK",26,true));
  add("Actualizar clima y calendario",0);add("Conexiones y estado",1);add("Wi-Fi",2);add("Google Calendar",3);add("Ubicación del clima",4);add("Frecuencia de actualización",5);
  add("Mantener pantalla activa: "+(getSharedPreferences("panel",0).getBoolean("keep_awake",true)?"sí":"no"),6);
  add("Abrir panel al encender: "+(getSharedPreferences("panel",0).getBoolean("autostart",true)?"sí":"no"),7);
  add("Elegir pantalla de inicio",8);add("Aplicaciones instaladas",9);add("Abrir sistema y lector NOOK",10);add("Último cierre y diagnóstico",11);
  list.addView(AppUi.text(this,"Panel NOOK 1.1.3 · Google Calendar de solo lectura\nClima: Open-Meteo · Hora: America/Santiago",15,false));
 }
 private void act(int action){
  if(action==0){startService(new Intent(this,UpdateService.class).putExtra("force",true));AppUi.message(this,"Actualización","Consultando por Wi-Fi. Puedes volver al panel.");}
  else if(action==1){JSONObject state=Store.json(this,"state.json");long now=System.currentTimeMillis(),weather=state.optLong("weather_ok",0),calendar=state.optLong("calendar_ok",0);java.util.TimeZone zone=CalendarEngine.localZone();
   AppUi.message(this,"Conexiones","Clima: "+state.optString("weather_error","")+"\nÚltimo dato: "+(weather>0?Store.format("dd/MM HH:mm",weather,zone):"pendiente")+"\n\nCalendario: "+state.optString("calendar_error","")+"\nÚltimo dato: "+(calendar>0?Store.format("dd/MM HH:mm",calendar,zone):"pendiente")+"\n\nFecha del NOOK: "+Store.format("dd/MM/yyyy HH:mm",now,zone)+"\nEl registro de diagnóstico no contiene el enlace privado.");
  }else if(action==2){try{startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));}catch(ActivityNotFoundException e){try{startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));}catch(ActivityNotFoundException ex){AppUi.message(this,"Wi-Fi","Abre los ajustes de Wi-Fi desde el sistema NOOK. El acceso directo de este firmware aún debe comprobarse.");}}}
  else if(action==3)calendar();else if(action==4)location();
  else if(action==5){final int[] values={15,30,60,180};new AlertDialog.Builder(this).setTitle("Actualizar cada…").setItems(new String[]{"15 minutos","30 minutos","1 hora","3 horas"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){getSharedPreferences("panel",0).edit().putInt("interval_minutes",values[which]).commit();UpdateService.schedule(SettingsActivity.this);}}).show();}
  else if(action==6||action==7){String key=action==6?"keep_awake":"autostart";boolean value=!getSharedPreferences("panel",0).getBoolean(key,true);getSharedPreferences("panel",0).edit().putBoolean(key,value).commit();if(action==6){if(value)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}draw();}
  else if(action==8){new AlertDialog.Builder(this).setTitle("Inicio propio").setMessage("Si el sistema ofrece elegir una aplicación al pulsar inicio, selecciona Panel NOOK. El botón n de este firmware necesita comprobarse en el lector.\n\nTambién se crea una notificación para volver al panel si el sistema la muestra.").setPositiveButton("Elegir inicio",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){startActivity(Intent.createChooser(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),"Abrir una pantalla de inicio"));}}).setNegativeButton("Cancelar",null).show();}
  else if(action==9)apps();else if(action==10)AppUi.openNook(this);
  else if(action==11){String body="Sin cierre registrado por la app. Si Android la termina por falta de memoria, puede no quedar una excepción aquí.";try{body=new String(Store.file(this,"crash.txt",32768),"UTF-8");}catch(Exception ignored){}AppUi.message(this,"Último cierre",body);}
 }
 private EditText field(LinearLayout form,String label,String value){form.addView(AppUi.text(this,label,17,true));EditText input=new EditText(this);input.setText(value);input.setTextSize(18);input.setSingleLine();form.addView(input);return input;}
 private void calendar(){final JSONObject cfg=Store.json(this,"config.json");LinearLayout form=AppUi.column(this);form.addView(AppUi.text(this,"Dirección secreta iCal de Google Calendar. El enlace queda oculto y se guarda solo en la app. Vacío desactiva la sincronización.",16,false));final EditText url=field(form,"Enlace iCal","");url.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);url.setText(cfg.optString("calendar_url",""));
  final AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Google Calendar").setView(form).setPositiveButton("Guardar",null).setNegativeButton("Cancelar",null).create();dialog.show();dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){public void onClick(View v){try{String value=url.getText().toString().trim();JSONObject candidate=new JSONObject(cfg.toString());candidate.put("calendar_url",value);if(value.length()>0)Store.calendarUrl(candidate);Store.saveJson(SettingsActivity.this,"config.json",candidate);if(!value.equals(cfg.optString("calendar_url",""))){deleteFile("calendar.ics");JSONObject meta=Store.json(SettingsActivity.this,"state.json");meta.put("calendar_ok",0);meta.put("calendar_error",value.length()==0?"Sincronización desactivada":"Esperando descarga del calendario");Store.saveJson(SettingsActivity.this,"state.json",meta);sendBroadcast(new Intent(UpdateService.CHANGED));}dialog.dismiss();startService(new Intent(SettingsActivity.this,UpdateService.class).putExtra("force",true));}catch(Exception e){url.setError("Usa el enlace HTTPS iCal de calendar.google.com terminado en /basic.ics");}}});
 }
 private void location(){final JSONObject cfg=Store.json(this,"config.json");LinearLayout form=AppUi.column(this);final EditText name=field(form,"Nombre",cfg.optString("location","Madrid (ejemplo)")),lat=field(form,"Latitud",String.valueOf(cfg.optDouble("latitude",40.42))),lon=field(form,"Longitud",String.valueOf(cfg.optDouble("longitude",-3.70)));
  final AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Ubicación del clima").setView(form).setPositiveButton("Guardar",null).setNegativeButton("Cancelar",null).create();dialog.show();dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){public void onClick(View v){try{JSONObject copy=new JSONObject(cfg.toString());double latitude=Double.parseDouble(lat.getText().toString().trim().replace(',','.')),longitude=Double.parseDouble(lon.getText().toString().trim().replace(',','.'));copy.put("latitude",latitude);copy.put("longitude",longitude);copy.put("location",name.getText().toString().trim());Store.weatherUrl(copy);Store.saveJson(SettingsActivity.this,"config.json",copy);dialog.dismiss();startService(new Intent(SettingsActivity.this,UpdateService.class).putExtra("force",true));}catch(Exception e){lat.setError("Revisa las coordenadas: latitud -90 a 90; longitud -180 a 180.");}}});
 }
 private void apps(){final List<ResolveInfo> items=getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);Collections.sort(items,new ResolveInfo.DisplayNameComparator(getPackageManager()));final List<ResolveInfo> shown=new ArrayList<ResolveInfo>();List<String> names=new ArrayList<String>();for(ResolveInfo item:items)if(!item.activityInfo.packageName.equals(getPackageName())){shown.add(item);names.add(item.loadLabel(getPackageManager()).toString());}
  if(shown.isEmpty()){AppUi.message(this,"Aplicaciones","No hay otras aplicaciones con acceso de inicio. El lector original está disponible en Abrir sistema y lector NOOK.");return;}
  new AlertDialog.Builder(this).setTitle("Aplicaciones").setItems(names.toArray(new String[names.size()]),new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){ResolveInfo info=shown.get(which);try{startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(new ComponentName(info.activityInfo.packageName,info.activityInfo.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception e){AppUi.message(SettingsActivity.this,"Aplicación","No se pudo abrir esta aplicación.");}}}).show();
 }
}
