package cl.balocchi.nookpanel;
import android.app.*;
import android.os.Bundle;
import android.view.*;
public class BaseScreen extends Activity {
 protected void onCreate(Bundle state){super.onCreate(state);requestWindowFeature(Window.FEATURE_NO_TITLE);getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);}
 protected void onResume(){super.onResume();
  if(getSharedPreferences("panel",0).getBoolean("keep_awake",true))getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  Diagnostics.event(this,getClass().getSimpleName()+" resume");AppUi.notification(this);
 }
 protected void onPause(){Diagnostics.event(this,getClass().getSimpleName()+" pause");super.onPause();}
 protected void onDestroy(){Diagnostics.event(this,getClass().getSimpleName()+" destroy");super.onDestroy();}
 public void onBackPressed(){AppUi.panel(this);}
}
