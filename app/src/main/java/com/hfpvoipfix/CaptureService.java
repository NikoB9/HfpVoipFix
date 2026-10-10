package com.hfpvoipfix;

import android.app.*;
import android.content.*;
import android.os.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.zip.*;

/** Adapted from diagnostic2 2.1: continuous root logcat, targeted filter, bounded first+tail retention. */
public final class CaptureService extends Service {
    public static volatile boolean alive;
    public static final String START="hfp.start",STOP="hfp.stop",SNAP="hfp.snap";
    private final ExecutorService work=Executors.newSingleThreadExecutor();
    private java.lang.Process logProcess;
    private Thread logThread;
    private volatile boolean capturing,stopping;
    private volatile long count,scanned,dropped;
    private volatile String error="";
    private long lastSnapshot;
    private String callEpoch="unknown";
    private final Pattern callPattern=Pattern.compile("CALL id=(\\d+) state=(\\d+) mode=(\\w+) ms=(\\d+)");
    private File file(String n){return new File(getFilesDir(),n);}
    private void state(String phase,String msg){getSharedPreferences("capture",0).edit().putString("phase",phase).putString("status",msg)
        .putLong("count",count).putLong("dropped",dropped).putString("error",error).putLong("heartbeat",System.currentTimeMillis()).apply();}
    private void fail(String text){error=text;getSharedPreferences("capture",0).edit().putString("error",text).apply();
        try{append("errors.txt",text+"\n",2000000);}catch(Exception ignored){}
    }
    private void save(String n,String text)throws IOException{Files.write(file(n).toPath(),text.getBytes(StandardCharsets.UTF_8));}
    private synchronized void append(String n,String text,long limit)throws IOException{
        File f=file(n);if(f.length()>limit)return;
        try(FileOutputStream out=new FileOutputStream(f,true)){out.write(text.getBytes(StandardCharsets.UTF_8));
            if(f.length()>limit)out.write("\nLIMITE ATTEINTE : instantanés suivants omis.\n".getBytes(StandardCharsets.UTF_8));}
    }
    private static boolean relevant(String line){String l=line.toLowerCase(Locale.ROOT);
        return l.contains("hfpvoipfix")||l.contains("hfplabcapture")||l.contains("hfpclient")||l.contains("headsetclient")
            ||l.contains("telecom-")||l.contains("audioalsa")||l.contains("audiobtcvsd")||l.contains("audioflinger")
            ||l.contains("audiopolicy")||l.contains("audiosystem")||l.contains("audioservice")||l.contains("mediafocuscontrol")||l.contains("apm_")||l.contains("sco")||l.contains("btif_hf")||l.contains("hfp_enable");
    }
    @Override public void onCreate(){super.onCreate();alive=true;
        count=getSharedPreferences("capture",0).getLong("count",0);dropped=getSharedPreferences("capture",0).getLong("dropped",0);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("lab","Diagnostic HFP",NotificationManager.IMPORTANCE_LOW));
    }
    private void foreground(){Intent i=new Intent(this,ModeActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        startForeground(703,new Notification.Builder(this,"lab").setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("HfpVoipLab · capture").setContentText("Ouvrir pour suivre ou arrêter la session").setContentIntent(pi).setOngoing(true).build());
    }
    @Override public int onStartCommand(Intent i,int flags,int id){if(i==null)return START_NOT_STICKY;foreground();
        if(START.equals(i.getAction()))work.execute(this::start);
        if(SNAP.equals(i.getAction()))work.execute(()->{if(capturing)snapshot("manuel");});
        if(STOP.equals(i.getAction()))work.execute(this::stop);
        return START_NOT_STICKY;
    }
    private void start(){if(capturing)return;
        stopping=false;count=scanned=dropped=0;error="";
        state("starting","Vérification root et écriture témoin…");
        try{
            Root.check();getSharedPreferences("capture",0).edit().putString("root","accordé").apply();
            // Preserve an earlier unexported/finished session until the user explicitly starts anew.
            File archive=file("previous-session.zip");if(file("HFP-Rapport.zip").isFile())Files.copy(file("HFP-Rapport.zip").toPath(),archive.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            for(File f:Objects.requireNonNull(getFilesDir().listFiles()))if(f.getName().startsWith("events-")||f.getName().matches("(summary|errors|audio-policy|audio-flinger|audio-manager|audio-config|system-info)\\.txt")||f.getName().equals("mediatek-audio.log")||f.getName().equals("HFP-Rapport.zip"))f.delete();
            SessionStore.begin(this);save("errors.txt","");getSharedPreferences("capture",0).edit().putString("strategy","en attente du prochain appel").putString("last_mode","aucun").putString("last_call_state","—").apply();
            final String marker="CAPTURE_MARKER_"+Long.toHexString(System.nanoTime());
            final CountDownLatch firstWrite=new CountDownLatch(1);
            capturing=true;
            logProcess=new ProcessBuilder("su","-c","exec logcat -b main -b system -v threadtime -T 1").redirectErrorStream(true).start();
            logThread=new Thread(()->readLogs(marker,firstWrite),"hfp-log-reader");logThread.start();
            Root.command("log -p i -t HfpLabCapture "+marker,1000,5);
            if(!firstWrite.await(8,TimeUnit.SECONDS))throw new IOException("Écriture témoin absente : capture non confirmée");
            if(!capturing)throw new IOException("logcat interrompu au démarrage");
            state("running","Capture confirmée. Effectuer les appels puis noter les résultats.");
            try{String probe=LabProbe.read(this);SessionStore.note(this,probe);
                Matcher m=Pattern.compile("epoch=(\\d+)").matcher(probe);if(m.find())callEpoch=m.group(1);
            }catch(Exception e){fail(e.getMessage());}
            StringBuilder sys=new StringBuilder("HfpVoipLab 1.7.9\n");
            for(String cmd:new String[]{"getprop ro.build.fingerprint","getprop ro.build.version.release","getprop ro.product.device","getprop ro.board.platform","magisk -V","pidof com.android.bluetooth"}){
                try{sys.append(cmd).append('\n').append(Privacy.clean(Root.command(cmd,4000,5))).append('\n');}catch(Exception e){sys.append("Indisponible : ").append(cmd).append('\n');}
            }
            save("system-info.txt",sys.toString());save("audio-config.txt",Privacy.clean(Root.config()));snapshot("début");
        }catch(Exception e){fail("Démarrage : "+e.getMessage());capturing=false;closeReader();state("error",error);stopForeground(true);stopSelf();}
    }
    private void readLogs(String marker,CountDownLatch firstWrite){
        int part=0;long size=0,partCount=0;Map<Integer,Long> sizes=new HashMap<>();
        BufferedWriter out=null;
        try(BufferedReader in=new BufferedReader(new InputStreamReader(logProcess.getInputStream(),StandardCharsets.UTF_8))){
            out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file("events-00000.log")),StandardCharsets.UTF_8));
            String line;long lastFlush=0;
            while(capturing&&(line=in.readLine())!=null){scanned++;if(!relevant(line))continue;
                String safe=Privacy.clean(line);int length=safe.getBytes(StandardCharsets.UTF_8).length+1;
                if(size+length>6L*1024*1024){out.close();sizes.put(part,partCount);part++;partCount=0;size=0;
                    if(part>5){int old=part-5;File stale=file(String.format(Locale.ROOT,"events-%05d.log",old));if(stale.delete())dropped+=sizes.getOrDefault(old,0L);sizes.remove(old);}
                    out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file(String.format(Locale.ROOT,"events-%05d.log",part))),StandardCharsets.UTF_8));
                }
                out.write(safe);out.newLine();count++;partCount++;size+=length;
                if(line.contains(marker)){out.flush();firstWrite.countDown();}
                if(line.contains("HfpVoipFix")&&line.contains("LAB ")){
                    Matcher em=Pattern.compile("epoch=(\\d+)").matcher(line);if(em.find())callEpoch=em.group(1);
                    Matcher m=callPattern.matcher(line);
                    if(m.find()){
                        SessionStore.call(this,callEpoch+"-"+m.group(1),Integer.parseInt(m.group(2)),m.group(3),Long.parseLong(m.group(4)));
                        getSharedPreferences("capture",0).edit().putString("last_mode",m.group(3)).putString("last_call_state",m.group(2)).apply();
                    }
                    if(line.contains("STRATEGY ")||line.contains("UNAVAILABLE")||line.contains("ERROR")){
                        String result=line.contains("result=applied")?"appliquée (transport audio non vérifié)":
                            line.contains("result=observation")?"observation native":
                            line.contains("result=partial")?"partiellement appliquée — voir logs":
                            line.contains("result=pending")?"en attente": "indisponible ou anomalie — voir détails";
                        getSharedPreferences("capture",0).edit().putString("strategy",result).apply();
                        SessionStore.note(this,safe);
                    }
                    if(line.contains("ERROR")||line.contains("UNAVAILABLE")||line.contains("result=unavailable"))fail(safe);
                    if((line.contains("SCO_BEGIN")&&System.currentTimeMillis()-lastSnapshot>3000)||line.contains("COMM_REQUEST")||line.contains("COMM_RELEASE")||line.contains("SOFT_BRIDGE ROUTE_CONFIRMED")||line.contains("SOFT_BRIDGE ERROR")){lastSnapshot=System.currentTimeMillis();work.execute(()->{if(capturing)snapshot("SCO début");});}
                }
                if(System.currentTimeMillis()-lastFlush>1000){out.flush();lastFlush=System.currentTimeMillis();
                    if(firstWrite.getCount()==0&&!stopping)state("running","Capture : "+count+" événements"+(dropped>0?" · saturation : milieu tronqué":""));}
            }
            if(capturing){fail("logcat fermé de façon inattendue ; session interrompue");capturing=false;state("error",error);}
        }catch(Exception e){if(!stopping){fail("Lecture logcat : "+e.getMessage());capturing=false;state("error",error);}}
        finally{if(out!=null)try{out.close();}catch(IOException ignored){}}
    }
    private void snapshot(String stage){
        for(String[] item:new String[][]{{"audio-manager.txt","dumpsys audio"},{"audio-policy.txt","dumpsys media.audio_policy"},{"audio-flinger.txt","dumpsys media.audio_flinger"}}){
            try{append(item[0],"\n=== "+stage+" · "+System.currentTimeMillis()+" ===\n"+Privacy.clean(Root.command(item[1],450000,8)),8000000);}
            catch(Exception e){fail("Instantané "+stage+" : "+e.getMessage());}
        }
    }
    private void closeReader(){if(logProcess!=null)logProcess.destroy();if(logThread!=null)try{logThread.join(2500);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
    private void stop(){if(RxBridgeService.alive){fail("Arrêter le pont D2 avant de terminer la capture.");return;}stopping=true;state("processing","Finalisation du rapport…");capturing=false;closeReader();
        if(logThread!=null&&logThread.isAlive()){fail("Lecteur non arrêté : export différé");state("error",error);stopForeground(true);return;}
        snapshot("fin");
        try{
            SessionStore.finish(this);
            save("summary.txt","HfpVoipLab 1.7.9\nÉvénements écrits : "+count+"\nLignes examinées : "+scanned+
                "\nÉvénements retirés du milieu par rotation : "+dropped+"\nConservation : début (6 Mio) + cinq derniers segments (30 Mio).\n"+
                "Les horodatages CALL et SCO servent au recoupement ; status=0 ne prouve pas un transport PCM.\n"+
                "WBS reflète la négociation rapportée par Android, pas une mesure du codec sur le fil.\n"+
                "Les événements UNAVAILABLE signalent une stratégie non appliquée.\nDernière anomalie : "+error+
                "\nMasquage ciblé : horodatages/PID/valeurs audio conservés. Numéros sans libellé susceptibles de rester visibles ; vérifier avant partage.\nLes appels commencés avant capture peuvent être incomplets.\n");
            for(String n:ReportArchive.REQUIRED)if(!file(n).exists())save(n,"Indisponible : donnée non capturée.\n");
            ReportArchive.build(getFilesDir());
            state("ready","Rapport prêt : "+count+" événements"+(dropped>0?" · ATTENTION : saturation":""));
        }catch(Exception e){fail("Export : "+e.getMessage());state("error",error);}
        stopForeground(true);stopSelf();
    }
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onDestroy(){alive=false;capturing=false;closeReader();work.shutdown();super.onDestroy();}
}
