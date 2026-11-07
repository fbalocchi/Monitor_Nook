package cl.balocchi.nookpanel;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;

public class AgendaActivity extends BaseScreen {
 private final Handler handler=new Handler();private final ExecutorService worker=Executors.newSingleThreadExecutor();private int generation=0;private boolean alive=true,registered=false;
 private AgendaWindow window;private TextView title,status;private LinearLayout list;
 private BroadcastReceiver changed=new BroadcastReceiver(){public void onReceive(Context c,Intent i){load();}};
 public void onCreate(Bundle saved){super.onCreate(saved);TimeZone zone=CalendarEngine.localZone();Calendar today=Calendar.getInstance(zone,AppUi.ES);
  int year=getIntent().getIntExtra("year",today.get(Calendar.YEAR)),month=getIntent().getIntExtra("month",today.get(Calendar.MONTH)),day=getIntent().getIntExtra("day",0);
  if(saved!=null){year=saved.getInt("year",year);month=saved.getInt("month",month);day=saved.getInt("day",day);}window=new AgendaWindow(zone,year,month,day);
  LinearLayout body=AppUi.column(this);title=AppUi.text(this,"Agenda",25,true);body.addView(title);LinearLayout nav=new LinearLayout(this);
  String[] labels={"‹","Hoy","Mes","›"};for(int i=0;i<labels.length;i++){final int action=i;nav.addView(AppUi.button(this,labels[i],new View.OnClickListener(){public void onClick(View v){if(action==0||action==3)window=window.move(action==0?-1:1);else if(action==1){Calendar now=Calendar.getInstance(window.zone);window=new AgendaWindow(window.zone,now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DATE));}else window=new AgendaWindow(window.zone,window.year,window.month,0);load();}}),new LinearLayout.LayoutParams(0,-2,1));}body.addView(nav);
  status=AppUi.text(this,"Leyendo calendario guardado…",16,false);body.addView(status);ScrollView scroll=new ScrollView(this);list=AppUi.column(this);scroll.addView(list);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));AppUi.attach(this,body,1);
 }
 protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putInt("year",window.year);state.putInt("month",window.month);state.putInt("day",window.day);}
 protected void onResume(){super.onResume();registerReceiver(changed,new IntentFilter(UpdateService.CHANGED));registered=true;load();}
 protected void onPause(){if(registered){unregisterReceiver(changed);registered=false;}super.onPause();}
 protected void onDestroy(){alive=false;generation++;worker.shutdownNow();super.onDestroy();}
 private void load(){if(!alive||worker.isShutdown())return;final int run=++generation;final AgendaWindow range=window;title.setText(Store.format(range.day>0?"EEEE d 'de' MMMM":"MMMM yyyy",range.start,range.zone));status.setText("Leyendo calendario guardado…");list.removeAllViews();
  worker.execute(new Runnable(){public void run(){if(!alive||run!=generation)return;List<CalendarEngine.Event> events=new ArrayList<CalendarEngine.Event>();String problem="";AgendaWindow resolved=range;
   try{CalendarEngine calendar=new CalendarEngine(Store.file(AgendaActivity.this,"calendar.ics",Store.ICS_LIMIT),"America/Santiago");resolved=new AgendaWindow(calendar.zone,range.year,range.month,range.day);events=calendar.between(resolved.start,resolved.end);}
   catch(java.io.FileNotFoundException e){JSONObject state=Store.json(AgendaActivity.this,"state.json");problem="Todavía no hay eventos descargados. "+state.optString("calendar_error","Configura Google Calendar en Ajustes.");}
   catch(Exception e){problem="No se pudo leer el calendario ("+e.getClass().getSimpleName()+").";Diagnostics.network(AgendaActivity.this,"agenda",e);}
   final List<CalendarEngine.Event> result=events;final String error=problem;final AgendaWindow selectedRange=resolved;handler.post(new Runnable(){public void run(){if(alive&&run==generation){window=selectedRange;show(selectedRange,result,error);}}});
  }});
 }
 private void show(final AgendaWindow range,List<CalendarEngine.Event> events,String error){
  JSONObject meta=Store.json(this,"state.json");String network=meta.optString("calendar_error","");status.setText(error.length()>0?error:network.length()>0?"Datos guardados · "+network:"Google Calendar · solo lectura");
  if(error.length()>0)return;if(events.isEmpty()){list.addView(AppUi.text(this,"Sin eventos en este período",21,false));return;}
  int count=0;Calendar date=Calendar.getInstance(range.zone);date.setTimeInMillis(range.start);
  while(date.getTimeInMillis()<range.end){final int day=date.get(Calendar.DATE);AgendaWindow oneDay=new AgendaWindow(range.zone,date.get(Calendar.YEAR),date.get(Calendar.MONTH),day);long start=oneDay.start,end=oneDay.end;boolean heading=false;
   for(final CalendarEngine.Event event:events)if(event.start<end&&event.end>start){if(!heading){Button group=AppUi.button(this,Store.format("EEEE dd/MM",start,range.zone),new View.OnClickListener(){public void onClick(View v){window=new AgendaWindow(range.zone,range.year,range.month,day);load();}});list.addView(group);heading=true;}
    String when=event.allDay?"Todo el día":Store.format("HH:mm",Math.max(start,event.start),range.zone)+(event.start<start?" · continúa":"");
    Button item=AppUi.button(this,when+"\n"+event.title,new View.OnClickListener(){public void onClick(View v){AppUi.detail(AgendaActivity.this,event,range.zone);}});item.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);item.setPadding(AppUi.dp(this,20),AppUi.dp(this,9),AppUi.dp(this,14),AppUi.dp(this,9));list.addView(item);count++;
    if(count>=200){list.addView(AppUi.text(this,"Mostrando 200 entradas. Toca un día para consultar sus eventos.",18,true));return;}
   }date.setTimeInMillis(end);
  }
 }
}
