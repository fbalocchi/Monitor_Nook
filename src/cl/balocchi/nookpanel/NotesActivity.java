package cl.balocchi.nookpanel;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.text.InputFilter;
public class NotesActivity extends BaseScreen {
 private EditText notes;private String original="";
 public void onCreate(Bundle state){super.onCreate(state);LinearLayout body=AppUi.column(this);body.addView(AppUi.text(this,"Mis notas",26,true));body.addView(AppUi.text(this,"Se guardan en este NOOK. Independientes de Google Calendar.",16,false));
  try{original=new String(Store.file(this,"notes.txt",65536),"UTF-8");}catch(Exception ignored){}
  notes=new EditText(this);notes.setGravity(Gravity.TOP|Gravity.LEFT);notes.setTextSize(21);notes.setTextColor(android.graphics.Color.BLACK);notes.setBackgroundColor(android.graphics.Color.WHITE);notes.setPadding(16,16,16,16);notes.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8000)});notes.setText(original);body.addView(notes,new LinearLayout.LayoutParams(-1,0,1));
  body.addView(AppUi.button(this,"Guardar notas",new View.OnClickListener(){public void onClick(View v){save(true);}}));AppUi.attach(this,body,2);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
 }
 private void save(boolean show){String text=notes.getText().toString();try{if(!text.equals(original)){Store.save(this,"notes.txt",text.getBytes("UTF-8"));original=text;}if(show)Toast.makeText(this,"Notas guardadas",Toast.LENGTH_SHORT).show();}catch(Exception e){if(show)AppUi.message(this,"Notas","No se pudieron guardar las notas.");}}
 protected void onPause(){if(notes!=null)save(false);super.onPause();}
}
