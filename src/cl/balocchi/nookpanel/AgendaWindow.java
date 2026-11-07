package cl.balocchi.nookpanel;
import java.util.*;
/** Local calendar boundaries, including 23/25-hour days and exclusive end. */
final class AgendaWindow {
 private static final TimeZone UTC=TimeZone.getTimeZone("UTC");
 final int year,month,day;final long start,end;final TimeZone zone;
 AgendaWindow(TimeZone zone,int year,int month,int day){this.zone=zone;Calendar c=Calendar.getInstance(UTC,Locale.US);c.clear();c.set(year,month,day>0?day:1,0,0,0);this.year=c.get(Calendar.YEAR);this.month=c.get(Calendar.MONTH);this.day=day>0?c.get(Calendar.DAY_OF_MONTH):0;start=boundary(zone,c.getTimeInMillis());
  c.add(this.day>0?Calendar.DATE:Calendar.MONTH,1);end=boundary(zone,c.getTimeInMillis());
 }
 AgendaWindow move(int direction){Calendar c=Calendar.getInstance(UTC,Locale.US);c.clear();c.set(year,month,day>0?day:1,0,0,0);c.add(day>0?Calendar.DATE:Calendar.MONTH,direction);return new AgendaWindow(zone,c.get(Calendar.YEAR),c.get(Calendar.MONTH),day>0?c.get(Calendar.DAY_OF_MONTH):0);}
 static long startOfDate(TimeZone zone,int year,int month,int day){Calendar c=Calendar.getInstance(UTC,Locale.US);c.clear();c.set(year,month,day,0,0,0);return boundary(zone,c.getTimeInMillis());}
 private static long boundary(TimeZone zone,long wall){
  // Resolve the first valid local instant. In Chile midnight can be skipped;
  // Calendar's lenient field conversion can otherwise choose the previous day.
  int before=zone.getOffset(wall-2*86400000L),after=zone.getOffset(wall+2*86400000L);
  long a=wall-before,b=wall-after,valid=Long.MAX_VALUE;
  if(zone.getOffset(a)==before)valid=a;
  if(zone.getOffset(b)==after)valid=Math.min(valid,b);
  if(valid!=Long.MAX_VALUE)return valid;
  long low=Math.min(a,b),high=Math.max(a,b);
  while(low<high){long mid=low+(high-low)/2;if(mid+zone.getOffset(mid)>=wall)high=mid;else low=mid+1;}
  return low;
 }
}
