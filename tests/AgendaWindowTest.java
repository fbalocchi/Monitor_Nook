package cl.balocchi.nookpanel;
import java.io.*;import java.util.*;
public class AgendaWindowTest {
 public static void main(String[] args)throws Exception{
  TimeZone zone=CalendarEngine.localZone();int checks=0,shortDays=0,longDays=0;
  BufferedReader in=new BufferedReader(new FileReader("tests/agenda-window-vectors.csv"));String line;
  try{while((line=in.readLine())!=null){String[] p=line.split(",");int y=Integer.parseInt(p[0]),m=Integer.parseInt(p[1]),d=Integer.parseInt(p[2]);long from=Long.parseLong(p[3]),to=Long.parseLong(p[4]);AgendaWindow window=new AgendaWindow(zone,y,m,d);
   if(window.start!=from||window.end!=to)throw new AssertionError("IANA boundary "+line+" actual "+window.start+","+window.end);
   AgendaWindow next=window.move(1);if(next.start!=window.end)throw new AssertionError("Next period must start at exclusive end");
   AgendaWindow back=next.move(-1);if(back.start!=window.start||back.end!=window.end)throw new AssertionError("Roundtrip navigation");
   if(d>0&&to-from==23*3600000L)shortDays++;if(d>0&&to-from==25*3600000L)longDays++;checks++;
  }}finally{in.close();}
  if(shortDays<3||longDays<3)throw new AssertionError("Missing DST transition coverage");
  int[][] allDays={{2026,8,6,23},{2026,3,4,25}};
  for(int[] d:allDays){String start=String.format(Locale.US,"%04d%02d%02d",d[0],d[1]+1,d[2]),end=String.format(Locale.US,"%04d%02d%02d",d[0],d[1]+1,d[2]+1);
   String ics="BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Test//EN\r\nBEGIN:VEVENT\r\nUID:dst-all-day\r\nDTSTART;VALUE=DATE:"+start+"\r\nDTEND;VALUE=DATE:"+end+"\r\nSUMMARY:All day at DST\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";
   CalendarEngine engine=new CalendarEngine(ics.getBytes("UTF-8"),"America/Santiago");AgendaWindow range=new AgendaWindow(zone,d[0],d[1],d[2]);java.util.List<CalendarEngine.Event> es=engine.between(range.start,range.end);
   if(es.size()!=1||es.get(0).start!=range.start||es.get(0).end!=range.end||es.get(0).end-es.get(0).start!=d[3]*3600000L)throw new AssertionError("All-day DST event shifted: "+start);
  }
  System.out.println("Agenda IANA day/month boundaries and bidirectional navigation OK: "+checks+" (23h="+shortDays+",25h="+longDays+"); all-day DST events OK");
 }
}
