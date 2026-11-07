import com.android.apksig.ApkVerifier;
import java.io.File;
public class VerifyApk {
 public static void main(String[] args)throws Exception{
  ApkVerifier.Result r=new ApkVerifier.Builder(new File(args.length == 0 ? "official/Panel-NOOK.apk" : args[0])).setMinCheckedPlatformVersion(7).setMaxCheckedPlatformVersion(7).build().verify();
  System.out.println("API7 signature verified: "+r.isVerified()+", v1: "+r.isVerifiedUsingV1Scheme());
  System.out.println("Errors: "+r.getErrors());
  for(ApkVerifier.Result.V1SchemeSignerInfo s:r.getV1SchemeSigners())System.out.println("Signer "+s.getName()+" errors: "+s.getErrors());
  if(!r.isVerified())throw new AssertionError("Signature validation failed");
 }
}
