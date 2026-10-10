package com.hfpvoipfix;
import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.util.Log;
import java.io.*;
import java.util.*;

/** Experimental one-way SCO bridge, explicitly armed per call; never stores PCM. */
public final class RxBridgeService extends Service {
    public static volatile boolean alive;
    private volatile boolean stop, permitted;
    private volatile long received;
    private volatile String probe="",fatal="";
    private Thread worker,watch;
    private static final String TAG="HfpVoipFix";
    private void status(String s){getSharedPreferences("rx",0).edit().putString("status",s).apply();}
    private void event(String s){Log.i(TAG,"LAB SOFT_BRIDGE "+s);}
    @Override public void onCreate(){super.onCreate();alive=true;getSharedPreferences("rx",0).edit().putBoolean("armed",false).apply();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("rx","Pont audio expérimental",NotificationManager.IMPORTANCE_LOW));}
    @Override public int onStartCommand(Intent i,int flags,int id){
        PendingIntent cancel=PendingIntent.getService(this,0,new Intent(this,RxBridgeService.class).setAction("stop"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String mode=i==null?"":i.getStringExtra("mode");boolean tx="soft_tx".equals(mode);
        startForeground(705,new Notification.Builder(this,"rx").setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(tx?"HFP · essai micro vers Samsung":"HFP · réception vers Redmi")
            .setContentText("Arrêt automatique après cet appel").addAction(new Notification.Action.Builder(null,"Arrêter",cancel).build()).setOngoing(true).build());
        if(i==null||"stop".equals(i.getAction())){stop=true;permitted=false;if(worker==null)stopSelf();return START_NOT_STICKY;}
        if(worker==null){worker=new Thread(this::runBridge,"hfp-software-sco-bridge");worker.start();}return START_NOT_STICKY;
    }
    private boolean allowed(){return !stop&&permitted&&RxGate.fresh(SystemClock.elapsedRealtime(),received);}
    private void watch(){boolean first=true;String activeIdentity=null,mode=null;long end=SystemClock.elapsedRealtime()+600000;
        ProbeChannel channel=null;int failures=0;
        try{while(!stop){String p;
            try{
                if(channel==null)channel=new ProbeChannel();
                long began=SystemClock.elapsedRealtime();p=channel.read();
                event("PROBE_OK latency_ms="+(SystemClock.elapsedRealtime()-began)+" recovered="+failures);
                failures=0;
            }catch(Exception e){
                if(channel!=null){channel.close();channel=null;}failures++;getSharedPreferences("rx",0).edit().putBoolean("armed",false).apply();
                event("PROBE_RETRY count="+failures+" detail="+Privacy.clean(String.valueOf(e.getMessage())));
                if(activeIdentity!=null||SystemClock.elapsedRealtime()>end)throw e;
                status("D2 en attente : suivi Bluetooth indisponible, nouvelle tentative. Ne pas appeler avant D2 armé.");Thread.sleep(1000);continue;
            }
            probe=p;received=SystemClock.elapsedRealtime();
            getSharedPreferences("capture",0).edit().putString("probe",p).putLong("probe_at",System.currentTimeMillis()).apply();
            Map<String,String> values=RxGate.parse(p);
            if(!"1.7.7".equals(values.get("version")))throw new IOException("Module 1.7.7 non chargé : redémarrage LSPosed nécessaire.");
            if(first){first=false;mode=values.get("requested");if(!"false".equals(values.get("busy"))||!LabModes.softwareBridge(mode))throw new IOException("Armer hors appel après avoir choisi D2 ou D3.");event("ARM_CONFIRMED direction="+mode);}
            if(activeIdentity==null){
                if(RxGate.ready(p)){getSharedPreferences("rx",0).edit().putBoolean("armed",false).apply();activeIdentity=RxGate.identity(p);permitted=true;event("HANDSHAKE "+p);}
                else if("true".equals(values.get("busy"))&&!mode.equals(values.get("applied")))throw new IOException("L'appel n'utilise pas le mode armé.");
                else if(SystemClock.elapsedRealtime()>end)throw new IOException("Armement expiré après 10 minutes.");
                else {getSharedPreferences("rx",0).edit().putBoolean("armed",true).apply();status((mode.equals("soft_tx")?"D3 micro armé":"D2 réception armée")+" : appeler depuis le Redmi. Attente restante : "+((end-SystemClock.elapsedRealtime())/1000)+" s.");}
            }else if(!RxGate.ready(p)||!activeIdentity.equals(RxGate.identity(p))){event("STOP reason=SCO_or_session_ended");stop=true;permitted=false;}
            Thread.sleep(250);
        }}catch(Exception e){fatal=String.valueOf(e.getMessage());stop=true;permitted=false;event("ERROR handshake="+e.getClass().getSimpleName()+" detail="+Privacy.clean(fatal));}
        finally{if(channel!=null)channel.close();}
    }
    private AudioDeviceInfo unique(AudioManager am,int flags,int type)throws IOException{
        AudioDeviceInfo result=null;for(AudioDeviceInfo d:am.getDevices(flags))if(d.getType()==type){if(result!=null)throw new IOException("Plusieurs périphériques audio candidats : essai refusé.");result=d;}
        if(result==null)throw new IOException("Périphérique absent : type="+type);return result;
    }
    private boolean routed(AudioRecord record,AudioTrack track,int input,int output,int inputType,int outputType){AudioDeviceInfo a=record.getRoutedDevice(),b=track.getRoutedDevice();return a!=null&&b!=null&&a.getId()==input&&a.getType()==inputType&&b.getId()==output&&b.getType()==outputType;}
    private void runBridge(){AudioRecord record=null;AudioTrack track=null;
        try{
            if(!CaptureService.alive||!getSharedPreferences("capture",0).getString("phase","").equals("running"))throw new IOException("Démarrer la capture avant d'armer le pont.");
            String requested=LabModes.valid(Root.command("getprop "+LabModes.PROP,1000,4).trim());boolean tx=requested.equals("soft_tx");
            if(!LabModes.softwareBridge(requested))throw new IOException("Choisir D2 réception ou D3 micro et appliquer le mode avant l'armement.");
            status("Vérification hors appel… attendre le message "+(tx?"D3 micro armé":"D2 réception armée")+".");event("ARM_REQUEST direction="+(tx?"tx":"rx")+" no_audio_saved");
            watch=new Thread(this::watch,"hfp-sco-handshake");watch.start();
            while(!stop&&!allowed())Thread.sleep(40);
            if(stop)return;
            int rate=Integer.parseInt(RxGate.parse(probe).get("rate"));
            AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);
            if(am.getMode()==AudioManager.MODE_IN_CALL)throw new IOException("Appel téléphonique local actif : essai refusé.");
            int inputType=tx?AudioDeviceInfo.TYPE_BUILTIN_MIC:AudioDeviceInfo.TYPE_BLUETOOTH_SCO;
            int outputType=tx?AudioDeviceInfo.TYPE_BLUETOOTH_SCO:AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;
            AudioDeviceInfo input=unique(am,AudioManager.GET_DEVICES_INPUTS,inputType),output=unique(am,AudioManager.GET_DEVICES_OUTPUTS,outputType);
            int inMin=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT),outMin=AudioTrack.getMinBufferSize(rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
            if(inMin<=0||outMin<=0)throw new IOException("Format audio non pris en charge à "+rate+" Hz");
            record=new AudioRecord.Builder().setAudioSource(tx?MediaRecorder.AudioSource.MIC:MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build()).setBufferSizeInBytes(Math.max(inMin*2,rate/5)).build();
            track=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(tx?AudioAttributes.USAGE_VOICE_COMMUNICATION:AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(Math.max(outMin*2,rate/5)).setTransferMode(AudioTrack.MODE_STREAM).build();
            if(record.getState()!=AudioRecord.STATE_INITIALIZED||track.getState()!=AudioTrack.STATE_INITIALIZED)throw new IOException("Initialisation AudioRecord/AudioTrack refusée.");
            if(!record.setPreferredDevice(input)||!track.setPreferredDevice(output))throw new IOException("Périphérique préféré refusé.");
            track.setVolume(tx?0.8f:0.35f);record.startRecording();track.play();
            short[] samples=new short[rate/50],silence=new short[rate/50];
            long opened=SystemClock.elapsedRealtime(),last=opened,lastData=opened,read=0,written=0,dropped=0,nonzero=0,energy=0,window=0;int peak=0;boolean confirmed=false;
            event("OPEN direction="+(tx?"tx":"rx")+" rate="+rate+" input_id="+input.getId()+" output_id="+output.getId()+" gain="+(tx?"0.8":"0.35")+" no_audio_saved");
            while(allowed()){
                long now=SystemClock.elapsedRealtime();if(now-opened>180000)throw new IOException("Limite de sécurité : 3 minutes de réception.");
                if(am.getMode()==AudioManager.MODE_IN_CALL)throw new IOException("Un appel local a pris le contrôle audio.");
                boolean route=routed(record,track,input.getId(),output.getId(),inputType,outputType);
                if(confirmed&&!route)throw new IOException("Route changée : arrêt sans restitution des données.");
                if(!confirmed){if(!tx)track.write(silence,0,silence.length,AudioTrack.WRITE_NON_BLOCKING);if(route){confirmed=true;event(tx?"ROUTE_CONFIRMED input=MIC output=SCO":"ROUTE_CONFIRMED input=SCO output=SPEAKER");}else if(now-opened>2500)throw new IOException(tx?"Route microphone → SCO non confirmée.":"Route SCO → haut-parleur non confirmée.");}
                int n=record.read(samples,0,samples.length,AudioRecord.READ_NON_BLOCKING);if(n<0)throw new IOException("AudioRecord.read="+n);
                if(n>0){lastData=now;read+=n;
                    if(!confirmed||!allowed()||!routed(record,track,input.getId(),output.getId(),inputType,outputType)){dropped+=n;Arrays.fill(samples,(short)0);continue;}
                    for(int k=0;k<n;k++){int v=samples[k];if(v!=0)nonzero++;energy+=(long)v*v;peak=Math.max(peak,Math.abs(v));}window+=n;
                    int done=track.write(samples,0,n,AudioTrack.WRITE_NON_BLOCKING);if(done<0)throw new IOException("AudioTrack.write="+done);written+=done;dropped+=n-done;
                }
                if(now-last>=1000){AudioRecordingConfiguration configuration=record.getActiveRecordingConfiguration();
                    if(configuration!=null&&configuration.isClientSilenced())throw new IOException("Capture rendue silencieuse par Android (concurrence ou confidentialité).");
                    long rms=window==0?0:(long)Math.sqrt((double)energy/window);event("STATS direction="+(tx?"tx":"rx")+" samples_read="+read+" samples_written="+written+" samples_dropped="+dropped+" nonzero="+nonzero+" rms="+rms+" peak="+peak+" route="+confirmed+" underruns="+track.getUnderrunCount());status((tx?"D3 micro":"D2 réception")+" : "+read+" échantillons lus · "+written+" écrits · niveau "+rms+" · route "+(confirmed?"confirmée":"en attente"));last=now;energy=window=0;peak=0;}
                if(now-lastData>8000)throw new IOException("Aucun échantillon reçu depuis 8 secondes.");
                if(n==0)Thread.sleep(10);
            }
            if(!stop&&!RxGate.fresh(SystemClock.elapsedRealtime(),received))throw new IOException("État Bluetooth périmé : pont arrêté.");
        }catch(Exception e){fatal=String.valueOf(e.getMessage());event("ERROR "+e.getClass().getSimpleName()+" "+fatal);}
        finally{stop=true;permitted=false;
            if(record!=null){try{record.stop();}catch(Exception ignored){}record.release();}
            if(track!=null){try{track.pause();track.flush();}catch(Exception ignored){}track.release();}
            event("CLOSED "+(fatal.isEmpty()?"normal":"error"));status(fatal.isEmpty()?"Pont arrêté. Noter RX et TX puis exporter.":"Pont arrêté : "+fatal);
            stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
        }
    }
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onDestroy(){stop=true;permitted=false;alive=false;getSharedPreferences("rx",0).edit().putBoolean("armed",false).apply();super.onDestroy();}
}
