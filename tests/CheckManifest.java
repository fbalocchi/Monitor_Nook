import com.android.apksig.apk.ApkUtils;
import java.nio.ByteBuffer;
import java.io.*;
public class CheckManifest{
 public static void main(String[] args)throws Exception{
  FileInputStream in=new FileInputStream("AndroidManifest.xml.bin");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);in.close();ByteBuffer data=ByteBuffer.wrap(out.toByteArray());
  int min=ApkUtils.getMinSdkVersionFromBinaryAndroidManifest(data);data.rewind();String pkg=ApkUtils.getPackageNameFromBinaryAndroidManifest(data);
  System.out.println("Manifest: "+pkg+", minSDK="+min);if(min!=7||!pkg.equals("cl.balocchi.nookpanel"))throw new AssertionError("Incorrect manifest");
 }
}
