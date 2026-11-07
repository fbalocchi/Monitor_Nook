package cl.balocchi.nookpanel;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import java.util.*;
import org.json.*;

public class MainActivity extends BaseScreen {
 private Panel panel;private Handler handler=new Handler();private volatile int generation=0;private java.util.concurrent.ExecutorService loader=java.util.concurrent.Executors.newSingleThreadExecutor();
 private String todayMark="";private int todayYear,todayMonth;
 private int year,month,selected=0;private Model model;private boolean active=false;
 private static final Locale ES=new Locale("es","CL");
 private static final String[] MONTHS={"Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};
 private static final class Model {
  JSONObject cfg,weather,meta;TimeZone zone;List<CalendarEngine.Event> events=new ArrayList<CalendarEngine.Event>();
  String calendarProblem="";long from,to;int year,month,monthDays,offset;long[] dayStarts;
 }
 private BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){
  if(UpdateService.CHANGED.equals(i.getAction()))load();
  else{handler.removeCallbacks(minute);handler.post(minute);panel.invalidate();}
 }};
 private Runnable minute=new Runnable(){public void run(){if(active){
  if(model!=null){
   java.util.Calendar today=Store.cal(model.zone,System.currentTimeMillis());
   int ty=today.get(java.util.Calendar.YEAR),tm=today.get(java.util.Calendar.MONTH);
   String key=ty+"-"+tm+"-"+today.get(java.util.Calendar.DAY_OF_MONTH);
   if(!key.equals(todayMark)){
    if(todayMark.length()>0&&year==todayYear&&month==todayMonth){year=ty;month=tm;selected=0;}
    todayMark=key;todayYear=ty;todayMonth=tm;load();
   }else panel.invalidate();
  }
  handler.postDelayed(this,60000L-System.currentTimeMillis()%60000L);
 }}};
 public void onCreate(Bundle state){
  super.onCreate(state);getFilesDir();
  java.util.Calendar now=Store.cal(CalendarEngine.localZone(),System.currentTimeMillis());year=now.get(java.util.Calendar.YEAR);month=now.get(java.util.Calendar.MONTH);
  if(state!=null){year=state.getInt("year",year);month=state.getInt("month",month);selected=state.getInt("selected",0);}
  panel=new Panel();AppUi.attach(this,panel,0);UpdateService.schedule(this);
 }
 protected void onSaveInstanceState(Bundle s){super.onSaveInstanceState(s);s.putInt("year",year);s.putInt("month",month);s.putInt("selected",selected);}
 protected void onResume(){super.onResume();active=true;IntentFilter changes=new IntentFilter(UpdateService.CHANGED);changes.addAction(Intent.ACTION_TIME_CHANGED);changes.addAction(Intent.ACTION_TIMEZONE_CHANGED);registerReceiver(receiver,changes);load();handler.post(minute);if(Store.configured(this))startService(new Intent(this,UpdateService.class));}
 protected void onPause(){active=false;handler.removeCallbacks(minute);unregisterReceiver(receiver);super.onPause();}
 protected void onDestroy(){generation++;loader.shutdownNow();super.onDestroy();}
 private void refresh(){startService(new Intent(this,UpdateService.class).putExtra("force",true));panel.note="Actualizando por Wi-Fi...";panel.invalidate();}
 private void load(){
  if(loader.isShutdown())return;
  final int run=++generation,y=year,m=month;
  loader.execute(new Runnable(){public void run(){
   if(run!=generation)return;
   final Model result=new Model();result.year=y;result.month=m;
   result.cfg=Store.json(MainActivity.this,"config.json");result.weather=Store.json(MainActivity.this,"weather.json");result.meta=Store.json(MainActivity.this,"state.json");
   result.zone=CalendarEngine.localZone();
   try{
    CalendarEngine engine=new CalendarEngine(Store.file(MainActivity.this,"calendar.ics",Store.ICS_LIMIT),"America/Santiago");result.zone=engine.zone;
    java.util.Calendar c=Store.cal(result.zone,System.currentTimeMillis());c.set(y,m,1,0,0,0);c.set(java.util.Calendar.MILLISECOND,0);c.add(java.util.Calendar.DATE,-7);result.from=c.getTimeInMillis();c.add(java.util.Calendar.MONTH,2);c.add(java.util.Calendar.DATE,14);result.to=c.getTimeInMillis();
    result.events=engine.between(result.from,result.to);
   }catch(java.io.FileNotFoundException e){result.calendarProblem=result.cfg.optString("calendar_url","").length()>0?"Esperando eventos de Google Calendar…":"Configura Google Calendar en Ajustes";}
   catch(Exception e){result.calendarProblem="No se pudo leer el calendario ("+e.getClass().getSimpleName()+")";Diagnostics.network(MainActivity.this,"panel-calendar",e);}
   java.util.Calendar dates=java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"));dates.clear();dates.set(y,m,1);result.monthDays=dates.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);result.offset=(dates.get(java.util.Calendar.DAY_OF_WEEK)+5)%7;
   result.dayStarts=new long[result.monthDays+1];for(int day=0;day<=result.monthDays;day++)result.dayStarts[day]=AgendaWindow.startOfDate(result.zone,y,m,day+1);
   handler.post(new Runnable(){public void run(){if(run==generation){model=result;panel.note="";panel.invalidate();}}});
  }});
 }
 private void monthMove(int direction){month+=direction;if(month<0){month=11;year--;}if(month>11){month=0;year++;}selected=0;load();}
 void openAgenda(){startActivity(new Intent(this,AgendaActivity.class).putExtra("year",year).putExtra("month",month).putExtra("day",selected));}
 public void onBackPressed(){menu();}
 private void menu(){new AlertDialog.Builder(this).setTitle("Panel NOOK").setItems(new String[]{"Actualizar ahora","Volver al mes actual","Agenda completa","Ajustes y conexiones","Abrir sistema NOOK"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){
  if(which==0)refresh();else if(which==1){TimeZone tz=model==null?CalendarEngine.localZone():model.zone;java.util.Calendar c=Store.cal(tz,System.currentTimeMillis());year=c.get(java.util.Calendar.YEAR);month=c.get(java.util.Calendar.MONTH);selected=0;load();}
  else if(which==2)openAgenda();else if(which==3)AppUi.go(MainActivity.this,3);else AppUi.openNook(MainActivity.this);
 }}).show();}
 public boolean onCreateOptionsMenu(Menu menu){menu.add("Opciones");return true;}
 public boolean onOptionsItemSelected(MenuItem item){menu();return true;}
 private final class Panel extends View {
  Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);String note="";float scale=1,ox=0,oy=0;long down;float dx,dy;
  List<CalendarEngine.Event> shown=new ArrayList<CalendarEngine.Event>();int gridOffset=0,days=31;
  Panel(){super(MainActivity.this);setBackgroundColor(Color.WHITE);setFocusable(true);}
  void text(Canvas c,String s,float x,float y,int size,boolean bold){p.setColor(Color.BLACK);p.setTextSize(size);p.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);c.drawText(s,x,y,p);}
  void fit(Canvas c,String s,float x,float y,int size,boolean bold,float width){p.setTextSize(size);p.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);while(s.length()>1&&p.measureText(s)>width)s=s.substring(0,s.length()-2)+"…";text(c,s,x,y,size,bold);}
  void line(Canvas c,float y){p.setColor(Color.BLACK);p.setStrokeWidth(1);c.drawLine(30,y,728,y,p);}
  String rounded(double n){return String.valueOf(Math.round(n));}
  protected void onDraw(Canvas original){
   super.onDraw(original);Canvas c=original;scale=Math.min(getWidth()/758f,getHeight()/1024f);ox=(getWidth()-758*scale)/2;oy=(getHeight()-1024*scale)/2;c.save();c.translate(ox,oy);c.scale(scale,scale);c.drawColor(Color.WHITE);
   if(model==null||model.year!=year||model.month!=month){text(c,"Panel NOOK",32,64,38,true);text(c,"Leyendo datos guardados…",32,120,25,false);c.restore();return;}
   long now=System.currentTimeMillis();TimeZone tz=model.zone;
   fit(c,Store.format("EEEE d 'de' MMMM",now,tz),32,45,29,true,515);
   String clock=Store.format("HH:mm",now,CalendarEngine.localZone());p.setTextSize(48);p.setTypeface(Typeface.DEFAULT_BOLD);text(c,clock,728-p.measureText(clock),50,48,true);line(c,64);
   fit(c,model.cfg.optString("location","Madrid (ejemplo)"),32,100,24,true,495);text(c,"ACTUALIZAR",566,100,20,true);
   JSONObject current=model.weather.optJSONObject("current"),daily=model.weather.optJSONObject("daily");
   if(current!=null){
    text(c,rounded(current.optDouble("temperature_2m",0))+"°",28,184,84,true);
    fit(c,Store.condition(current.optInt("weather_code",-1)),32,220,23,false,322);
    text(c,"Sensación "+rounded(current.optDouble("apparent_temperature",0))+"° · viento "+rounded(current.optDouble("wind_speed_10m",0))+" km/h",32,247,18,false);
   }else{text(c,"Esperando clima",32,165,30,true);}
   if(daily!=null)try{JSONArray dates=daily.getJSONArray("time"),max=daily.getJSONArray("temperature_2m_max"),min=daily.getJSONArray("temperature_2m_min"),rain=daily.getJSONArray("precipitation_probability_max");
    for(int i=0;i<Math.min(3,dates.length());i++){
     float x=380+i*117;String date=dates.getString(i);String title=i==0?"HOY":i==1?"MAÑANA":date.substring(8)+"/"+date.substring(5,7);
     text(c,title,x,135,17,true);text(c,rounded(max.getDouble(i))+"°/"+rounded(min.getDouble(i))+"°",x,174,24,true);text(c,"Lluvia",x,204,17,false);text(c,rain.isNull(i)?"—":rain.getInt(i)+"%",x,230,23,true);
    }
   }catch(Exception ignored){}
   line(c,267);text(c,"‹",35,309,42,true);text(c,"›",696,309,42,true);
   String heading=MONTHS[month]+" "+year;p.setTextSize(30);p.setTypeface(Typeface.DEFAULT_BOLD);text(c,heading,(758-p.measureText(heading))/2,306,30,true);
   String[] weekdays={"L","M","X","J","V","S","D"};
   for(int col=0;col<7;col++)text(c,weekdays[col],66+col*99,342,19,true);
   gridOffset=model.offset;days=model.monthDays;
   for(int day=1;day<=days;day++){
    int index=gridOffset+day-1,col=index%7,row=index/7;float x=30+col*99,y=353+row*45;
    long start=model.dayStarts[day-1],end=model.dayStarts[day];
    boolean today=now>=start&&now<end,has=false;for(CalendarEngine.Event e:model.events)if(e.start<end&&e.end>start){has=true;break;}
    if(selected==day){p.setColor(Color.BLACK);c.drawRect(x+8,y+1,x+86,y+42,p);p.setColor(Color.WHITE);p.setTextSize(25);p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText(String.valueOf(day),x+25,y+30,p);if(has)c.drawCircle(x+72,y+13,3,p);}
    else{
     if(today){p.setColor(Color.BLACK);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRect(x+8,y+1,x+86,y+42,p);p.setStyle(Paint.Style.FILL);}
     text(c,String.valueOf(day),x+25,y+30,25,today);if(has){p.setColor(Color.BLACK);c.drawCircle(x+72,y+13,3,p);}
    }
   }
   line(c,632);shown.clear();
   if(model.calendarProblem.length()>0){fit(c,model.calendarProblem,32,675,22,true,690);}
   else{
    long from=now,to=Long.MAX_VALUE;
    if(selected>0){from=model.dayStarts[selected-1];to=model.dayStarts[selected];}
    else if(year!=Store.cal(tz,now).get(java.util.Calendar.YEAR)||month!=Store.cal(tz,now).get(java.util.Calendar.MONTH)){from=model.dayStarts[0];to=model.dayStarts[days];}
    text(c,selected==0?"Próximos eventos":selected+" de "+MONTHS[month].toLowerCase(ES),32,668,26,true);
    for(CalendarEngine.Event e:model.events)if(e.end>from&&e.start<to)shown.add(e);
    if(shown.isEmpty())text(c,"Sin eventos en este período",32,717,24,false);
    for(int i=0;i<Math.min(4,shown.size());i++){
     CalendarEngine.Event e=shown.get(i);float y=710+i*57;
     String date=Store.format("dd/MM",e.start,tz)+(e.allDay?" · Todo el día":" · "+Store.format("HH:mm",e.start,tz));
     text(c,date,32,y,18,false);fit(c,e.title,32,y+25,25,true,685);
    }
    if(shown.size()>4)text(c,"+ "+(shown.size()-4)+" eventos · abre Agenda para verlos",32,947,18,false);
   }
   line(c,963);
   long w=model.meta.optLong("weather_ok",0),cal=model.meta.optLong("calendar_ok",0);
   String status=note.length()>0?note:"Clima "+(w>0?Store.format(now-w>24*3600000L?"dd/MM HH:mm":"HH:mm",w,tz):"pendiente")+" · Calendario "+(cal>0?Store.format(now-cal>24*3600000L?"dd/MM HH:mm":"HH:mm",cal,tz):"pendiente");
   if(note.length()==0&&(now-w>90*60000L||now-cal>90*60000L||model.meta.optString("weather_error","").length()>0||model.meta.optString("calendar_error","").length()>0))status+=" · revisar conexión";
   fit(c,status,32,990,18,false,690);text(c,"Open-Meteo · Google Calendar · mantener pulsado: opciones",32,1013,14,false);c.restore();
  }
  public boolean onTouchEvent(MotionEvent e){float x=(e.getX()-ox)/scale,y=(e.getY()-oy)/scale;
   if(e.getAction()==MotionEvent.ACTION_DOWN){down=System.currentTimeMillis();dx=x;dy=y;return true;}
   if(e.getAction()==MotionEvent.ACTION_UP){if(Math.abs(x-dx)>25||Math.abs(y-dy)>25)return true;if(System.currentTimeMillis()-down>800){menu();return true;}
    if(y>=70&&y<120&&x>540){refresh();return true;}
    if(y>=270&&y<320){if(x<120)monthMove(-1);else if(x>638)monthMove(1);return true;}
    if(y>=353&&y<623&&x>=30&&x<723){int day=((int)(y-353)/45)*7+((int)(x-30)/99)-gridOffset+1;if(day>=1&&day<=days){selected=selected==day?0:day;invalidate();}return true;}
    if(y>=688&&y<938&&model!=null){int i=(int)(y-688)/57;if(i<shown.size()&&i<4)AppUi.detail(MainActivity.this,shown.get(i),model.zone);return true;}
    if(y>=938&&y<963){openAgenda();return true;}
    return true;
   }return true;
  }
 }
}
