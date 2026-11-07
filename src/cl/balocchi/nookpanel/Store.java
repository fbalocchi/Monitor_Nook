package cl.balocchi.nookpanel;

import android.content.Context;
import java.io.*;
import java.util.*;
import org.json.*;

final class Store {
 static final int ICS_LIMIT=4*1024*1024;
 static byte[] read(InputStream in,int limit)throws IOException{
  try{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(n>limit-out.size())throw new IOException("Archivo demasiado grande");out.write(b,0,n);}return out.toByteArray();}finally{in.close();}
 }
 static byte[] file(Context c,String name,int limit)throws IOException{return read(c.openFileInput(name),limit);}
 static JSONObject json(Context c,String name){try{return new JSONObject(new String(file(c,name,128*1024),"UTF-8"));}catch(Exception e){return new JSONObject();}}
 static synchronized void save(Context c,String name,byte[] b)throws IOException{
  File tmp=new File(c.getFilesDir(),name+".new"),target=new File(c.getFilesDir(),name);
  FileOutputStream f=new FileOutputStream(tmp);try{f.write(b);f.getFD().sync();}finally{f.close();}
  if(!tmp.renameTo(target))throw new IOException("No se pudo guardar");
 }
 static void saveJson(Context c,String name,JSONObject o)throws Exception{save(c,name,o.toString().getBytes("UTF-8"));}
 static boolean configured(Context c){return new File(c.getFilesDir(),"config.json").exists();}
 static String weatherUrl(JSONObject cfg)throws Exception{
  double lat=cfg.optDouble("latitude",40.42),lon=cfg.optDouble("longitude",-3.70);
  if(lat< -90||lat>90||lon< -180||lon>180||Double.isNaN(lat)||Double.isNaN(lon))throw new IOException("Ubicacion invalida");
  return "https://api.open-meteo.com/v1/forecast?latitude="+lat+"&longitude="+lon+
   "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m"+
   "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&forecast_days=3&timezone=America%2FSantiago";
 }
 static String calendarUrl(JSONObject cfg)throws Exception{
  String s=cfg.optString("calendar_url","");java.net.URL u=new java.net.URL(s);
  if(!u.getProtocol().equals("https")||!u.getHost().equals("calendar.google.com")||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443)||!u.getPath().startsWith("/calendar/ical/")||!u.getPath().endsWith("/basic.ics"))throw new IOException("Enlace de calendario invalido");
  return s;
 }
 static String condition(int n){
  if(n<0)return "Sin dato";if(n==0)return "Despejado";if(n<=2)return "Parcialmente nublado";if(n==3)return "Nublado";
  if(n==45||n==48)return "Niebla";if(n>=51&&n<=57)return "Llovizna";if(n>=61&&n<=67)return "Lluvia";
  if(n>=71&&n<=77)return "Nieve";if(n>=80&&n<=82)return "Chubascos";if(n>=85&&n<=86)return "Chubascos de nieve";if(n>=95)return "Tormenta";return "Sin dato";
 }
 static java.util.Calendar cal(java.util.TimeZone tz,long time){java.util.Calendar c=java.util.Calendar.getInstance(tz,new Locale("es","CL"));c.setTimeInMillis(time);return c;}
 static String format(String pattern,long time,java.util.TimeZone tz){java.text.SimpleDateFormat f=new java.text.SimpleDateFormat(pattern,new Locale("es","CL"));f.setTimeZone(tz);return f.format(new java.util.Date(time));}
}
