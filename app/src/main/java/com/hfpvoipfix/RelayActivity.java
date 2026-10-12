package com.hfpvoipfix;
import android.app.*;import android.os.*;import android.content.*;import android.widget.*;
public final class RelayActivity extends Activity {
 private TextView status;private final Handler ui=new Handler(Looper.getMainLooper());private boolean visible;private String pendingMode="telecom_duplex";
 private Button button(LinearLayout l,String s,Runnable action){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->action.run());l.addView(b);return b;}
 @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout l=new LinearLayout(this);l.setOrientation(1);int pad=(int)(20*getResources().getDisplayMetrics().density);l.setPadding(pad,pad,pad,pad);TextView title=new TextView(this);title.setText("Relais d’appels · 1.9.3");title.setTextSize(25);l.addView(title);status=new TextView(this);status.setTextSize(17);l.addView(status);
  TextView help=new TextView(this);help.setText("Relais local : son et micro du Redmi. Relais AA : essais avec l’autoradio, en USB ou sans fil. Le relais choisi reprend après redémarrage. Changez de relais hors appel.");l.addView(help);
  button(l,"Activer le relais",()->enable("telecom_duplex"));button(l,"Activer le relais AA",()->enable("telecom_aa"));button(l,"Désactiver le relais",()->{getSharedPreferences("relay",0).edit().putBoolean("enabled",false).apply();startService(new Intent(this,RelayService.class).setAction("stop"));refresh();});
  button(l,"Ouvrir Téléphone",()->{try{startActivity(new Intent(Intent.ACTION_DIAL));}catch(ActivityNotFoundException e){status.setText("Application Téléphone indisponible.");}});
  button(l,"Configuration initiale des comptes d’appel",()->{try{startActivity(new Intent(android.telecom.TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS));}catch(ActivityNotFoundException e){status.setText("Dans Téléphone, activer le compte Samsung/HFP et le choisir pour les appels.");}});
  button(l,"Options avancées et rapport",()->startActivity(new Intent(this,ModeActivity.class)));setContentView(l);refresh();
 }
 private void enable(String mode){pendingMode=mode;if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},190);return;}
  new Thread(()->{try{
   if(RxBridgeService.alive&&!RelayService.enabled(this))throw new java.io.IOException("Terminer le pont de diagnostic avant d’activer le relais.");
   if(RelayService.enabled(this)&&!mode.equals(RelayService.mode(this))){
    java.util.Map<String,String> v=RxGate.parse(LabProbe.read(this));
    if(!"false".equals(v.get("busy"))||!"false".equals(v.get("dirty")))throw new java.io.IOException("Attendre la fin de l’appel et du nettoyage avant de changer de relais.");
    getSharedPreferences("relay",0).edit().putString("mode",mode).apply();
    if(RxBridgeService.alive)startService(new Intent(this,RxBridgeService.class).setAction("reconfigure"));
   }
   getSharedPreferences("relay",0).edit().putString("mode",mode).putBoolean("enabled",true).apply();
   startForegroundService(new Intent(this,RelayService.class));ui.post(this::refresh);
  }catch(Exception e){ui.post(()->status.setText(String.valueOf(e.getMessage())));}},"relay-select").start();
 }
 private void refresh(){if(status==null)return;status.setText(RelayService.enabled(this)?((LabModes.car(RelayService.mode(this))?"D6 · Relais AA\n":"D5 · Relais local\n")+(RxBridgeService.alive?getSharedPreferences("rx",0).getString("status","Préparation…"):getSharedPreferences("relay",0).getString("status","Préparation du relais…"))):"Relais désactivé");}
 private final Runnable poll=new Runnable(){public void run(){refresh();if(visible)ui.postDelayed(this,2000);}};
 @Override protected void onResume(){super.onResume();visible=true;if(RelayService.enabled(this))startForegroundService(new Intent(this,RelayService.class));poll.run();}
 @Override protected void onPause(){visible=false;ui.removeCallbacks(poll);super.onPause();}
 @Override public void onRequestPermissionsResult(int n,String[] p,int[] r){super.onRequestPermissionsResult(n,p,r);if(n==190&&r.length>0&&r[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)enable(pendingMode);}
}
