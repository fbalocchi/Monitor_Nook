package cl.balocchi.nookpanel;
public class DiagnosticPrivacyTest {
 public static void main(String[] args){
  String privateUrl="https://calendar.google.com/calendar/ical/private-user/private-testsecret/basic.ics";
  Throwable cause=new IllegalArgumentException(privateUrl+" Private event title");Throwable outer=new IllegalStateException("Secret notes "+privateUrl,cause);
  StackTraceElement[] frames=new StackTraceElement[100];for(int i=0;i<frames.length;i++)frames[i]=new StackTraceElement("example.FixedClass","method","PrivateFilename",42);outer.setStackTrace(frames);
  String report=Diagnostics.describe("calendar",outer);
  if(report.indexOf("private-user")>=0||report.indexOf("testsecret")>=0||report.indexOf("Secret notes")>=0||report.indexOf("Private event")>=0||report.indexOf("PrivateFilename")>=0)throw new AssertionError("Private content in report");
  if(report.indexOf("IllegalStateException")<0||report.indexOf("IllegalArgumentException")<0||report.indexOf("FixedClass.method:42")<0)throw new AssertionError("Missing useful error context");
  if(report.length()>4096)throw new AssertionError("Unbounded diagnostic");
  System.out.println("Diagnostic cause/frame context retained; exception messages, URLs, event text and notes excluded");
 }
}
