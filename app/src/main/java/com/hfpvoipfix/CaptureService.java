package com.hfpvoipfix;

import android.app.*;
import android.content.*;
import android.media.*;
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
    private Thread logThread,autoThread;
    private CallDiagnostics callDiagnostics;
    private volatile boolean capturing,stopping;
    private volatile long count,scanned,dropped;
    private volatile String error="";
    private long lastSnapshot;
    private String callEpoch="unknown";
    private final Pattern callPattern=Pattern.compile("CALL id=(\\d+) state=(\\d+) mode=(\\w+) ms=(\\d+)");
    private File file(String n){return new File(getFilesDir(),n);}
    private void state(String phase,String msg){getSharedPreferences("capture",0).edit().putString("phase",phase).putString("status",msg)
        .putLong("count",count).putLong("scanned",scanned).putLong("dropped",dropped).putString("error",error).putLong("heartbeat",System.currentTimeMillis()).apply();}
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
        return l.contains("hfpvoipfix")||l.contains("hfplabcapture")||l.contains("hfpclient")||l.contains("headsetclient")||l.contains("headsetservice")||l.contains("headsetstatemachine")
            ||l.contains("telecom-")||l.contains("callsmanager")||l.contains("newoutgoingcallintentbroadcaster")||l.contains("hfpclientconnservice")||l.contains("audioalsa")||l.contains("audiobtcvsd")||l.contains("audioflinger")
            ||l.contains("audiopolicy")||l.contains("audiosystem")||l.contains("audioservice")||l.contains("mediafocuscontrol")||l.contains("apm_")||l.contains("sco")||l.contains("btif_hf")||l.contains("hfp_enable");
    }
    @Override public void onCreate(){super.onCreate();alive=true;
        count=getSharedPreferences("capture",0).getLong("count",0);dropped=getSharedPreferences("capture",0).getLong("dropped",0);
        scanned=getSharedPreferences("capture",0).getLong("scanned",0);error=getSharedPreferences("capture",0).getString("error","");
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
        stopping=false;count=scanned=dropped=0;error="";getSharedPreferences("rx",0).edit().putBoolean("auto_paused",false).apply();
        state("starting","Vérification root et écriture témoin…");
        try{
            Root.check();getSharedPreferences("capture",0).edit().putString("root","accordé").apply();
            // Preserve an earlier unexported/finished session until the user explicitly starts anew.
            File archive=file("previous-session.zip");if(file("HFP-Rapport.zip").isFile())Files.copy(file("HFP-Rapport.zip").toPath(),archive.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            CaptureFiles.clearSession(getFilesDir());
            save("system-info.txt","Non collecté pour cette session.\n");save("audio-config.txt","Non collecté pour cette session.\n");
            save("capture-state.txt","started_at_ms="+System.currentTimeMillis()+"\nlogcat_confirmed=false\n");
            SessionStore.begin(this);save("errors.txt","");getSharedPreferences("capture",0).edit().putString("strategy","en attente du prochain appel").putString("last_mode","aucun").putString("last_call_state","—").apply();
            final String marker="CAPTURE_MARKER_"+Long.toHexString(System.nanoTime());
            final CountDownLatch firstWrite=new CountDownLatch(1);
            capturing=true;
            logProcess=new ProcessBuilder("su","-c","exec logcat -b main -b system -v threadtime -T 1 HfpVoipFix:I HfpLabCapture:I Telecom:D TelecomFramework:D HeadsetClientService:D HfpClientConnService:D HeadsetService:D HeadsetStateMachine:D '*:S'").redirectErrorStream(true).start();
            logThread=new Thread(()->readLogs(marker,firstWrite),"hfp-log-reader");logThread.start();
            // su/logcat startup can lag behind the first marker. Re-emit until the
            // reader confirms it; -T 1 alone cannot guarantee that first write survives.
            long deadline=SystemClock.elapsedRealtime()+12000;
            while(capturing&&firstWrite.getCount()>0&&SystemClock.elapsedRealtime()<deadline){
                Root.command("log -p i -t HfpLabCapture "+marker,1000,3);
                firstWrite.await(500,TimeUnit.MILLISECONDS);
            }
            if(firstWrite.getCount()>0)throw new IOException("Écriture témoin absente : capture non confirmée");
            if(!capturing)throw new IOException("logcat interrompu au démarrage");
            save("capture-state.txt","confirmed_at_ms="+System.currentTimeMillis()+"\nlogcat_confirmed=true\n");
            callDiagnostics=new CallDiagnostics(getFilesDir());callDiagnostics.start();
            state("running","Capture confirmée. Préparation automatique du pont hors appel.");
            autoThread=new Thread(this::autoBridge,"hfp-auto-bridge");autoThread.start();
            try{String probe=LabProbe.read(this);SessionStore.note(this,probe);
                Matcher m=Pattern.compile("epoch=(\\d+)").matcher(probe);if(m.find())callEpoch=m.group(1);
            }catch(Exception e){fail(e.getMessage());}
            StringBuilder sys=new StringBuilder("HfpVoipLab 1.9.14\n");
            for(String cmd:new String[]{"getprop ro.build.fingerprint","getprop ro.build.version.release","getprop ro.product.device","getprop ro.board.platform","magisk -V","pidof com.android.bluetooth","getprop debug.hfpvoipfix.native_split","sha256sum /vendor/lib/hw/audio.primary.mt6785.so","sha256sum /vendor/etc/audio_policy_configuration.xml"}){
                try{sys.append(cmd).append('\n').append(Privacy.clean(Root.command(cmd,4000,5))).append('\n');}catch(Exception e){sys.append("Indisponible : ").append(cmd).append('\n');}
            }
            try{save("telecom-hook.txt",Privacy.clean(Root.command("grep -R -i -E 'HfpVoipFix|TELECOM_AUDIO|TELECOM_LOADER|com.android.server.telecom' /data/adb/lspd/log 2>/dev/null",200000,5)));}catch(Exception e){save("telecom-hook.txt","Diagnostic de chargement indisponible : "+e.getClass().getSimpleName());}
            save("system-info.txt",sys.toString());save("audio-config.txt",Privacy.clean(Root.config()));if(!RxBridgeService.alive)snapshot("début");
        }catch(Exception e){stopping=true;fail("Démarrage : "+e.getMessage());capturing=false;closeReader();closeCallDiagnostics();state("error",error);stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
    }
    private void autoBridge(){
        while(capturing&&!stopping){
            try{
                if(!RelayService.enabled(this)&&!RxBridgeService.alive&&!getSharedPreferences("rx",0).getBoolean("auto_paused",false)&&checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED){
                    String p=LabProbe.read(this);
                    String mode=Root.command("getprop "+LabModes.PROP,1000,4).trim();
                    if(AutoBridgeGate.prepare(p,mode,System.currentTimeMillis()-getSharedPreferences("capture",0).getLong("probe_at",0),capturing&&!stopping&&!getSharedPreferences("rx",0).getBoolean("auto_paused",false),RxBridgeService.alive,true)){
                        BridgeJournal.write(getFilesDir(),"AUTO_PREPARE mode="+mode);
                        startForegroundService(new Intent(this,RxBridgeService.class).putExtra("mode",mode).putExtra("automatic",true));
                    }
                }
                Thread.sleep(2000);
            }catch(InterruptedException e){Thread.currentThread().interrupt();return;}
            catch(Exception e){BridgeJournal.write(getFilesDir(),"AUTO_WAIT detail="+Privacy.clean(String.valueOf(e.getMessage())));try{Thread.sleep(2000);}catch(InterruptedException interrupted){return;}}
        }
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
                    // Heavy AudioPolicy/AudioFlinger dumps are manual or outside calls only.
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
        try{append("telecom-state.txt","\n=== "+stage+" · "+System.currentTimeMillis()+" ===\n"+Privacy.clean(Root.command("dumpsys telecom",500000,12)),2000000);}
        catch(Exception e){fail("Instantané Telecom "+stage+" : "+e.getMessage());}
        try{append("projection-state.txt","\n=== "+stage+" · "+System.currentTimeMillis()+" ===\n"+projectionState(),500000);}
        catch(Exception e){fail("Instantané projection "+stage+" : "+e.getMessage());}
        try{append("bluetooth-state.txt","\n=== "+stage+" · "+System.currentTimeMillis()+" ===\n"+Privacy.clean(Root.command("dumpsys bluetooth_manager",450000,12)),2000000);}
        catch(Exception e){fail("Instantané Bluetooth "+stage+" : "+e.getMessage());}
    }
    private String projectionState()throws Exception{
        AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);StringBuilder out=new StringBuilder();
        String pid=Root.command("pidof com.google.android.projection.gearhead || true",100,3).trim();out.append("gearhead_active=").append(!pid.isEmpty()).append('\n');
        for(int flags:new int[]{AudioManager.GET_DEVICES_OUTPUTS,AudioManager.GET_DEVICES_INPUTS})for(AudioDeviceInfo d:am.getDevices(flags))
            out.append(flags==AudioManager.GET_DEVICES_OUTPUTS?"output":"input").append(" id=").append(d.getId()).append(" type=").append(d.getType())
                .append(" address=").append(Privacy.clean(d.getAddress())).append(" name=").append(Privacy.clean(String.valueOf(d.getProductName())))
                .append(" rates=").append(Arrays.toString(d.getSampleRates())).append('\n');
        return out.toString();
    }
    private void closeReader(){if(logProcess!=null)logProcess.destroy();if(logThread!=null)try{logThread.join(2500);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
    private boolean closeCallDiagnostics(){if(callDiagnostics==null)return true;
        try{callDiagnostics.close();return true;}catch(Exception e){fail("Arrêt diagnostic AA : "+e.getMessage());return false;}}
    private void stop(){stopping=true;getSharedPreferences("rx",0).edit().putBoolean("auto_paused",true).apply();
        if(RxBridgeService.alive&&!RxBridgeService.autonomousActive){startService(new Intent(this,RxBridgeService.class).setAction("stop"));long until=SystemClock.elapsedRealtime()+6000;
            while(RxBridgeService.alive&&SystemClock.elapsedRealtime()<until)try{Thread.sleep(50);}catch(InterruptedException e){Thread.currentThread().interrupt();return;}
            if(RxBridgeService.alive){fail("Pont audio toujours en fermeture. Réessayer de terminer après son arrêt.");return;}
        }state("processing","Finalisation du rapport…");capturing=false;closeReader();
        if(logThread!=null&&logThread.isAlive()){fail("Lecteur non arrêté : export différé");state("error",error);stopForeground(true);return;}
        if(!closeCallDiagnostics()){state("error",error);return;}
        snapshot("fin");
        try{
            SessionStore.finish(this);
            save("summary.txt","HfpVoipLab 1.9.14\nÉvénements écrits : "+count+"\nLignes examinées : "+scanned+
                "\nÉvénements retirés du milieu par rotation : "+dropped+"\nConservation : début (6 Mio) + cinq derniers segments (30 Mio).\n"+
                "Les horodatages CALL et SCO servent au recoupement ; status=0 ne prouve pas un transport PCM.\n"+
                "WBS reflète la négociation rapportée par Android, pas une mesure du codec sur le fil.\n"+
                "Les événements UNAVAILABLE signalent une stratégie non appliquée.\nDernière anomalie : "+error+
                "\nDiagnostic AA : aa-events-0.log conserve le début ; aa-events-1/2.log conservent la fin, à trier par SEGMENT_STARTED. Limite 3 Mio. État et pertes dans aa-diagnostic-state.txt.\n"+
                "Android Auto peut refuser sans publier de journal ; absence de ligne ne prouve pas absence de demande.\n"+
                "\nMasquage ciblé : horodatages/PID/valeurs audio conservés. Numéros sans libellé susceptibles de rester visibles ; vérifier avant partage.\nLes appels commencés avant capture peuvent être incomplets.\n");
            for(String n:ReportArchive.REQUIRED)if(!file(n).exists())save(n,"Indisponible : donnée non capturée.\n");
            ReportArchive.build(getFilesDir());
            state("ready","Rapport prêt : "+count+" événements"+(dropped>0?" · ATTENTION : saturation":""));
        }catch(Exception e){fail("Export : "+e.getMessage());state("error",error);}
        stopForeground(true);stopSelf();
    }
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onDestroy(){alive=false;capturing=false;if(autoThread!=null)autoThread.interrupt();closeReader();closeCallDiagnostics();work.shutdown();super.onDestroy();}
}
