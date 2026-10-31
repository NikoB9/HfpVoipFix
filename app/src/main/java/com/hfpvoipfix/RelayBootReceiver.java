package com.hfpvoipfix;
import android.content.*;
public final class RelayBootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){
  boolean microphone=c.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED;
  if(!RelayStartupPolicy.restore(i==null?null:i.getAction(),RelayService.enabled(c),c.getSharedPreferences("relay",0).getBoolean("auto_start",true),microphone))return;
  c.getSharedPreferences("aa_diagnostic",0).edit().putBoolean("enabled",false).putString("call","").apply();
  c.getSharedPreferences("aa_trials",0).edit().putBoolean("enabled",false).putInt("index",0).putString("call","").putInt("assigned",-1).apply();
  try{c.startForegroundService(new Intent(c,RelayService.class));}
  catch(RuntimeException blocked){c.getSharedPreferences("relay",0).edit().putString("status","Ouvrir l’application pour reprendre le relais.").apply();BridgeJournal.write(c.getFilesDir(),"BOOT_RESTORE_UNAVAILABLE "+blocked.getClass().getSimpleName());}
 }
}
