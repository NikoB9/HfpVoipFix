package com.hfpvoipfix;
import android.app.*;import android.os.*;import android.content.*;import android.widget.*;
public final class RelayActivity extends Activity {
 private TextView status;private final Handler ui=new Handler(Looper.getMainLooper());private boolean visible;
 private Button button(LinearLayout l,String s,Runnable action){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->action.run());l.addView(b);return b;}
 @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout l=new LinearLayout(this);l.setOrientation(1);int pad=(int)(20*getResources().getDisplayMetrics().density);l.setPadding(pad,pad,pad,pad);TextView title=new TextView(this);title.setText("Relais d’appels · 1.9.1");title.setTextSize(25);l.addView(title);status=new TextView(this);status.setTextSize(17);l.addView(status);
  TextView help=new TextView(this);help.setText("Une fois activé, le relais attend les appels en arrière-plan et reprend après redémarrage. Utilisez Téléphone avec le compte Samsung. Android Auto sera testé dans une prochaine étape, en USB et sans fil.");l.addView(help);
  button(l,"Activer le relais",this::enable);button(l,"Désactiver le relais",()->{getSharedPreferences("relay",0).edit().putBoolean("enabled",false).apply();startService(new Intent(this,RelayService.class).setAction("stop"));refresh();});
  button(l,"Ouvrir Téléphone",()->{try{startActivity(new Intent(Intent.ACTION_DIAL));}catch(ActivityNotFoundException e){status.setText("Application Téléphone indisponible.");}});
  button(l,"Configuration initiale des comptes d’appel",()->{try{startActivity(new Intent(android.telecom.TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS));}catch(ActivityNotFoundException e){status.setText("Dans Téléphone, activer le compte Samsung/HFP et le choisir pour les appels.");}});
  button(l,"Diagnostic et rapport",()->startActivity(new Intent(this,ModeActivity.class)));setContentView(l);refresh();
 }
 private void enable(){if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},190);return;}getSharedPreferences("relay",0).edit().putBoolean("enabled",true).apply();startForegroundService(new Intent(this,RelayService.class));refresh();}
 private void refresh(){if(status==null)return;status.setText(RelayService.enabled(this)?(RxBridgeService.alive?getSharedPreferences("rx",0).getString("status","Préparation…"):getSharedPreferences("relay",0).getString("status","Préparation du relais…")):"Relais désactivé");}
 private final Runnable poll=new Runnable(){public void run(){refresh();if(visible)ui.postDelayed(this,2000);}};
 @Override protected void onResume(){super.onResume();visible=true;if(RelayService.enabled(this))startForegroundService(new Intent(this,RelayService.class));poll.run();}
 @Override protected void onPause(){visible=false;ui.removeCallbacks(poll);super.onPause();}
 @Override public void onRequestPermissionsResult(int n,String[] p,int[] r){super.onRequestPermissionsResult(n,p,r);if(n==190&&r.length>0&&r[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)enable();}
}
