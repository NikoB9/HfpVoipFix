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
        String title=tx?"HFP · essai micro vers Samsung":"soft_duplex".equals(mode)?"HFP · réception + micro duplex":"HFP · réception vers Redmi";
        startForeground(705,new Notification.Builder(this,"rx").setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(title)
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
                status(("soft_tx".equals(mode)?"D3":"soft_duplex".equals(mode)?"D4":"D2")+" en attente : suivi Bluetooth indisponible, nouvelle tentative.");Thread.sleep(1000);continue;
            }
            probe=p;received=SystemClock.elapsedRealtime();
            getSharedPreferences("capture",0).edit().putString("probe",p).putLong("probe_at",System.currentTimeMillis()).apply();
            Map<String,String> values=RxGate.parse(p);
            if(!"1.7.12".equals(values.get("version")))throw new IOException("Module 1.7.12 non chargé : redémarrage LSPosed nécessaire.");
            if(first){first=false;mode=values.get("requested");if(!"false".equals(values.get("busy"))||!LabModes.softwareBridge(mode))throw new IOException("Armer hors appel après avoir choisi D2 ou D3.");event("ARM_CONFIRMED direction="+mode);}
            if(activeIdentity==null){
                if(RxGate.ready(p)){getSharedPreferences("rx",0).edit().putBoolean("armed",false).apply();activeIdentity=RxGate.identity(p);permitted=true;event("HANDSHAKE "+p);}
                else if("true".equals(values.get("busy"))&&!mode.equals(values.get("applied")))throw new IOException("L'appel n'utilise pas le mode armé.");
                else if(SystemClock.elapsedRealtime()>end)throw new IOException("Armement expiré après 10 minutes.");
                else {getSharedPreferences("rx",0).edit().putBoolean("armed",true).apply();status((mode.equals("soft_tx")?"D3 micro armé":mode.equals("soft_duplex")?"D4 duplex armé":"D2 réception armée")+" : appeler depuis le Redmi. Attente restante : "+((end-SystemClock.elapsedRealtime())/1000)+" s.");}
            }else if(!RxGate.ready(p)||!activeIdentity.equals(RxGate.identity(p))){event("STOP reason=SCO_or_session_ended");stop=true;permitted=false;}
            Thread.sleep(250);
        }}catch(Exception e){fatal=String.valueOf(e.getMessage());stop=true;permitted=false;event("ERROR handshake="+e.getClass().getSimpleName()+" detail="+Privacy.clean(fatal));}
        finally{if(channel!=null)channel.close();}
    }
    private AudioDeviceInfo unique(AudioManager am,int flags,int type)throws IOException{
        AudioDeviceInfo result=null;for(AudioDeviceInfo d:am.getDevices(flags))if(d.getType()==type){if(result!=null)throw new IOException("Plusieurs périphériques audio candidats : essai refusé.");result=d;}
        if(result==null)throw new IOException("Périphérique absent : type="+type);return result;
    }
    private void logMicCandidates(AudioManager am){
        for(AudioDeviceInfo d:am.getDevices(AudioManager.GET_DEVICES_INPUTS))if(d.getType()==AudioDeviceInfo.TYPE_BUILTIN_MIC)
            event("MIC_CANDIDATE id="+d.getId()+" address="+Privacy.clean(d.getAddress())+" name="+Privacy.clean(String.valueOf(d.getProductName())));
    }
    private boolean routed(AudioRecord record,AudioTrack track,int input,int output,int inputType,int outputType){AudioDeviceInfo a=record.getRoutedDevice(),b=track.getRoutedDevice();return a!=null&&b!=null&&(input<0||a.getId()==input)&&a.getType()==inputType&&b.getId()==output&&b.getType()==outputType;}
    private void runBridge(){AudioRecord record=null;AudioTrack track=null;
        try{
            if(!CaptureService.alive||!getSharedPreferences("capture",0).getString("phase","").equals("running"))throw new IOException("Démarrer la capture avant d'armer le pont.");
            String requested=LabModes.valid(Root.command("getprop "+LabModes.PROP,1000,4).trim());
            if(!LabModes.softwareBridge(requested))throw new IOException("Choisir D2, D3 ou D4 et appliquer le mode avant l'armement.");
            if(requested.equals("soft_duplex")){runDuplexBridge();return;}
            boolean tx=requested.equals("soft_tx");
            status("Vérification hors appel… attendre le message "+(tx?"D3 micro armé":"D2 réception armée")+".");event("ARM_REQUEST direction="+(tx?"tx":"rx")+" no_audio_saved");
            watch=new Thread(this::watch,"hfp-sco-handshake");watch.start();
            while(!stop&&!allowed())Thread.sleep(40);
            if(stop)return;
            int rate=Integer.parseInt(RxGate.parse(probe).get("rate"));
            AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);
            if(am.getMode()==AudioManager.MODE_IN_CALL)throw new IOException("Appel téléphonique local actif : essai refusé.");
            int inputType=tx?AudioDeviceInfo.TYPE_BUILTIN_MIC:AudioDeviceInfo.TYPE_BLUETOOTH_SCO;
            int outputType=tx?AudioDeviceInfo.TYPE_BLUETOOTH_SCO:AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;
            // For TX, let Android's MIC source choose the policy's default microphone.
            // Some MIUI builds expose the front and rear mics as multiple devices of the
            // same public type; choosing one arbitrarily can reject a valid route.
            AudioDeviceInfo input=null;
            if(tx)logMicCandidates(am);else input=unique(am,AudioManager.GET_DEVICES_INPUTS,inputType);
            AudioDeviceInfo output=unique(am,AudioManager.GET_DEVICES_OUTPUTS,outputType);
            int inMin=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT),outMin=AudioTrack.getMinBufferSize(rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
            if(inMin<=0||outMin<=0)throw new IOException("Format audio non pris en charge à "+rate+" Hz");
            record=new AudioRecord.Builder().setAudioSource(tx?MediaRecorder.AudioSource.MIC:MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build()).setBufferSizeInBytes(Math.max(inMin*2,rate/5)).build();
            track=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(tx?AudioAttributes.USAGE_VOICE_COMMUNICATION:AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(Math.max(outMin*2,rate/5)).setTransferMode(AudioTrack.MODE_STREAM).build();
            if(record.getState()!=AudioRecord.STATE_INITIALIZED||track.getState()!=AudioTrack.STATE_INITIALIZED)throw new IOException("Initialisation AudioRecord/AudioTrack refusée.");
            if((input!=null&&!record.setPreferredDevice(input))||!track.setPreferredDevice(output))throw new IOException("Périphérique préféré refusé.");
            track.setVolume(tx?0.8f:0.35f);record.startRecording();track.play();
            short[] samples=new short[rate/50],silence=new short[rate/50];
            long opened=SystemClock.elapsedRealtime(),last=opened,lastData=opened,read=0,written=0,dropped=0,nonzero=0,energy=0,window=0;int peak=0,inputId=input==null?-1:input.getId();boolean confirmed=false;
            event("OPEN direction="+(tx?"tx":"rx")+" rate="+rate+" input_id="+(inputId<0?"system_default":inputId)+" output_id="+output.getId()+" gain="+(tx?"0.8":"0.35")+" no_audio_saved");
            while(allowed()){
                long now=SystemClock.elapsedRealtime();if(now-opened>180000)throw new IOException("Limite de sécurité : 3 minutes de réception.");
                if(am.getMode()==AudioManager.MODE_IN_CALL)throw new IOException("Un appel local a pris le contrôle audio.");
                // Snapshot each route once. MIUI can return null between calls while the
                // SCO route is being rebuilt; do not dereference a second, fresh lookup.
                AudioDeviceInfo routedInput=record.getRoutedDevice(),routedOutput=track.getRoutedDevice();
                boolean route=routedInput!=null&&routedOutput!=null&&(inputId<0||routedInput.getId()==inputId)&&routedInput.getType()==inputType&&routedOutput.getId()==output.getId()&&routedOutput.getType()==outputType;
                if(confirmed&&!route)throw new IOException("Route changée : arrêt sans restitution des données.");
                if(!confirmed){if(!tx)track.write(silence,0,silence.length,AudioTrack.WRITE_NON_BLOCKING);if(route){confirmed=true;if(inputId<0){inputId=routedInput.getId();event("MIC_SELECTED id="+inputId+" address="+Privacy.clean(routedInput.getAddress())+" name="+Privacy.clean(String.valueOf(routedInput.getProductName())));}event(tx?"ROUTE_CONFIRMED input=MIC output=SCO":"ROUTE_CONFIRMED input=SCO output=SPEAKER");}else if(now-opened>2500)throw new IOException(tx?"Route microphone → SCO non confirmée.":"Route SCO → haut-parleur non confirmée.");}
                int n=record.read(samples,0,samples.length,AudioRecord.READ_NON_BLOCKING);if(n<0)throw new IOException("AudioRecord.read="+n);
                if(n>0){lastData=now;read+=n;
                    if(!confirmed||!allowed()||!routed(record,track,inputId,output.getId(),inputType,outputType)){dropped+=n;Arrays.fill(samples,(short)0);continue;}
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
    private void runDuplexBridge()throws Exception{
        if(!CaptureService.alive||!getSharedPreferences("capture",0).getString("phase","").equals("running"))throw new IOException("Démarrer la capture avant d’armer le pont.");
        if(!LabModes.valid(Root.command("getprop "+LabModes.PROP,1000,4).trim()).equals("soft_duplex"))throw new IOException("Le mode D4 n’est plus sélectionné.");
        status("Vérification hors appel… attendre « D4 duplex armé ».");event("ARM_REQUEST direction=duplex no_audio_saved");
        watch=new Thread(this::watch,"hfp-sco-handshake");watch.start();
        while(!stop&&!allowed())Thread.sleep(40);if(stop)return;
        int rate=Integer.parseInt(RxGate.parse(probe).get("rate"));AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);
        if(am.getMode()==AudioManager.MODE_IN_CALL)throw new IOException("Appel téléphonique local actif : essai refusé.");
        AudioDeviceInfo scoIn=unique(am,AudioManager.GET_DEVICES_INPUTS,AudioDeviceInfo.TYPE_BLUETOOTH_SCO);
        AudioDeviceInfo speaker=unique(am,AudioManager.GET_DEVICES_OUTPUTS,AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
        AudioDeviceInfo scoOut=unique(am,AudioManager.GET_DEVICES_OUTPUTS,AudioDeviceInfo.TYPE_BLUETOOTH_SCO);
        logMicCandidates(am);
        int inMin=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT),outMin=AudioTrack.getMinBufferSize(rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
        if(inMin<=0||outMin<=0)throw new IOException("Format audio duplex non pris en charge à "+rate+" Hz");
        AudioFormat inFormat=new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build();
        AudioFormat outFormat=new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
        AudioRecord rxRecord=null,txRecord=null;AudioTrack rxTrack=null,txTrack=null;
        try{
            rxRecord=new AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION).setAudioFormat(inFormat).setBufferSizeInBytes(Math.max(inMin*2,rate/5)).build();
            txRecord=new AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.MIC).setAudioFormat(inFormat).setBufferSizeInBytes(Math.max(inMin*2,rate/5)).build();
            rxTrack=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setAudioFormat(outFormat).setBufferSizeInBytes(Math.max(outMin*2,rate/5)).setTransferMode(AudioTrack.MODE_STREAM).build();
            txTrack=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setAudioFormat(outFormat).setBufferSizeInBytes(Math.max(outMin*2,rate/5)).setTransferMode(AudioTrack.MODE_STREAM).build();
            if(rxRecord.getState()!=AudioRecord.STATE_INITIALIZED||txRecord.getState()!=AudioRecord.STATE_INITIALIZED||rxTrack.getState()!=AudioTrack.STATE_INITIALIZED||txTrack.getState()!=AudioTrack.STATE_INITIALIZED)throw new IOException("Initialisation d’un flux audio duplex refusée.");
            if(!rxRecord.setPreferredDevice(scoIn)||!rxTrack.setPreferredDevice(speaker)||!txTrack.setPreferredDevice(scoOut))throw new IOException("Routage d’un flux audio duplex refusé.");
            rxTrack.setVolume(0.35f);txTrack.setVolume(0.8f);rxRecord.startRecording();txRecord.startRecording();rxTrack.play();txTrack.play();
            short[] rxSamples=new short[rate/50],txSamples=new short[rate/50];
            long opened=SystemClock.elapsedRealtime(),lastStats=opened,lastRxData=opened,lastTxData=opened;
            long rxRead=0,rxWritten=0,rxDropped=0,rxNonzero=0,rxEnergy=0,rxWindow=0,txRead=0,txWritten=0,txDropped=0,txNonzero=0,txEnergy=0,txWindow=0;
            int rxPeak=0,txPeak=0,txInputId=-1;boolean rxConfirmed=false,txConfirmed=false;
            event("OPEN direction=duplex rate="+rate+" rx_input_id="+scoIn.getId()+" rx_output_id="+speaker.getId()+" tx_input_id=system_default tx_output_id="+scoOut.getId()+" no_audio_saved");
            while(allowed()){
                long now=SystemClock.elapsedRealtime();if(now-opened>180000)throw new IOException("Limite de sécurité : 3 minutes de duplex.");
                if(am.getMode()==AudioManager.MODE_IN_CALL)throw new IOException("Un appel local a pris le contrôle audio.");
                AudioDeviceInfo ri=rxRecord.getRoutedDevice(),ro=rxTrack.getRoutedDevice(),ti=txRecord.getRoutedDevice(),to=txTrack.getRoutedDevice();
                boolean rxRoute=ri!=null&&ro!=null&&ri.getId()==scoIn.getId()&&ri.getType()==AudioDeviceInfo.TYPE_BLUETOOTH_SCO&&ro.getId()==speaker.getId()&&ro.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;
                boolean txRoute=ti!=null&&to!=null&&(txInputId<0||ti.getId()==txInputId)&&ti.getType()==AudioDeviceInfo.TYPE_BUILTIN_MIC&&to.getId()==scoOut.getId()&&to.getType()==AudioDeviceInfo.TYPE_BLUETOOTH_SCO;
                if(rxConfirmed&&!rxRoute)throw new IOException("Route D4 réception changée : arrêt du duplex.");
                if(txConfirmed&&!txRoute)throw new IOException("Route D4 microphone changée : arrêt du duplex.");
                if(!rxConfirmed&&rxRoute){rxConfirmed=true;event("ROUTE_CONFIRMED direction=rx input=SCO output=SPEAKER");}
                if(!txConfirmed&&txRoute){txConfirmed=true;if(txInputId<0){txInputId=ti.getId();event("MIC_SELECTED id="+txInputId+" address="+Privacy.clean(ti.getAddress())+" name="+Privacy.clean(String.valueOf(ti.getProductName())));}event("ROUTE_CONFIRMED direction=tx input=MIC output=SCO");}
                if((!rxConfirmed||!txConfirmed)&&now-opened>2500)throw new IOException(!rxConfirmed?"Route D4 SCO → haut-parleur non confirmée.":"Route D4 microphone → SCO non confirmée.");
                int nr=rxRecord.read(rxSamples,0,rxSamples.length,AudioRecord.READ_NON_BLOCKING);if(nr<0)throw new IOException("AudioRecord RX.read="+nr);
                if(nr>0){lastRxData=now;rxRead+=nr;if(rxConfirmed&&txConfirmed&&allowed()&&routed(rxRecord,rxTrack,scoIn.getId(),speaker.getId(),AudioDeviceInfo.TYPE_BLUETOOTH_SCO,AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)){
                    for(int k=0;k<nr;k++){int v=rxSamples[k];if(v!=0)rxNonzero++;rxEnergy+=(long)v*v;rxPeak=Math.max(rxPeak,Math.abs(v));}rxWindow+=nr;
                    int done=rxTrack.write(rxSamples,0,nr,AudioTrack.WRITE_NON_BLOCKING);if(done<0)throw new IOException("AudioTrack RX.write="+done);rxWritten+=done;rxDropped+=nr-done;
                }else rxDropped+=nr;Arrays.fill(rxSamples,(short)0);}
                int nt=txRecord.read(txSamples,0,txSamples.length,AudioRecord.READ_NON_BLOCKING);if(nt<0)throw new IOException("AudioRecord TX.read="+nt);
                if(nt>0){lastTxData=now;txRead+=nt;if(rxConfirmed&&txConfirmed&&allowed()&&routed(txRecord,txTrack,txInputId,scoOut.getId(),AudioDeviceInfo.TYPE_BUILTIN_MIC,AudioDeviceInfo.TYPE_BLUETOOTH_SCO)){
                    for(int k=0;k<nt;k++){int v=txSamples[k];if(v!=0)txNonzero++;txEnergy+=(long)v*v;txPeak=Math.max(txPeak,Math.abs(v));}txWindow+=nt;
                    int done=txTrack.write(txSamples,0,nt,AudioTrack.WRITE_NON_BLOCKING);if(done<0)throw new IOException("AudioTrack TX.write="+done);txWritten+=done;txDropped+=nt-done;
                }else txDropped+=nt;Arrays.fill(txSamples,(short)0);}
                if(now-lastStats>=1000){AudioRecordingConfiguration txConfig=txRecord.getActiveRecordingConfiguration(),rxConfig=rxRecord.getActiveRecordingConfiguration();if(txConfig!=null&&txConfig.isClientSilenced())throw new IOException("Capture micro rendue silencieuse par Android.");if(rxConfig!=null&&rxConfig.isClientSilenced())throw new IOException("Capture SCO rendue silencieuse par Android.");
                    long rxRms=rxWindow==0?0:(long)Math.sqrt((double)rxEnergy/rxWindow),txRms=txWindow==0?0:(long)Math.sqrt((double)txEnergy/txWindow);
                    event("STATS direction=rx samples_read="+rxRead+" samples_written="+rxWritten+" samples_dropped="+rxDropped+" nonzero="+rxNonzero+" rms="+rxRms+" peak="+rxPeak+" route="+rxConfirmed+" underruns="+rxTrack.getUnderrunCount());
                    event("STATS direction=tx samples_read="+txRead+" samples_written="+txWritten+" samples_dropped="+txDropped+" nonzero="+txNonzero+" rms="+txRms+" peak="+txPeak+" route="+txConfirmed+" underruns="+txTrack.getUnderrunCount());
                    status("D4 duplex : RX "+rxWritten+" écrits · TX "+txWritten+" écrits · micro RMS "+txRms+" · routes "+(rxConfirmed&&txConfirmed?"confirmées":"en attente"));
                    lastStats=now;rxEnergy=rxWindow=txEnergy=txWindow=0;rxPeak=txPeak=0;
                }
                if(now-lastRxData>8000||now-lastTxData>8000)throw new IOException("Aucun échantillon audio duplex reçu depuis 8 secondes.");
                if(nr==0&&nt==0)Thread.sleep(5);
            }
            if(!stop&&!RxGate.fresh(SystemClock.elapsedRealtime(),received))throw new IOException("État Bluetooth périmé : duplex arrêté.");
        }finally{
            if(rxRecord!=null){try{rxRecord.stop();}catch(Exception ignored){}rxRecord.release();}
            if(txRecord!=null){try{txRecord.stop();}catch(Exception ignored){}txRecord.release();}
            if(rxTrack!=null){try{rxTrack.pause();rxTrack.flush();}catch(Exception ignored){}rxTrack.release();}
            if(txTrack!=null){try{txTrack.pause();txTrack.flush();}catch(Exception ignored){}txTrack.release();}
        }
    }
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onDestroy(){stop=true;permitted=false;alive=false;getSharedPreferences("rx",0).edit().putBoolean("armed",false).apply();super.onDestroy();}
}
