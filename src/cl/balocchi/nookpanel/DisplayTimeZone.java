package cl.balocchi.nookpanel;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/** Bridge for Harmony calendars: never use ical4j's implicit-default callback. */
final class DisplayTimeZone extends TimeZone {
    private static final long serialVersionUID=1L;
    private static final TimeZone UTC=TimeZone.getTimeZone("UTC");
    private final TimeZone delegate;
    DisplayTimeZone(TimeZone delegate){this.delegate=delegate;setID(delegate.getID());}
    public int getOffset(long utcMillis){return delegate.getOffset(utcMillis);}
    public int getOffset(int era,int year,int month,int day,int dayOfWeek,int millis){
        // This API receives standard local time, including at DST transitions.
        GregorianCalendar fields=new GregorianCalendar(UTC,Locale.US);
        fields.clear();
        fields.set(Calendar.ERA,era);
        fields.set(year,month,day,0,0,0);
        long instant=fields.getTimeInMillis()+millis-getRawOffset();
        return delegate.getOffset(instant);
    }
    public int getRawOffset(){return delegate.getRawOffset();}
    public void setRawOffset(int offset){throw new UnsupportedOperationException("Read-only calendar timezone");}
    public boolean useDaylightTime(){return delegate.useDaylightTime();}
    public boolean inDaylightTime(Date date){return delegate.inDaylightTime(date);}
    public int getDSTSavings(){return delegate.getDSTSavings();}
}
