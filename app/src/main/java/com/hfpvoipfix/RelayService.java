package com.hfpvoipfix;
import android.app.*;
import android.content.*;
import android.os.*;
import java.util.*;
/** Persistent D5 supervisor, independent of diagnostic capture and future car transport. */
public final class RelayService extends Service {
    static volatile boolean alive;
    private volatile boolean stopped;private Thread monitor;
    static boolean enabled(Context c){return c.getSharedPreferences("relay",0).getBoolean("enabled",false);}
    static String mode(Context c){return LabModes.car(c.getSharedPreferences("relay",0).getString("mode","telecom_duplex"))?"telecom_aa":"telecom_duplex";}
    private void status(String s){getSharedPreferences("relay",0).edit().putString("status",s).apply();}
    @Override public void onCreate(){super.onCreate();alive=true;((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("relay","Relais d’appels",NotificationManager.IMPORTANCE_LOW));}
    @Override public int onStartCommand(Intent i,int flags,int id){
        if(i!=null&&"stop".equals(i.getAction())){getSharedPreferences("relay",0).edit().putBoolean("enabled",false).apply();stopped=true;startService(new Intent(this,RxBridgeService.class).setAction("stop"));new Thread(()->{try{if(!enabled(this))Root.command("setprop "+LabModes.PROP+" observe",1000,4);}catch(Exception e){BridgeJournal.write(getFilesDir(),"RELAY_STOP_MODE_RESET_FAILED");}},"hfp-relay-disable").start();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}
        if(!enabled(this)){stopSelf();return START_NOT_STICKY;}
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,RelayActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,0,new Intent(this,RelayService.class).setAction("stop"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        startForeground(706,new Notification.Builder(this,"relay").setSmallIcon(android.R.drawable.sym_call_incoming).setContentTitle("Relais d’appels actif").setContentText("Appels du Samsung via Téléphone").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Désactiver",stop).build()).build());
        if(monitor==null){monitor=new Thread(this::monitor,"hfp-relay-supervisor");monitor.start();}return START_STICKY;
    }
    private void monitor(){ProbeChannel channel=null;
        try{while(!stopped&&enabled(this)){
            try{
                if(!RxBridgeService.alive){
                    if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED)throw new Exception("Ouvrir l’application et autoriser le microphone.");
                    if(channel==null)channel=new ProbeChannel();String p=channel.read();Map<String,String> v=RxGate.parse(p);
                    if(!"1.9.8".equals(v.get("version")))throw new Exception("Installer la version du module puis redémarrer.");
                    if(!"false".equals(v.get("busy"))){status("Appel en cours : reprise du relais après raccrochage.");}
                    else if(!"false".equals(v.get("dirty"))){status("Nettoyage audio incomplet : ouvrir le diagnostic.");}
                    else if(!RelayPolicy.prepare(p,enabled(this),RxBridgeService.alive,true)){status("Module d’appels incomplet : vérifier LSPosed puis redémarrer.");}
                    else{
                        String selected=mode(this);
                        Root.command("setprop "+LabModes.PROP+" "+selected,1000,4);
                        getSharedPreferences("rx",0).edit().putBoolean("auto_paused",false).apply();
                        // Android 11 allows while-in-use FGS access for root/system callers.
                        Root.command("am start-foreground-service --user 0 -n com.hfpvoipfix/.RxBridgeService --ez autonomous true --es mode "+selected,4000,8);
                        channel.close();channel=null;status("Relais lancé. Attente du Samsung et des appels.");
                    }
                }
                Thread.sleep(3000);
            }catch(InterruptedException e){return;}catch(Exception e){status(String.valueOf(e.getMessage()));BridgeJournal.write(getFilesDir(),"RELAY_WAIT "+Privacy.clean(String.valueOf(e.getMessage())));if(channel!=null){channel.close();channel=null;}Thread.sleep(15000);}
        }}catch(InterruptedException e){Thread.currentThread().interrupt();}finally{if(channel!=null)channel.close();if(!enabled(this)){try{Root.command("setprop "+LabModes.PROP+" observe",1000,4);}catch(Exception ignored){}if(!enabled(this)){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}}}
    }
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onDestroy(){stopped=true;alive=false;if(monitor!=null)monitor.interrupt();super.onDestroy();}
}
