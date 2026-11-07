import com.android.apksig.ApkSigner;
import java.io.*;
import java.security.*;
import java.security.spec.*;
import java.security.cert.*;
import java.util.*;
public class SignApk {
 static byte[] read(String name)throws Exception{FileInputStream i=new FileInputStream(name);try{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=i.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}finally{i.close();}}
 public static void main(String[] args)throws Exception{
  PrivateKey key=KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(read(args[2])));
  X509Certificate cert=(X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(read(args[3])));
  ApkSigner.SignerConfig config=new ApkSigner.SignerConfig.Builder("NOOK",key,Collections.singletonList(cert)).build();
  new ApkSigner.Builder(Collections.singletonList(config)).setInputApk(new File(args[0])).setOutputApk(new File(args[1])).setMinSdkVersion(7)
   .setV1SigningEnabled(true).setV2SigningEnabled(false).setV3SigningEnabled(false).setV4SigningEnabled(false).build().sign();
  System.out.println("Signed using official Android apksig, minSDK7, v1/JAR");
 }
}
