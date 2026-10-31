package com.hfpvoipfix;
import android.content.Context;
import android.os.SystemClock;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
/** Commands address the real HFP client. An acknowledgement is not a completed call action. */
final class RemoteCallController {
 private static final AtomicBoolean sending=new AtomicBoolean();
 static String send(Context context,String action,String number)throws Exception{
  String token=Long.toHexString(System.nanoTime());String payload=token+":"+action+":"+number;
  if(CallCommand.parse(payload)==null)throw new IOException("Numéro ou commande non valide.");
  if(!sending.compareAndSet(false,true))throw new IOException("Une commande attend déjà sa réponse.");
  boolean published=false;
  try{
   String status=LabProbe.read(context);Map<String,String> p=RxGate.parse(status);
   if(!"1.9.18".equals(p.get("version"))||!"true".equals(p.get("control")))throw new IOException("Module de commandes 1.9.18 indisponible.");
   if(!LabModes.isolated(p.get("requested")))throw new IOException("Ces commandes directes sont réservées au relais AA isolé. Pour D5/D6 contrôlable, utiliser Téléphone.");
   if("dial".equals(action)&&(!"false".equals(p.get("busy"))||!"false".equals(p.get("dirty"))))throw new IOException("Attendre la fin de l’appel et du nettoyage.");
   if("dial".equals(action)&&(!RelayService.enabled(context)||!LabModes.isolated(RelayService.mode(context))||!RxBridgeService.alive||!context.getSharedPreferences("rx",0).getBoolean("armed",false)))throw new IOException("Activer AA isolé et attendre le relais prêt.");
   published=true;try{Root.command("setprop debug.hfpvoipfix.ctrl '"+payload+"'",1000,5);}catch(Exception failed){throw new IOException("Échec de transmission de la commande root.");}
   long until=SystemClock.elapsedRealtime()+6000;
   while(SystemClock.elapsedRealtime()<until){
    Thread.sleep(250);Map<String,String> reply=RxGate.parse(LabProbe.read(context));
    if(!token.equals(reply.get("ctrl_token")))continue;
    if(!"queued".equals(reply.get("ctrl_result")))throw new IOException("Commande refusée : "+reply.get("ctrl_result")+" · "+reply.get("ctrl_error"));
    return "Commande transmise au Samsung. Vérifier le nouvel état de l’appel.";
   }
   throw new IOException("Pas d’accusé : vérifier l’état du Samsung avant de réessayer.");
  }finally{
   if(published)try{Root.command("if [ \"$(getprop debug.hfpvoipfix.ctrl)\" = '"+payload+"' ]; then setprop debug.hfpvoipfix.ctrl ''; fi",1000,4);}catch(Exception ignored){}
   sending.set(false);
  }
 }
}
