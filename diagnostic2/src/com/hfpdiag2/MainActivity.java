package com.hfpdiag2;
import android.app.*;import android.content.*;import android.os.*;import android.widget.*;import java.io.*;
public final class MainActivity extends Activity {
 private TextView status;
 private Button start,stop,share,snap;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private boolean visible;
 private void action(String s){Intent i=new Intent(this,CaptureService.class).setAction(s);startForegroundService(i);}
 @Override public void onCreate(Bundle b){super.onCreate(b);
  ScrollView sc=new ScrollView(this);LinearLayout l=new LinearLayout(this);l.setPadding(26,22,26,22);l.setOrientation(1);sc.addView(l);
  TextView title=new TextView(this);title.setText("Diagnostic Bluetooth HFP");title.setTextSize(25);l.addView(title);
  TextView intro=new TextView(this);intro.setText("Redmi Note 8 Pro + S24 Ultra\n3 étapes, sans commande Termux ni redémarrage.");intro.setTextSize(17);l.addView(intro);
  status=new TextView(this);status.setTextSize(18);l.addView(status);
  start=add(l,"1 — Démarrer la capture",()->action(CaptureService.START));
  snap=add(l,"Pendant l’appel : instantané audio (optionnel)",()->action(CaptureService.SNAP));
  stop=add(l,"2 — Arrêter et préparer le rapport",()->action(CaptureService.STOP));
  share=add(l,"3 — Envoyer le ZIP dans ChatGPT",()->{
    File f=new File(getFilesDir(),"HFP-Rapport.zip");if(!f.isFile())return;
    Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/zip");
    i.putExtra(Intent.EXTRA_STREAM,ReportProvider.URI);
    i.setClipData(ClipData.newUri(getContentResolver(),"HFP",ReportProvider.URI));
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    startActivity(Intent.createChooser(i,"Envoyer le diagnostic"));
  });
  TextView help=new TextView(this);help.setText("Mode d’emploi : démarre la capture, autorise Magisk, passe un appel court vers le 666, raccroche, arrête la capture puis partage le ZIP. Si les logs ne sont pas accessibles, le rapport indique l’erreur.\n\nDiagnostic en lecture seule : ne change ni LSPosed, ni le Bluetooth, ni la configuration audio.\n\nVérifie le ZIP avant partage : les données privées sont masquées partiellement.");l.addView(help);
  setContentView(sc);refresh();
 }
 private Button add(LinearLayout l,String title,Runnable f){Button b=new Button(this);b.setAllCaps(false);b.setText(title);b.setOnClickListener(v->f.run());l.addView(b);return b;}
 private void refresh(){
  String phase=getSharedPreferences("capture",0).getString("phase","idle");
  String msg=getSharedPreferences("capture",0).getString("status","Prêt pour un diagnostic.");
  int count=getSharedPreferences("capture",0).getInt("count",0);
  status.setText(msg+"\nÉvénements reçus : "+count);
  start.setEnabled(!phase.equals("running")&&!phase.equals("starting")&&!phase.equals("processing"));
  snap.setEnabled(phase.equals("running"));
  stop.setEnabled(phase.equals("running")||phase.equals("starting"));
  share.setEnabled(new File(getFilesDir(),"HFP-Rapport.zip").isFile()&&!phase.equals("running"));
 }
 private final Runnable poll=new Runnable(){public void run(){refresh();if(visible)handler.postDelayed(this,1200);}};
 @Override protected void onResume(){super.onResume();visible=true;poll.run();}
 @Override protected void onPause(){visible=false;handler.removeCallbacks(poll);super.onPause();}
}