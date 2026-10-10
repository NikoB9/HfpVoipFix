package com.hfpvoipfix;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.*;
import java.util.concurrent.*;

/** HCI logging is explicit, reversible, and never toggles Bluetooth or touches pairing. */
final class TransportDiagnostic {
    static final String PROPERTY="persist.bluetooth.btsnooplogmode";
    private static final String[] LOGS={
        "/data/misc/bluetooth/logs/btsnoop_hci.log.last",
        "/data/misc/bluetooth/logs/btsnoop_hci.log",
        "/data/misc/bluedroid/btsnoop_hci.log.last",
        "/data/misc/bluedroid/btsnoop_hci.log"};
    static boolean logPath(String p){for(String s:LOGS)if(s.equals(p))return true;return false;}
    static void setLogging(Context c,boolean enable)throws Exception {
        Root.check();
        if(CaptureService.alive||RxBridgeService.alive||FirmwareService.alive)throw new IOException("Arrêter capture, pont et collecte avant de changer la journalisation.");
        if(!LabProbe.read(c).contains(" busy=false "))throw new IOException("Attendre la fin de l’appel et du SCO.");
        SharedPreferences prefs=c.getSharedPreferences("transport",0);
        String current=Root.command("getprop "+PROPERTY,200,4).trim();
        if(!current.matches("|disabled|filtered|full"))throw new IOException("Valeur initiale inconnue : aucune modification.");
        if(enable){
            if(!prefs.contains("previous")){
                if(current.equals("full"))throw new IOException("Journalisation complète déjà demandée hors du lab ; réglage conservé. Réactiver Bluetooth si nécessaire.");
                if(!prefs.edit().putString("previous",current).commit())throw new IOException("Sauvegarde du réglage refusée.");
            }else if(!current.equals("full")&&!current.equals(prefs.getString("previous","")))throw new IOException("Réglage changé hors du lab : aucune modification.");
        }else{
            if(!prefs.contains("previous"))throw new IOException("Aucun réglage à restaurer par le lab.");
            String previous=prefs.getString("previous","");
            if(!previous.matches("|disabled|filtered|full"))throw new IOException("Sauvegarde invalide.");
            if(!current.equals("full")&&!current.equals(previous))throw new IOException("Réglage changé hors du lab : restauration annulée.");
        }
        String target=enable?"full":prefs.getString("previous","");
        Root.command("setprop "+PROPERTY+" '"+target+"'",500,4);
        if(!target.equals(Root.command("getprop "+PROPERTY,200,4).trim()))throw new IOException("Propriété non confirmée ; sauvegarde conservée.");
        if(!enable&&!prefs.edit().remove("previous").commit())throw new IOException("Réglage restauré ; effacement du marqueur non confirmé.");
    }
    static String inspect(){
        StringBuilder out=new StringBuilder("HfpVoipLab 1.7.8 · transport Bluetooth\nLecture après appel ; journaux bruts jamais copiés dans le ZIP.\n");
        for(String key:new String[]{PROPERTY,"persist.bluetooth.btsnoopdefaultmode","persist.bluetooth.btsnooppath","persist.vendor.connsys.chipid","vendor.connsys.adie.chipid"}){
            try{String v=Root.command("getprop "+key,1000,4).trim();
                // Do not publish an arbitrary configured filename from a custom ROM.
                out.append(key).append('=').append(key.endsWith("path")&&!v.isEmpty()&&!logPath(v)?"chemin personnalisé non collecté":v).append('\n');
            }catch(Exception e){out.append(key).append(" : lecture indisponible\n");}
        }
        int found=0;
        for(String path:LOGS){out.append("\n=== ").append(path).append(" ===\n");
            try{out.append(readLog(path));found++;}catch(Exception e){out.append("INDISPONIBLE : ").append(e.getMessage()).append('\n');}
        }
        out.append("\nFichiers HCI analysés=").append(found).append('\n');
        if(found==0)out.append("AUCUNE PREUVE HCI : activer explicitement la trace, désactiver/réactiver Bluetooth hors appel, puis effectuer un appel. MIUI peut ignorer la propriété AOSP.\n");
        return out.toString();
    }
    private static String readLog(String path)throws Exception {
        if(!logPath(path))throw new IOException("Chemin non autorisé");
        String real=Root.command("readlink -f '"+path+"'",4096,5).trim();
        if(!logPath(real))throw new IOException("Fichier absent ou cible non autorisée");
        Process process=new ProcessBuilder("su","-c","test -f '"+real+"' && exec head -c 134217729 '"+real+"'").start();
        ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> timeout=timer.schedule(()->process.destroyForcibly(),25,TimeUnit.SECONDS);
        try(InputStream in=process.getInputStream()){
            String result=HciMetadata.decode(in);
            // Decoder may stop deliberately at a bound; do not wait for a blocked writer.
            if(result.contains("LIMITE 128")||result.contains("Longueur de paquet invalide")){process.destroyForcibly();return result;}
            if(!process.waitFor(2,TimeUnit.SECONDS)||process.exitValue()!=0)throw new IOException("Lecture refusée ou délai dépassé");
            return result;
        }catch(EOFException e){throw new IOException("Journal absent, vide ou en-tête incomplet");}
        finally{timeout.cancel(false);timer.shutdownNow();process.destroyForcibly();}
    }
}
