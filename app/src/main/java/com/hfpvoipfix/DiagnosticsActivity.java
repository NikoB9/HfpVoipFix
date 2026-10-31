package com.hfpvoipfix;
import android.app.*;import android.content.*;import android.os.*;import android.widget.*;import java.io.File;
/** A report can be collected on demand; ordinary relay use never starts this capture. */
public final class DiagnosticsActivity extends Activity {
 private TextView status;private Button begin,finish,save;private boolean visible,writing;private final Handler ui=new Handler(Looper.getMainLooper());
 private Button button(LinearLayout l,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setOnClickListener(v->action.run());l.addView(b);return b;}
 @Override public void onCreate(Bundle state){super.onCreate(state);LinearLayout l=new LinearLayout(this);l.setOrientation(1);int pad=(int)(24*getResources().getDisplayMetrics().density);l.setPadding(pad,pad,pad,pad);
  TextView title=new TextView(this);title.setText("Diagnostic");title.setTextSize(26);l.addView(title);TextView help=new TextView(this);help.setText("À utiliser seulement en cas de problème. Démarrer, reproduire le problème pendant un appel, puis terminer et enregistrer le rapport dans Téléchargements. Le relais continue de fonctionner indépendamment du diagnostic.");l.addView(help);status=new TextView(this);status.setTextSize(17);l.addView(status);
  begin=button(l,"Démarrer le diagnostic",()->{
   Runnable start=()->startForegroundService(new Intent(this,CaptureService.class).setAction(CaptureService.START));
   if(new File(getFilesDir(),"HFP-Rapport.zip").isFile())new AlertDialog.Builder(this).setTitle("Nouveau diagnostic ?").setMessage("Enregistrer le rapport précédent si vous souhaitez le conserver séparément.").setNegativeButton("Annuler",null).setPositiveButton("Démarrer",(d,w)->start.run()).show();else start.run();
  });
  finish=button(l,"Terminer le diagnostic",()->startForegroundService(new Intent(this,CaptureService.class).setAction(CaptureService.STOP)));
  save=button(l,"Enregistrer dans Téléchargements",()->{writing=true;refresh();new Thread(()->{try{String name=DownloadExport.save(getContentResolver(),new File(getFilesDir(),"HFP-Rapport.zip"),"HFP-Rapport");ui.post(()->message("Rapport enregistré",name+"\n\nOuvrir la conversation souhaitée puis joindre ce fichier depuis Téléchargements."));}catch(Exception e){ui.post(()->message("Enregistrement impossible","Réessayer après la fin du diagnostic."));}finally{ui.post(()->{writing=false;refresh();});}},"report-save").start();});
  TextView privacy=new TextView(this);privacy.setText("Le rapport contient des informations techniques partiellement masquées. Aucun son n’est enregistré. Vérifier son contenu avant de le transmettre.");l.addView(privacy);button(l,"Outils avancés",()->startActivity(new Intent(this,MaintenanceActivity.class)));ScrollView scroll=new ScrollView(this);scroll.addView(l);setContentView(scroll);refresh();
 }
 private void message(String title,String text){if(!isFinishing()&&!isDestroyed())new AlertDialog.Builder(this).setTitle(title).setMessage(text).setPositiveButton("Fermer",null).show();}
 private void refresh(){String phase=getSharedPreferences("capture",0).getString("phase","");boolean active=CaptureService.alive&&(phase.equals("starting")||phase.equals("running")||phase.equals("processing"));
  status.setText(phase.equals("running")&&CaptureService.alive?"Diagnostic en cours":phase.equals("processing")&&CaptureService.alive?"Préparation du rapport…":phase.equals("starting")&&CaptureService.alive?"Démarrage…":phase.equals("ready")?"Rapport prêt":phase.equals("error")?"Le diagnostic a été interrompu. Consulter les outils avancés.":"Aucun diagnostic en cours");
  begin.setEnabled(!active&&!writing&&RelayService.enabled(this));finish.setEnabled(CaptureService.alive&&phase.equals("running")&&!writing);save.setEnabled(!active&&!writing&&phase.equals("ready")&&new File(getFilesDir(),"HFP-Rapport.zip").isFile());
  if(!RelayService.enabled(this)&&!active)status.setText("Activer d’abord le relais sur l’écran principal.");
 }
 private final Runnable poll=new Runnable(){public void run(){refresh();if(visible)ui.postDelayed(this,1000);}};
 @Override public void onResume(){super.onResume();visible=true;poll.run();}
 @Override public void onPause(){visible=false;ui.removeCallbacks(poll);super.onPause();}
}
