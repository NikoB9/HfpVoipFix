package com.hfpvoipfix;
import android.content.*;
import android.content.pm.PackageManager;
import java.io.IOException;
import java.util.Map;
/** User-visible activation; no diagnostic capture is needed for ordinary calling. */
final class RelaySetup {
 static void activate(Context context,String selected)throws Exception{
  if(context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)throw new IOException("Autoriser le microphone pour activer le relais.");
  String mode=RelayStartupPolicy.normalMode(selected);
  Map<String,String> state;
  try{state=RxGate.parse(LabProbe.read(context));}catch(Exception unavailable){BridgeJournal.write(context.getFilesDir(),"SETUP_PROBE_UNAVAILABLE "+unavailable.getClass().getSimpleName());throw new IOException("Le relais n’est pas prêt. Vérifier l’installation dans Options.");}
  if(!"1.9.18".equals(state.get("version")))throw new IOException("Redémarrer le Redmi pour terminer l’installation.");
  if(!"false".equals(state.get("busy"))||!"false".equals(state.get("dirty")))throw new IOException("Attendre la fin de l’appel avant de modifier le relais.");
  if(!"true".equals(state.get("route"))||!"true".equals(state.get("calls"))||!"true".equals(state.get("disconnect"))||!"3".equals(state.get("voip")))throw new IOException("Terminer la configuration du module dans Options, puis redémarrer.");
  if(RxBridgeService.alive&&!RelayService.enabled(context))throw new IOException("Terminer le test avancé en cours avant d’activer le relais.");
  boolean changed=!mode.equals(RelayService.mode(context));
  context.getSharedPreferences("aa_diagnostic",0).edit().putBoolean("enabled",false).putString("call","").apply();
  context.getSharedPreferences("aa_trials",0).edit().putBoolean("enabled",false).putInt("index",0).putString("call","").putInt("assigned",-1).apply();
  if(!context.getSharedPreferences("relay",0).edit().putString("mode",mode).putBoolean("enabled",true).commit())throw new IOException("Le réglage n’a pas pu être enregistré.");
  if(changed&&RxBridgeService.alive)context.startService(new Intent(context,RxBridgeService.class).setAction("reconfigure"));
  context.startForegroundService(new Intent(context,RelayService.class));
 }
}
