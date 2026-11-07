package cl.balocchi.nookpanel;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;

public class CertificatePathTest {
 static final Provider SC=new org.spongycastle.jce.provider.BouncyCastleProvider();
 static byte[] bytes(String path)throws Exception{FileInputStream in=new FileInputStream(path);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;try{while((n=in.read(b))!=-1)out.write(b,0,n);}finally{in.close();}return out.toByteArray();}
 static X509Certificate cert(String name)throws Exception{return (X509Certificate)CertificateFactory.getInstance("X.509",SC).generateCertificate(new ByteArrayInputStream(bytes("tests-111/certs/"+name+".der")));}
 static List<X509Certificate> chain(String...names)throws Exception{List<X509Certificate> chain=new ArrayList<X509Certificate>();for(String n:names)chain.add(cert(n));return chain;}
 static int checks=0;
 static void pass(SecureHttp http,String host,String...names)throws Exception{http.validateChain(chain(names),host);if(http.certificateTrace.indexOf("PKIX verified")<0)throw new AssertionError("No verified path");checks++;}
 static void reject(SecureHttp http,String host,String...names)throws Exception{boolean rejected=false;try{http.validateChain(chain(names),host);}catch(CertificateException e){rejected=true;}catch(CertPathBuilderException e){rejected=true;}if(!rejected)throw new AssertionError("Invalid certificate accepted: "+Arrays.toString(names));checks++;}
 public static void main(String[] args)throws Exception{
  SecureHttp http=new SecureHttp(bytes("tests-111/certs/trust-modern.pem"));
  List<X509Certificate> supplied=chain("leaf","intermediate","root-cross");
  Set<TrustAnchor> roots=new HashSet<TrustAnchor>();roots.add(new TrustAnchor(cert("root-modern"),null));PKIXParameters params=new PKIXParameters(roots);params.setRevocationEnabled(false);
  boolean reproduced=false;try{CertPathValidator.getInstance("PKIX",SC).validate(CertificateFactory.getInstance("X.509",SC).generateCertPath(supplied),params);}catch(CertPathValidatorException e){reproduced=e.getIndex()==-1&&e.getMessage().indexOf("Trust anchor")>=0;}
  if(!reproduced)throw new AssertionError("Observed anchor-not-found failure not reproduced");
  pass(http,"calendar.google.com","leaf","intermediate");
  pass(http,"calendar.google.com","leaf","intermediate","root-cross");
  if(http.certificateTrace.indexOf("path certificates=2")<0)throw new AssertionError("Cross-sign was not excluded");
  pass(http,"calendar.google.com","leaf","root-cross","intermediate");
  pass(http,"calendar.google.com","leaf","intermediate","root-modern");
  pass(http,"calendar.google.com","leaf","intermediate","root-cross","root-legacy");
  reject(http,"calendar.google.com","leaf","root-cross");
  reject(http,"wrong.example","leaf","intermediate","root-cross");
  reject(http,"calendar.google.com","leaf-expired","intermediate","root-cross");
  reject(http,"calendar.google.com","leaf-wrong-eku","intermediate","root-cross");
  reject(http,"calendar.google.com","leaf-no-san","intermediate","root-cross");
  reject(http,"calendar.google.com","leaf-untrusted","root-legacy");
  reject(http,"calendar.google.com","leaf-tampered","intermediate","root-cross");
  if(!SecureHttp.matches("calendar.google.com","*.google.com")||SecureHttp.matches("x.calendar.google.com","*.google.com")||SecureHttp.matches("evilgoogle.com","*.google.com"))throw new AssertionError("Wildcard policy changed");
  SecureHttp stopped=new SecureHttp(bytes("tests-111/certs/trust-modern.pem")){protected java.net.Socket openSocket(String host)throws IOException{throw new IOException("Private request is never sent");}};
  try{stopped.get("https://calendar.google.com/calendar/ical/private-user/private-testsecret/basic.ics",100);}catch(IOException expected){}
  if(stopped.certificateTrace.indexOf("testsecret")>=0||stopped.certificateTrace.indexOf("private-user")>=0||stopped.certificateTrace.indexOf("basic.ics")>=0)throw new AssertionError("Request secret in certificate trace");
  System.out.println("Observed direct-validation failure reproduced. PKIX path-building acceptance/rejection cases OK: "+checks+"; hostname, expiry, signature, trust and EKU retained.");
 }
}
