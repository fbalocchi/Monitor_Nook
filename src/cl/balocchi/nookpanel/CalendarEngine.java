package cl.balocchi.nookpanel;

import java.io.*;
import java.text.*;
import java.util.*;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;

/** Read-only RFC5545 expansion using ical4j. Nothing is written back to Google. */
public final class CalendarEngine {
    public static final class Event implements Comparable<Event> {
        public long start,end; public boolean allDay; public String title,detail;
        public int compareTo(Event e){return start<e.start?-1:start>e.start?1:title.compareTo(e.title);}
    }
    private static java.util.TimeZone fallback;
    public static synchronized java.util.TimeZone localZone(){
        if(fallback==null){
            System.setProperty("net.fortuna.ical4j.timezone.update.enabled","false");
            fallback=new TimeZoneRegistryImpl().getTimeZone("America/Santiago");
            if(fallback==null)fallback=java.util.TimeZone.getTimeZone("America/Santiago");
            fallback=new DisplayTimeZone(fallback);
        }
        return fallback;
    }
    private final net.fortuna.ical4j.model.Calendar calendar;
    public final java.util.TimeZone zone;
    public CalendarEngine(byte[] ics, String zoneId) throws Exception {
        System.setProperty("net.fortuna.ical4j.timezone.update.enabled","false");
        System.setProperty("ical4j.unfolding.relaxed","true");
        TimeZoneRegistry registry=new TimeZoneRegistryImpl();
        calendar=new CalendarBuilder(registry).build(new ByteArrayInputStream(ics));
        net.fortuna.ical4j.model.TimeZone z=registry.getTimeZone(zoneId);
        zone=new DisplayTimeZone(z!=null?z:java.util.TimeZone.getTimeZone(zoneId));
    }
    private static String uid(VEvent e){return e.getUid()==null?"":e.getUid().getValue();}
    private static String key(net.fortuna.ical4j.model.Date d){return d instanceof DateTime?String.valueOf(d.getTime()):d.toString();}
    private static boolean cancelled(VEvent e){return e.getStatus()!=null&&"CANCELLED".equals(e.getStatus().getValue());}
    private long dayTime(String date) throws ParseException {
        try{return AgendaWindow.startOfDate(zone,Integer.parseInt(date.substring(0,4)),Integer.parseInt(date.substring(4,6))-1,Integer.parseInt(date.substring(6,8)));}
        catch(RuntimeException e){throw new ParseException("Invalid calendar date",0);}
    }
    private void add(List<Event> result,VEvent e,Period p,boolean all,long from,long to)throws Exception{
        Event v=new Event();v.allDay=all;v.start=p.getStart().getTime();v.end=p.getEnd().getTime();
        if(all){
            // DATE values are calendar dates, independent of the UTC offset.
            SimpleDateFormat utc=new SimpleDateFormat("yyyyMMdd",Locale.US);utc.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            v.start=dayTime(utc.format(p.getStart()));v.end=dayTime(utc.format(p.getEnd()));
            if(v.end<=v.start){java.util.Calendar c=java.util.Calendar.getInstance(zone);c.setTimeInMillis(v.start);c.add(java.util.Calendar.DATE,1);v.end=c.getTimeInMillis();}
        }
        if(v.end<=v.start)v.end=v.start+60000;
        if(v.start>=to||v.end<=from)return;
        v.title=e.getSummary()==null?"Sin titulo":e.getSummary().getValue();
        String location=e.getLocation()==null?"":e.getLocation().getValue();
        String description=e.getDescription()==null?"":e.getDescription().getValue();
        v.detail=location+(location.length()>0&&description.length()>0?"\n\n":"")+description;
        result.add(v);
    }
    public synchronized List<Event> between(long from,long to)throws Exception{
        List<Event> out=new ArrayList<Event>();
        Map<String,Map<String,VEvent>> exceptions=new HashMap<String,Map<String,VEvent>>();
        List<VEvent> masters=new ArrayList<VEvent>();
        for(Object object:calendar.getComponents(Component.VEVENT)){
            VEvent e=(VEvent)object;
            if(e.getRecurrenceId()==null){masters.add(e);continue;}
            Map<String,VEvent> group=exceptions.get(uid(e));if(group==null){group=new HashMap<String,VEvent>();exceptions.put(uid(e),group);}
            String k=key(e.getRecurrenceId().getDate());VEvent old=group.get(k);
            if(old==null||sequence(e)>=sequence(old))group.put(k,e);
        }
        Period range=new Period(new DateTime(from-86400000L),new DateTime(to+86400000L));
        for(VEvent e:masters){
            if(cancelled(e)||e.getStartDate()==null)continue;
            boolean all=!(e.getStartDate().getDate() instanceof DateTime);
            Map<String,VEvent> ex=exceptions.get(uid(e));
            for(Object object:e.calculateRecurrenceSet(range)){
                Period p=(Period)object;String k=all?dateKey(p.getStart()):String.valueOf(p.getStart().getTime());
                if(ex==null||!ex.containsKey(k))add(out,e,p,all,from,to);
            }
        }
        // Changed instances are added separately, including instances moved into this range.
        for(Map<String,VEvent> group:exceptions.values())for(VEvent e:group.values()){
            if(cancelled(e)||e.getStartDate()==null)continue;
            boolean all=!(e.getStartDate().getDate() instanceof DateTime);
            DtEnd end=e.getEndDate(true);
            DateTime s=new DateTime(e.getStartDate().getDate());
            DateTime en=end==null?new DateTime(s.getTime()+(all?86400000L:60000L)):new DateTime(end.getDate());
            add(out,e,new Period(s,en),all,from,to);
        }
        Collections.sort(out);return out;
    }
    private static String dateKey(java.util.Date d){SimpleDateFormat f=new SimpleDateFormat("yyyyMMdd",Locale.US);f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));return f.format(d);}
    private static int sequence(VEvent e){return e.getSequence()==null?0:e.getSequence().getSequenceNo();}
}
