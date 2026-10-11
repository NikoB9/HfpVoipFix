package com.hfpvoipfix;
import android.app.*;
import android.content.*;
import android.os.*;
import java.io.*;
import java.util.concurrent.*;

/** Foreground, static reads only; independent of the Bluetooth module version. */
public final class FirmwareService extends Service {
    public static volatile boolean alive;
    private final ExecutorService work=Executors.newSingleThreadExecutor();
    private boolean started;
    private void state(String phase,String message){getSharedPreferences("firmware",0).edit().putString("phase",phase).putString("status",message).apply();}
    @Override public void onCreate(){super.onCreate();alive=true;
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("firmware","Collecte du firmware audio",NotificationManager.IMPORTANCE_LOW));}
    @Override public int onStartCommand(Intent i,int flags,int id){
        startForeground(704,new Notification.Builder(this,"firmware").setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("HfpVoipLab · fichiers audio système").setContentText("Lecture seule en cours").setOngoing(true).build());
        if(!started){started=true;work.execute(this::collect);}return START_NOT_STICKY;
    }
    private void collect(){state("running","Vérification root…");
        try {
            Root.check();
            if(CaptureService.alive)throw new IOException("Terminer la capture avant cette collecte.");
            String probe=LabProbe.read(this);if(!probe.contains(" busy=false "))throw new IOException("Attendre la fin de l'appel et du SCO.");
            String result=FirmwareArchive.build(getFilesDir(),new FirmwareArchive.Source(){
                public String paths()throws Exception{
                    // Fixed directories and patterns. Never recursively scans the device.
                    return Root.command("for d in /system/lib /system/lib64 /system_ext/lib /system_ext/lib64 /apex/com.android.btservices/lib /apex/com.android.btservices/lib64 /apex/com.android.bluetooth/lib /apex/com.android.bluetooth/lib64; do for n in libbluetooth.so libbluetooth_jni.so libbt-stack.so libbt-hci.so; do f=\"$d/$n\"; if [ -f \"$f\" ]; then echo \"$f\"; fi; done; done; for d in /vendor/lib/hw /vendor/lib64/hw /odm/lib/hw /odm/lib64/hw /vendor/lib /vendor/lib64 /odm/lib /odm/lib64; do for f in \"$d\"/audio*.so \"$d\"/lib*audio*.so \"$d\"/lib*Audio*.so \"$d\"/lib*speech*.so \"$d\"/lib*Speech*.so \"$d\"/lib*bluetooth*.so \"$d\"/lib*bt*.so \"$d\"/lib*BT*.so \"$d\"/lib*cvsd*.so \"$d\"/android.hardware.audio*.so \"$d\"/android.hardware.bluetooth*.so; do if [ -f \"$f\" ]; then echo \"$f\"; fi; done; done; for d in /vendor/etc /odm/etc /system/etc; do for f in \"$d\"/audio*.xml \"$d\"/mixer_paths*.xml \"$d\"/bluetooth_audio*.xml; do if [ -f \"$f\" ]; then echo \"$f\"; fi; done; done",60000,15);
                }
                public byte[] read(String path,long limit)throws Exception{return readStatic(path,limit);}
                public String transport(){state("running","Analyse des métadonnées HCI (sans export des paquets bruts)…");return TransportDiagnostic.inspect();}
                public String identity()throws Exception{
                    StringBuilder s=new StringBuilder("HfpVoipLab 1.7.11\nCollecte ms="+System.currentTimeMillis()+"\n");
                    for(String key:new String[]{"ro.build.fingerprint","ro.vendor.build.fingerprint","ro.product.device","ro.board.platform","ro.hardware","ro.hardware.audio.primary","ro.build.version.release"})s.append(key).append('=').append(Root.command("getprop "+key,4000,5));return s.toString();
                }
            },message->state("running",message));
            state("ready","Firmware prêt : "+result);
        }catch(Exception e){state("error","Collecte non validée : "+e.getMessage());}
        finally{stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
    }
    private static byte[] readStatic(String path,long limit)throws Exception {
        if(!FirmwareArchive.allowed(path))throw new IOException("Chemin interdit");
        // Resolve links as root; reject links into writable data, proc, sys or device nodes.
        String real=Root.command("readlink -f '"+path+"'",4096,5).trim();
        if(!FirmwareArchive.allowed(real))throw new IOException("Cible de lien hors liste autorisée");
        java.lang.Process process=new ProcessBuilder("su","-c","test -f '"+real+"' && exec cat '"+real+"'").start();
        ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> timeout=timer.schedule(()->process.destroyForcibly(),20,TimeUnit.SECONDS);
        try(InputStream in=process.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>limit)throw new IOException("limite de taille atteinte");out.write(buffer,0,n);}
            if(!process.waitFor(2,TimeUnit.SECONDS)||process.exitValue()!=0)throw new IOException("lecture refusée ou délai dépassé");return out.toByteArray();
        }finally{timeout.cancel(false);timer.shutdownNow();process.destroy();}
    }
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onDestroy(){alive=false;work.shutdown();super.onDestroy();}
}
