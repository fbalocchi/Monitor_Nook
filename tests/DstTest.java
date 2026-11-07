package cl.balocchi.nookpanel;
import java.util.*;import java.text.*;
public class DstTest {
 public static void main(String[] args)throws Exception{
  String tz="BEGIN:VTIMEZONE\r\nTZID:America/Santiago\r\nBEGIN:STANDARD\r\nDTSTART:19700405T000000\r\nRRULE:FREQ=YEARLY;BYMONTH=4;BYDAY=1SU\r\nTZOFFSETFROM:-0300\r\nTZOFFSETTO:-0400\r\nEND:STANDARD\r\nBEGIN:DAYLIGHT\r\nDTSTART:19700906T000000\r\nRRULE:FREQ=YEARLY;BYMONTH=9;BYDAY=1SU\r\nTZOFFSETFROM:-0400\r\nTZOFFSETTO:-0300\r\nEND:DAYLIGHT\r\nEND:VTIMEZONE\r\n";
  String s="BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Test//ES\r\n"+tz+"BEGIN:VEVENT\r\nUID:dst\r\nDTSTART;TZID=America/Santiago:20260905T090000\r\nDTEND;TZID=America/Santiago:20260905T100000\r\nRRULE:FREQ=WEEKLY;COUNT=3\r\nSUMMARY:Clase\\, taller \r\n y reunion\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";
  CalendarEngine e=new CalendarEngine(s.getBytes("UTF-8"),"America/Santiago");
  SimpleDateFormat utc=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US);utc.setTimeZone(TimeZone.getTimeZone("UTC"));
  SimpleDateFormat local=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US);local.setTimeZone(e.zone);
  java.util.List<CalendarEngine.Event> out=e.between(utc.parse("2026-09-01 00:00").getTime(),utc.parse("2026-10-01 00:00").getTime());
  if(out.size()!=3)throw new AssertionError("count");
  String[] dates={"2026-09-05 13:00","2026-09-12 12:00","2026-09-19 12:00"};
  for(int i=0;i<3;i++){if(!utc.format(out.get(i).start).equals(dates[i]))throw new AssertionError("DST UTC "+i+" "+utc.format(out.get(i).start));if(!local.format(out.get(i).start).endsWith("09:00"))throw new AssertionError("local hour");}
  if(!out.get(0).title.equals("Clase, taller y reunion"))throw new AssertionError("Unfolding: "+out.get(0).title);
  System.out.println("Embedded timezone/DST/folded text OK");
 }
}
