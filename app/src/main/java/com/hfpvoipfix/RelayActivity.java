package com.hfpvoipfix;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.graphics.Color;
import android.widget.*;
import java.util.Map;
/** Everyday controls only. Maintenance and diagnostic capture are explicit opt-ins. */
public final class RelayActivity extends Activity {
 private final Handler ui=new Handler(Looper.getMainLooper());
 private TextView status,description,notice;private Button toggle;private boolean visible,working;
 private String pendingMode;
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 private TextView text(LinearLayout l,String value,int size){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(Color.rgb(25,37,55));v.setPadding(0,dp(10),0,dp(10));l.addView(v);return v;}
 private Button button(LinearLayout l,String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setOnClickListener(v->action.run());l.addView(b,new LinearLayout.LayoutParams(-1,dp(56)));return b;}
 @Override public void onCreate(Bundle saved){super.onCreate(saved);
  LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(24),dp(30),dp(24),dp(24));page.setBackgroundColor(Color.WHITE);
  text(page,"Relais d’appels",28);text(page,"Les appels du Samsung, sur le Redmi et Android Auto.",17);
  status=text(page,"",23);description=text(page,"",16);notice=text(page,"",15);
  toggle=button(page,"Activer",()->{if(RelayService.enabled(this))disable();else activate(RelayService.mode(this));});
  CheckBox automatic=new CheckBox(this);automatic.setText("Démarrage automatique");automatic.setTextSize(16);automatic.setPadding(0,dp(16),0,dp(8));automatic.setChecked(getSharedPreferences("relay",0).getBoolean("auto_start",true));
  automatic.setOnCheckedChangeListener((v,checked)->getSharedPreferences("relay",0).edit().putBoolean("auto_start",checked).apply());page.addView(automatic);
  text(page,"Une fois activé, le relais reprend après le redémarrage et le premier déverrouillage du Redmi.",14);
  button(page,"Options",this::options);ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);refresh();
 }
 private void activate(String mode){if(working)return;pendingMode=mode;
  if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},190);return;}
  work(()->RelaySetup.activate(this,mode));
 }
 private interface Task{void run()throws Exception;}
 private void work(Task task){working=true;notice.setText("");refresh();new Thread(()->{try{task.run();}
  catch(Exception e){BridgeJournal.write(getFilesDir(),"SETUP_FAILED "+Privacy.clean(String.valueOf(e.getMessage())));ui.post(()->notice.setText(e instanceof java.io.IOException?e.getMessage():"Activation impossible. Vérifier l’installation dans Options."));}
  finally{ui.post(()->{working=false;refresh();});}},"relay-setup").start();}
 private void disable(){if(working)return;work(()->{
  if(!getSharedPreferences("relay",0).edit().putBoolean("enabled",false).commit())throw new java.io.IOException("Le réglage n’a pas pu être enregistré.");
  startService(new Intent(this,RelayService.class).setAction("stop"));
  long until=SystemClock.elapsedRealtime()+5000;while((RelayService.alive||RxBridgeService.alive)&&SystemClock.elapsedRealtime()<until)Thread.sleep(100);
 });}
 private void options(){new AlertDialog.Builder(this).setTitle("Options").setItems(new String[]{"Sortie du son","Diagnostic","Installation","Outils avancés","À propos"},(d,which)->{
  if(which==0)output();else if(which==1)startActivity(new Intent(this,DiagnosticsActivity.class));else if(which==2)installation();
  else if(which==3)startActivity(new Intent(this,MaintenanceActivity.class));
  else new AlertDialog.Builder(this).setTitle("Relais d’appels · 1.9.18").setMessage("Préparé pour le Redmi Note 8 Pro sous Android 11, avec Android Auto en USB. Le micro utilisé est celui du Redmi. La compatibilité avec d’autres téléphones ou autoradios n’est pas établie.").setPositiveButton("Fermer",null).show();
 }).setNegativeButton("Fermer",null).show();}
 private void output(){String[] modes={"telecom_aa_control","telecom_duplex","projection_only"};String current=RelayService.mode(this);int selected=current.equals(modes[1])?1:current.equals(modes[2])?2:0;
  new AlertDialog.Builder(this).setTitle("Sortie du son").setSingleChoiceItems(new String[]{"Autoradio · recommandé","Redmi · usage local","Compatibilité · secours"},selected,(d,index)->{
   d.dismiss();if(RelayService.enabled(this))activate(modes[index]);else{getSharedPreferences("relay",0).edit().putString("mode",modes[index]).apply();refresh();}
  }).setNegativeButton("Fermer",null).show();}
 private void installation(){new AlertDialog.Builder(this).setTitle("Installation initiale").setMessage("À faire une seule fois : autoriser le microphone et l’accès root, activer ce module dans LSPosed pour Bluetooth, Gestion des appels, android, system et Android Auto, puis redémarrer. Conserver le module Magisk 1.9.0-persistent.\n\nConnecter le Samsung au Redmi en Bluetooth et l’autoradio en USB pour Android Auto. Dans MIUI, autoriser le lancement automatique de cette application si le système le demande. Ouvrir ensuite cette application et appuyer sur Activer.\n\nLa sortie Redmi se choisit dans Options. Aucun diagnostic n’est nécessaire pour utiliser le relais.").setPositiveButton("Fermer",null).show();}
 private void refresh(){if(status==null)return;boolean enabled=RelayService.enabled(this);toggle.setEnabled(!working);toggle.setText(working?"Veuillez patienter…":enabled?"Désactiver":"Activer");
  String mode=RelayService.mode(this);description.setText("telecom_duplex".equals(mode)?"Son et micro sur le Redmi.":LabModes.isolated(mode)?"Son vers Android Auto. Commandes de secours dans les outils avancés.":"Appels et commandes sur Android Auto. Micro du Redmi.");
  if(!enabled){status.setText("Désactivé");return;}
  Map<String,String> p=RxGate.parse(getSharedPreferences("capture",0).getString("probe",""));long age=System.currentTimeMillis()-getSharedPreferences("capture",0).getLong("probe_at",0);
  if(age>=0&&age<8000&&!"1.9.18".equals(p.get("version"))){status.setText("Redémarrage du Redmi nécessaire");return;}
  if(RxBridgeService.alive&&age>=0&&age<8000&&"true".equals(p.get("call_active"))){status.setText("Appel en cours");return;}
  status.setText(RxBridgeService.alive&&getSharedPreferences("rx",0).getBoolean("armed",false)?"Activé · en attente d’un appel":"Préparation du relais…");
 }
 private final Runnable poll=new Runnable(){public void run(){refresh();if(visible)ui.postDelayed(this,2000);}};
 @Override public void onResume(){super.onResume();visible=true;if(RelayService.enabled(this)&&!RelayService.alive)try{startForegroundService(new Intent(this,RelayService.class));}catch(RuntimeException e){notice.setText("Appuyer sur Activer pour reprendre le relais.");}poll.run();}
 @Override public void onPause(){visible=false;ui.removeCallbacks(poll);super.onPause();}
 @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] result){super.onRequestPermissionsResult(code,permissions,result);if(code==190){if(result.length>0&&result[0]==PackageManager.PERMISSION_GRANTED)activate(pendingMode);else notice.setText("Autoriser le microphone pour activer le relais.");}}
}
