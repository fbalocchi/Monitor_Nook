package cl.balocchi.nookpanel;
import android.app.Application;
import android.content.Context;
import java.io.*;

/** Bounded diagnostics: class names and frames, never URLs, notes or event text. */
public class Diagnostics extends Application {
 private byte[] reserve;
 public void onCreate(){
  super.onCreate();reserve=new byte[16384];
  final Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
  Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){
   public void uncaughtException(Thread thread,Throwable failure){
    reserve=null;
    try{Store.save(Diagnostics.this,"crash.txt",describe("uncaught",failure).getBytes("UTF-8"));}catch(Throwable ignored){}
    if(previous!=null)previous.uncaughtException(thread,failure);
    else{android.os.Process.killProcess(android.os.Process.myPid());System.exit(10);}
   }
  });
  event(this,"process_start_1.1.3");
 }
 static String describe(String phase,Throwable failure){
  StringBuilder s=new StringBuilder();s.append(System.currentTimeMillis()).append(" v1.1.3 ").append(phase).append('\n');
  for(int depth=0;failure!=null&&depth<8;depth++,failure=failure.getCause()){
   s.append(failure.getClass().getName());
   if(failure instanceof org.spongycastle.tls.TlsFatalAlert)s.append(" alert=").append(((org.spongycastle.tls.TlsFatalAlert)failure).getAlertDescription());
   if(failure instanceof java.security.cert.CertPathValidatorException)s.append(" cert_index=").append(((java.security.cert.CertPathValidatorException)failure).getIndex());
   s.append('\n');StackTraceElement[] frames=failure.getStackTrace();
   for(int i=0;i<Math.min(16,frames.length);i++)s.append("  ").append(frames[i].getClassName()).append('.').append(frames[i].getMethodName()).append(':').append(frames[i].getLineNumber()).append('\n');
  }
  return s.toString();
 }
 static synchronized void event(Context c,String phase){
  try{
   String old="";try{old=new String(Store.file(c,"activity.log",32768),"UTF-8");}catch(Exception ignored){}
   if(old.length()>12000)old=old.substring(old.length()-12000);
   Runtime r=Runtime.getRuntime();
   Store.save(c,"activity.log",(old+System.currentTimeMillis()+" "+phase+" pid="+android.os.Process.myPid()+" heap="+(r.totalMemory()-r.freeMemory())+"/"+r.maxMemory()+"\n").getBytes("UTF-8"));
  }catch(Exception ignored){}
 }
 static void network(Context c,String kind,Exception e){try{Store.save(c,kind+"-error.txt",describe(kind,e).getBytes("UTF-8"));}catch(Exception ignored){}}
 static void publicTls(Context c,String kind,String trace){try{if(trace.length()>16384)trace=trace.substring(0,16384);Store.save(c,kind+"-tls.txt",trace.getBytes("UTF-8"));}catch(Exception ignored){}}
}
