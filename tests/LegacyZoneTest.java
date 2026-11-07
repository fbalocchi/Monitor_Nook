package cl.balocchi.nookpanel;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class LegacyZoneTest {
    public static void main(String[] args)throws Exception{
        TimeZone original=TimeZone.getDefault();int checks=0;
        try{
            String[] defaults={"UTC","America/Los_Angeles","America/Santiago"};
            for(int d=0;d<defaults.length;d++){
                TimeZone.setDefault(TimeZone.getTimeZone(defaults[d]));
                TimeZone display=CalendarEngine.localZone();
                if(!TimeZone.getDefault().getID().equals(defaults[d]))throw new AssertionError("Default changed");
                BufferedReader r=new BufferedReader(new FileReader("tests/chile-offset-vectors.csv"));
                try{String line;while((line=r.readLine())!=null){
                    if(line.startsWith("#")||line.trim().length()==0)continue;
                    String[] parts=line.split(",");long instant=Long.parseLong(parts[0]);int expected=Integer.parseInt(parts[1]);
                    if(display.getOffset(instant)!=expected)throw new AssertionError("Long offset at "+instant);
                    GregorianCalendar fields=new GregorianCalendar(TimeZone.getTimeZone("UTC"),Locale.US);
                    fields.setTimeInMillis(instant+display.getRawOffset());
                    int millis=fields.get(Calendar.HOUR_OF_DAY)*3600000+fields.get(Calendar.MINUTE)*60000+fields.get(Calendar.SECOND)*1000+fields.get(Calendar.MILLISECOND);
                    int actual=display.getOffset(fields.get(Calendar.ERA),fields.get(Calendar.YEAR),fields.get(Calendar.MONTH),fields.get(Calendar.DAY_OF_MONTH),fields.get(Calendar.DAY_OF_WEEK),millis);
                    if(actual!=expected)throw new AssertionError("Six-argument offset at "+instant+": "+actual+" expected "+expected);
                    GregorianCalendar local=new GregorianCalendar(display,Locale.US);local.setTimeInMillis(instant);
                    if(local.get(Calendar.ZONE_OFFSET)+local.get(Calendar.DST_OFFSET)!=expected)throw new AssertionError("Calendar fields at "+instant);
                    SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);format.setTimeZone(display);format.format(new Date(instant));
                    checks++;
                }}finally{r.close();}
            }
            System.out.println("Legacy six-argument timezone / default invariance / IANA vectors OK: "+checks);
        }finally{TimeZone.setDefault(original);}
    }
}
