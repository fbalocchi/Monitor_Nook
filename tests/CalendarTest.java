package cl.balocchi.nookpanel;
import java.util.*;import java.text.*;
public class CalendarTest{
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static String pre="BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//NookPanel Test//ES\r\n";
 static String event(String p){return "BEGIN:VEVENT\r\n"+p+"\r\nEND:VEVENT\r\n";}
 public static void main(String[] args)throws Exception{
  String text=pre+event("UID:rec\r\nDTSTART:20261001T120000Z\r\nDTEND:20261001T130000Z\r\nRRULE:FREQ=DAILY;COUNT=5\r\nEXDATE:20261002T120000Z\r\nSUMMARY:Clase")
   +event("UID:rec\r\nRECURRENCE-ID:20261003T120000Z\r\nDTSTART:20261003T140000Z\r\nDTEND:20261003T150000Z\r\nSUMMARY:Cambiada")
   +event("UID:rec\r\nRECURRENCE-ID:20261004T120000Z\r\nDTSTART:20261004T120000Z\r\nSTATUS:CANCELLED")
   +event("UID:all\r\nDTSTART;VALUE=DATE:20261006\r\nDTEND;VALUE=DATE:20261008\r\nSUMMARY:Dos dias")
   +event("UID:single\r\nDTSTART;VALUE=DATE:20261009\r\nSUMMARY:Un dia")+"END:VCALENDAR\r\n";
  CalendarEngine c=new CalendarEngine(text.getBytes("UTF-8"),"America/Santiago");
  SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US);f.setTimeZone(c.zone);
  long a=f.parse("2026-10-01 00:00").getTime(),b=f.parse("2026-11-01 00:00").getTime();
  java.util.List<CalendarEngine.Event> es=c.between(a,b);
  for(CalendarEngine.Event e:es)System.out.println(e.title+" "+f.format(e.start)+" -- "+f.format(e.end));
  check(es.size()==5,"recurrence count expected 5 got "+es.size());
  check(es.get(1).title.equals("Cambiada"),"exception replaces original");
  check(f.format(es.get(3).start).equals("2026-10-06 00:00"),"all day start");
  check(f.format(es.get(3).end).equals("2026-10-08 00:00"),"all day exclusive end");
  check(f.format(es.get(4).end).equals("2026-10-10 00:00"),"no DTEND defaults one day");
  System.out.println("Calendar recurrence/date tests OK");
 }
}
