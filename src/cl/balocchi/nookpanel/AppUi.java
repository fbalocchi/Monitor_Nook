package cl.balocchi.nookpanel;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;

final class AppUi {
 static final Locale ES=new Locale("es","CL");
 static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
 static TextView text(Activity a,String s,int size,boolean bold){TextView v=new TextView(a);v.setText(s);v.setTextSize(size);v.setTextColor(Color.BLACK);if(bold)v.setTypeface(Typeface.DEFAULT_BOLD);v.setPadding(dp(a,12),dp(a,8),dp(a,12),dp(a,8));return v;}
 static Button button(Activity a,String s,View.OnClickListener action){Button b=new Button(a);b.setText(s);b.setTextColor(Color.BLACK);b.setTextSize(17);b.setMinimumHeight(dp(a,48));b.setOnClickListener(action);return b;}
 static LinearLayout column(Activity a){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);l.setBackgroundColor(Color.WHITE);return l;}
 static void attach(final Activity a,View body,int current){
  LinearLayout root=column(a);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));LinearLayout nav=new LinearLayout(a);nav.setBackgroundColor(Color.BLACK);
  String[] names={"Panel","Agenda","Notas","Ajustes"};
  for(int i=0;i<names.length;i++){final int target=i;Button b=button(a,names[i],new View.OnClickListener(){public void onClick(View v){go(a,target);}});b.setTextSize(15);b.setSingleLine();
   GradientDrawable bg=new GradientDrawable();bg.setColor(i==current?Color.BLACK:Color.WHITE);bg.setStroke(dp(a,1),Color.BLACK);b.setBackgroundDrawable(bg);b.setTextColor(i==current?Color.WHITE:Color.BLACK);
   nav.addView(b,new LinearLayout.LayoutParams(0,dp(a,51),1));
  }root.addView(nav,new LinearLayout.LayoutParams(-1,-2));a.setContentView(root);
 }
 static void go(Activity a,int target){Class<?>[] types={MainActivity.class,AgendaActivity.class,NotesActivity.class,SettingsActivity.class};if(a.getClass()==types[target])return;if(target==1&&a instanceof MainActivity){((MainActivity)a).openAgenda();return;}Intent i=new Intent(a,types[target]);if(target==0)i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);a.startActivity(i);if(!(a instanceof MainActivity))a.finish();}
 static void panel(Activity a){go(a,0);}
 static void message(Activity a,String title,String body){ScrollView scroll=new ScrollView(a);scroll.addView(text(a,body,18,false));new AlertDialog.Builder(a).setTitle(title).setView(scroll).setPositiveButton("Cerrar",null).show();}
 static void detail(Activity a,CalendarEngine.Event e,TimeZone zone){String time=Store.format("EEEE dd/MM/yyyy",e.start,zone);if(e.allDay){time+=" · Todo el día";String end=Store.format("dd/MM/yyyy",e.end-1,zone);if(!end.equals(Store.format("dd/MM/yyyy",e.start,zone)))time+="\nHasta el "+end;}
  else time+="\n"+Store.format("HH:mm",e.start,zone)+" – "+Store.format("dd/MM HH:mm",e.end,zone);
  message(a,e.title,time+(e.detail.length()>0?"\n\n"+e.detail:""));
 }
 static void notification(Context c){try{
  Intent i=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  Notification n=new Notification(android.R.drawable.ic_menu_my_calendar,"Panel NOOK",System.currentTimeMillis());n.flags=Notification.FLAG_ONGOING_EVENT|Notification.FLAG_NO_CLEAR;
  n.setLatestEventInfo(c,"Panel NOOK","Toca para volver al clima y la agenda",PendingIntent.getActivity(c,100,i,PendingIntent.FLAG_UPDATE_CURRENT));
  ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(100,n);
 }catch(Exception ignored){}}
 static void openNook(Activity a){
  List<ResolveInfo> homes=a.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0);
  ResolveInfo chosen=null;for(ResolveInfo info:homes)if(!info.activityInfo.packageName.equals(a.getPackageName())){chosen=info;if(info.activityInfo.packageName.startsWith("com.bn."))break;}
  if(chosen==null){message(a,"NOOK","No se encontró otro inicio disponible.");return;}
  a.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setComponent(new ComponentName(chosen.activityInfo.packageName,chosen.activityInfo.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
 }
}
