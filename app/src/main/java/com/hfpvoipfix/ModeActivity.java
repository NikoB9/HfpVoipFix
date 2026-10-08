package com.hfpvoipfix;
import android.app.*;import android.os.*;import android.provider.Settings;import android.content.*;import android.widget.*;
public class ModeActivity extends Activity {
 private TextView state; private String selected="observe";
 private static final String KEY="hfp_diag_audio_mode";
 private static final String[] MODES={"observe","minimal","endpoints","patches"};
 private static final String[] LABELS={"0 - Observation seule (aucune modification)","A - VoIP + bt_wbs, sans AudioPatch","B - VoIP + ports SCO, sans AudioPatch","C - Pont complet (risque de retour micro)"};
 private String read(){try{String m=Settings.Global.getString(getContentResolver(),KEY);for(String x:MODES)if(x.equals(m))return m;}catch(Exception ignored){}return "observe";}
 @Override public void onCreate(Bundle b){super.onCreate(b);
  ScrollView sc=new ScrollView(this); LinearLayout l=new LinearLayout(this);l.setPadding(28,24,28,24);l.setOrientation(1);sc.addView(l);
  TextView title=new TextView(this);title.setText("HFP VoIP Fix - laboratoire");title.setTextSize(22);l.addView(title);
  TextView hint=new TextView(this);hint.setText("Choisis un mode ENTRE deux appels. Les hooks LSPosed sont chargés au démarrage du processus Bluetooth. Le mode est relu à chaque nouvel appel.");l.addView(hint);
  state=new TextView(this);l.addView(state);selected=read();RadioGroup group=new RadioGroup(this);
  for(int i=0;i<MODES.length;i++){final int idx=i;RadioButton r=new RadioButton(this);r.setText(LABELS[i]);r.setId(100+i);group.addView(r);if(MODES[i].equals(selected))group.check(100+i);}
  group.setOnCheckedChangeListener((g,id)->{if(id>=100&&id<104)selected=MODES[id-100];});l.addView(group);
  Button apply=new Button(this);apply.setText("Appliquer au prochain appel");apply.setOnClickListener(v->{
   final String choice=selected;
   Runnable save=()->new Thread(()->{
     String result;try{
       java.lang.Process p=new ProcessBuilder("su","-c","settings put global "+KEY+" "+choice+" && settings get global "+KEY).redirectErrorStream(true).start();
       java.io.BufferedReader br=new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
       String line=br.readLine();int exit=p.waitFor();
       result=exit==0&&choice.equals(line)?"Mode enregistré : "+choice+" (prochain appel)":"Échec écriture root: "+line;
     }catch(Exception e){result="Erreur root: "+e.getMessage();}
     final String message=result;runOnUiThread(()->state.setText(message));
   }).start();
   if("patches".equals(choice))new AlertDialog.Builder(this).setTitle("Mode C expérimental").setMessage("Ce mode a déjà produit un retour micro déformé. Volume bas. Continuer?").setNegativeButton("Annuler",null).setPositiveButton("Activer",(d,w)->save.run()).show();else save.run();
  });l.addView(apply);
  TextView notes=new TextView(this);notes.setText("Utilise HFP Diagnostic 2.0 pour capturer les logs. Aucun changement de mode pendant les appels. Ne lis jamais /proc/asound/*/status pendant l'appel.");l.addView(notes);
  setContentView(sc);state.setText("Mode actuel : "+read());
 }
}