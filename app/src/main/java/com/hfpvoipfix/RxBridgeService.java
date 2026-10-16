package com.hfpvoipfix;
import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.util.Log;
import java.io.*;
import java.util.*;

/** Experimental SCO bridge, automatically prepared during capture; never stores PCM. */
public final class RxBridgeService extends Service {
    public static volatile boolean alive,autonomousActive;
    private boolean autonomous;
    private boolean sessionEnabled(){return autonomous?RelayService.alive&&RelayService.enabled(this):CaptureService.alive&&getSharedPreferences("capture",0).getString("phase","").equals("running");}
    private volatile boolean stop, permitted;
    private volatile long received;
    private volatile String probe="",fatal="",audioIdentity,probeIdentity;
    private String lastNotice="";
    private Thread worker,watch;
    private static final String TAG="HfpVoipFix";
    private void status(String s){getSharedPreferences("rx",0).edit().putString("status",s).apply();}
    private void event(String s){BridgeJournal.write(getFilesDir(),"LAB SOFT_BRIDGE "+s);Log.i(TAG,"LAB SOFT_BRIDGE "+s);}
    @Override public void onCreate(){super.onCreate();alive=true;getSharedPreferences("rx",0).edit().putBoolean("armed",false).apply();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("rx","Pont audio expérimental",NotificationManager.IMPORTANCE_LOW));}
    @Override public int onStartCommand(Intent i,int flags,int id){
        PendingIntent cancel=PendingIntent.getService(this,0,new Intent(this,RxBridgeService.class).setAction("stop"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(i!=null&&i.getBooleanExtra("autonomous",false)){autonomous=true;autonomousActive=true;}
        if(i!=null&&"reconfigure".equals(i.getAction())){event("RECONFIGURE outside_call=true");stop=true;permitted=false;if(worker==null)stopSelf();return START_NOT_STICKY;}
        String mode=i==null?"":i.getStringExtra("mode");boolean tx="soft_tx".equals(mode);
        String title=tx?"HFP · essai micro vers Samsung":LabModes.duplex(mode)?"HFP · son + micro":"HFP · réception vers Redmi";
        startForeground(705,new Notification.Builder(this,"rx").setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(title)
            .setContentText("Pont actif pendant la session").addAction(new Notification.Action.Builder(null,"Arrêter",cancel).build()).setOngoing(true).build());
        if(i==null||"stop".equals(i.getAction())){event("STOP_REQUEST source=service_action");if(autonomous)getSharedPreferences("relay",0).edit().putBoolean("enabled",false).apply();getSharedPreferences("rx",0).edit().putBoolean("auto_paused",true).apply();stop=true;permitted=false;if(worker==null)stopSelf();return START_NOT_STICKY;}
        if(i.getBooleanExtra("automatic",false)&&getSharedPreferences("rx",0).getBoolean("auto_paused",false)){stopSelf();return START_NOT_STICKY;}
        if(worker==null){worker=new Thread(this::runBridge,"hfp-software-sco-bridge");worker.start();}return START_NOT_STICKY;
    }
    private boolean allowed(){return !stop&&sessionEnabled()&&permitted&&(audioIdentity==null||audioIdentity.equals(probeIdentity))&&RxGate.fresh(SystemClock.elapsedRealtime(),received);}
    private void watch(){boolean first=true;String activeIdentity=null,completedIdentity=null,mode=null;boolean continuous=false;long end=autonomous?Long.MAX_VALUE:SystemClock.elapsedRealtime()+600000;
        ProbeChannel channel=null;int failures=0;
        try{while(!stop&&sessionEnabled()){String p;
            try{
                if(channel==null)channel=new ProbeChannel();
                long began=SystemClock.elapsedRealtime();p=channel.read();
                if(failures>0||SystemClock.elapsedRealtime()-began>1500)event("PROBE_OK latency_ms="+(SystemClock.elapsedRealtime()-began)+" recovered="+failures);
                failures=0;
            }catch(Exception e){
                if(channel!=null){channel.close();channel=null;}failures++;getSharedPreferences("rx",0).edit().putBoolean("armed",false).apply();
                event("PROBE_RETRY count="+failures+" detail="+Privacy.clean(String.valueOf(e.getMessage())));
                if((activeIdentity!=null&&!continuous)||SystemClock.elapsedRealtime()>end)throw e;
                if(activeIdentity!=null&&!BridgeRecovery.waiting(SystemClock.elapsedRealtime(),received,permitted,activeIdentity,probeIdentity))throw e;
                status((LabModes.car(mode)?"D6":LabModes.telecom(mode)?"D5":"soft_tx".equals(mode)?"D3":"soft_duplex".equals(mode)?"D4":"D2")+" en attente : suivi Bluetooth indisponible, nouvelle tentative.");Thread.sleep(Math.min(15000,1000L*failures));continue;
            }
            probe=p;probeIdentity=RxGate.identity(p);received=SystemClock.elapsedRealtime();
            getSharedPreferences("capture",0).edit().putString("probe",p).putLong("probe_at",System.currentTimeMillis()).apply();
            Map<String,String> values=RxGate.parse(p);
            if(LabModes.car(values.get("requested"))){channel.publishAaReadiness(AaRelayPolicy.proof(p,SystemClock.elapsedRealtime()));channel.publishNativeSco(NativeScoLease.proof(p,SystemClock.elapsedRealtime()));}
            if(!"1.9.10".equals(values.get("version")))throw new IOException("Module 1.9.10 non chargé : redémarrage LSPosed nécessaire.");
            if(first){first=false;mode=values.get("requested");continuous=LabModes.duplex(mode)&&!"soft_duplex_test".equals(mode);if(LabModes.duplex(mode)){String nativeTrial=Root.command("getprop debug.hfpvoipfix.native_split",1000,4).trim();event("NATIVE_TRIAL marker="+Privacy.clean(nativeTrial));if(!"1.7.22-one-boot".equals(nativeTrial)&&!"1.9.0-persistent".equals(nativeTrial))throw new IOException("Duplex : le patch natif 1.7.22 doit être actif pour ce démarrage. Réactiver ce module dans Magisk avant de redémarrer.");}if(!"false".equals(values.get("busy"))||!LabModes.softwareBridge(mode))throw new IOException("Préparation hors appel après application de D2, D3, D4 ou D5.");event("ARM_CONFIRMED direction="+mode);}
            if(activeIdentity==null){
                if(!mode.equals(values.get("requested")))throw new IOException("Mode modifié pendant la préparation automatique.");
                if(RxGate.ready(p)&&!RxGate.identity(p).equals(completedIdentity)){getSharedPreferences("rx",0).edit().putBoolean("armed",false).apply();activeIdentity=RxGate.identity(p);permitted=true;event("HANDSHAKE "+p);}
                else if("true".equals(values.get("busy"))&&!mode.equals(values.get("applied")))throw new IOException("L'appel n'utilise pas le mode armé.");
                else if(SystemClock.elapsedRealtime()>end)throw new IOException("Armement expiré après 10 minutes.");
                else {getSharedPreferences("rx",0).edit().putBoolean("armed",true).apply();status((mode.equals("soft_tx")?"D3 prêt":LabModes.car(mode)?"D6 prêt":LabModes.telecom(mode)?"D5 prêt":LabModes.duplex(mode)?"D4 prêt":"D2 prêt")+(autonomous?" : relais actif, en attente d’un appel.":" : appeler depuis le Redmi. Attente restante : "+((end-SystemClock.elapsedRealtime())/1000)+" s."));}
            }else if(!RxGate.ready(p)||!activeIdentity.equals(RxGate.identity(p))){event("STOP reason=SCO_or_session_ended");permitted=false;if(continuous&&mode.equals(values.get("requested"))&&"false".equals(values.get("dirty"))){completedIdentity=activeIdentity;activeIdentity=null;end=autonomous?Long.MAX_VALUE:SystemClock.elapsedRealtime()+600000;}else stop=true;}
            Thread.sleep(1000);
        }}catch(Exception e){fatal=String.valueOf(e.getMessage());stop=true;permitted=false;event("ERROR handshake="+e.getClass().getSimpleName()+" detail="+Privacy.clean(fatal));}
        finally{if(channel!=null){try{if(LabModes.car(mode))channel.publishNativeSco("off");}catch(Exception ignored){}channel.close();}permitted=false;stop=true;}
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
    private boolean carRxRoute(AudioRecord record,AudioTrack track,AudioDeviceInfo master,AudioDeviceInfo output){
        AudioDeviceInfo a=record.getRoutedDevice(),b=track.getRoutedDevice();
        return a!=null&&b!=null&&a.getId()==master.getId()&&a.getType()==AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            &&(output==null?CarEndpoints.projectionOutput(b):b.getId()==output.getId()&&b.getType()==output.getType());
    }
    private void inventory(AudioManager am){for(int flags:new int[]{AudioManager.GET_DEVICES_INPUTS,AudioManager.GET_DEVICES_OUTPUTS})for(AudioDeviceInfo d:am.getDevices(flags))
        event("D6_DEVICE direction="+flags+" id="+d.getId()+" type="+d.getType()+" address="+Privacy.clean(d.getAddress())+" rates="+Arrays.toString(d.getSampleRates()));}
    private void runBridge(){AudioRecord record=null;AudioTrack track=null;
        try{
            if(!sessionEnabled())throw new IOException("Démarrer la capture avant le pont automatique.");
            String requested=LabModes.valid(Root.command("getprop "+LabModes.PROP,1000,4).trim());
            if(!LabModes.softwareBridge(requested))throw new IOException("Choisir D2, D3, D4 ou D5 et appliquer le mode avant l’appel.");
            if(LabModes.duplex(requested)){boolean progressive="soft_duplex_test".equals(requested);watch=new Thread(this::watch,"hfp-sco-handshake");watch.start();do{audioIdentity=null;runDuplexBridge(progressive);}while(!progressive&&!stop);return;}
            boolean tx=requested.equals("soft_tx");
            status("Vérification hors appel… attendre le message "+(tx?"D3 prêt":"D2 prêt")+".");event("ARM_REQUEST direction="+(tx?"tx":"rx")+" no_audio_saved");
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
            if(!stop&&!RxGate.fresh(SystemClock.elapsedRealtime(),received)){event("STOP reason=stale_probe");throw new IOException("État Bluetooth périmé : pont arrêté.");}
        }catch(Exception e){fatal=String.valueOf(e.getMessage());event("ERROR "+e.getClass().getSimpleName()+" "+fatal);}
        finally{stop=true;permitted=false;
            if(record!=null){try{record.stop();}catch(Exception ignored){}record.release();}
            if(track!=null){try{track.pause();track.flush();}catch(Exception ignored){}track.release();}
            if(!fatal.isEmpty())getSharedPreferences("rx",0).edit().putBoolean("auto_paused",true).apply();
            event("CLOSED "+(fatal.isEmpty()?"normal":"error"));status(fatal.isEmpty()?"Pont arrêté. Noter RX et TX puis exporter.":"Pont arrêté : "+fatal);
            stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
        }
    }
    private void phaseNotice(int phase,String label,int remaining){
        String message=remaining<0?"Duplex · "+(phase==3?"Son + micro actifs jusqu’au raccrochage":"Préparation audio…"):"Diagnostic D4 · phase "+phase+"/4 · "+label+" · "+remaining+" s";
        status(message);if(message.equals(lastNotice))return;lastNotice=message;
        PendingIntent cancel=PendingIntent.getService(this,0,new Intent(this,RxBridgeService.class).setAction("stop"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(705,new Notification.Builder(this,"rx")
            .setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("HFP · audio 1.9.10")
            .setContentText(message).setOnlyAlertOnce(true).setOngoing(true)
            .addAction(new Notification.Action.Builder(null,"Arrêter",cancel).build()).build());
    }
    private void runDuplexBridge(boolean progressive)throws Exception{
        status("Préparation automatique… attendre « D4 prêt ».");event("ARM_REQUEST direction=duplex sequence="+(progressive?"progressive":"continuous")+" no_audio_saved");
        while(!stop&&!allowed())Thread.sleep(40);if(stop)return;
        audioIdentity=RxGate.identity(probe);
        int rate=Integer.parseInt(RxGate.parse(probe).get("rate"));AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);
        boolean telecomBridge=LabModes.telecom(RxGate.parse(probe).get("requested"));
        if(telecomBridge){long until=SystemClock.elapsedRealtime()+5000;while(allowed()&&am.getMode()!=AudioManager.MODE_NORMAL&&SystemClock.elapsedRealtime()<until)Thread.sleep(50);if(!allowed())return;if(am.getMode()!=AudioManager.MODE_NORMAL)throw new IOException("D5 : mode audio normal non confirmé. Activer Gestion des appels et Cadre du sous-système dans LSPosed puis redémarrer.");event("TELECOM_AUDIO_CONFIRMED mode=0");}
        if(am.getMode()==AudioManager.MODE_IN_CALL)throw new IOException("Appel téléphonique local actif : essai refusé.");
        boolean carBridge=LabModes.car(RxGate.parse(probe).get("requested"));
        if(carBridge)inventory(am);
        Map<String,String> ports=RxGate.parse(probe);
        AudioDeviceInfo scoIn=carBridge?CarEndpoints.master(am,AudioManager.GET_DEVICES_INPUTS,Integer.parseInt(ports.getOrDefault("sco_in_id","-1"))):unique(am,AudioManager.GET_DEVICES_INPUTS,AudioDeviceInfo.TYPE_BLUETOOTH_SCO);
        AudioDeviceInfo scoOut=carBridge?CarEndpoints.master(am,AudioManager.GET_DEVICES_OUTPUTS,Integer.parseInt(ports.getOrDefault("sco_out_id","-1"))):unique(am,AudioManager.GET_DEVICES_OUTPUTS,AudioDeviceInfo.TYPE_BLUETOOTH_SCO);
        AudioDeviceInfo[] vehicle=null; // D6 projection experiment does not bridge two SCO devices.
        boolean projection=carBridge&&vehicle==null&&CarEndpoints.projection(am);
        if(carBridge&&vehicle==null&&!projection)throw new IOException("D6 : aucun endpoint véhicule ni mix de projection identifié.");
        AudioDeviceInfo mic=vehicle==null?null:vehicle[0];
        AudioDeviceInfo speaker=vehicle!=null?vehicle[1]:projection?null:unique(am,AudioManager.GET_DEVICES_OUTPUTS,AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
        int micType=mic==null?AudioDeviceInfo.TYPE_BUILTIN_MIC:mic.getType();
        if(carBridge)event("D6_ENDPOINTS master_in="+scoIn.getId()+" master_out="+scoOut.getId()+" car_in="+(mic==null?-1:mic.getId())+" car_out="+(speaker==null?-1:speaker.getId())+" projection="+projection+" mic_local="+(mic==null));
        logMicCandidates(am);
        int inMin=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT),outMin=AudioTrack.getMinBufferSize(rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
        if(inMin<=0||outMin<=0)throw new IOException("Format audio duplex non pris en charge à "+rate+" Hz");
        AudioFormat inFormat=new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build();
        AudioFormat outFormat=new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
        AudioRecord rxRecord=null,txRecord=null;AudioTrack rxTrack=null,txTrack=null;
        ProjectionAudioFocus projectionFocus=null;
        try{
            // Phase 1 opens only the same capture/playback paths as standalone D2.
            rxRecord=new AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION).setAudioFormat(inFormat).setBufferSizeInBytes(Math.max(inMin*2,rate/5)).build();
            AudioAttributes rxAttributes=new AudioAttributes.Builder().setUsage(carBridge?AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
            if(carBridge){projectionFocus=new ProjectionAudioFocus(am,rxAttributes,this::event);projectionFocus.acquire();}
            rxTrack=new AudioTrack.Builder().setAudioAttributes(rxAttributes).setAudioFormat(outFormat).setBufferSizeInBytes(Math.max(outMin*2,rate/5)).setTransferMode(AudioTrack.MODE_STREAM).build();
            if(carBridge)event("D6_PROJECTED_SPEECH usage=NAVIGATION_GUIDANCE experimental=true mic=local");
            if(rxRecord.getState()!=AudioRecord.STATE_INITIALIZED||rxTrack.getState()!=AudioTrack.STATE_INITIALIZED)throw new IOException("Initialisation RX refusée.");
            if(!rxRecord.setPreferredDevice(scoIn)||(speaker!=null&&!rxTrack.setPreferredDevice(speaker)))throw new IOException("Routage RX refusé.");
            rxTrack.setVolume(0.35f);rxRecord.startRecording();if(projectionFocus==null||projectionFocus.granted())rxTrack.play();
            event("PLAYBACK_VOLUME rx=MEDIA index="+am.getStreamVolume(AudioManager.STREAM_MUSIC)+" max="+am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            short[] rxSamples=new short[rate/50],txSamples=new short[rate/50],silence=new short[rate/50];
            String[] labels={"","Réception seule","Réception + capture micro","Duplex complet","TX arrêté, micro maintenu"};
            int phase=0;long routeLostSince=0;long phaseStart=0,lastStats=0,lastRxData=SystemClock.elapsedRealtime(),lastTxData=lastRxData;
            long rxRead=0,rxWritten=0,rxDropped=0,rxEnergy=0,rxWindow=0,txRead=0,txWritten=0,txEnergy=0,txWindow=0;
            int rxPeak=0,txPeak=0,txInputId=mic==null?-1:mic.getId();String lastRoutes="";
            boolean paused=false,focusPaused=false;
            while(!stop){
                if(!allowed()){
                    if(!BridgeRecovery.waiting(SystemClock.elapsedRealtime(),received,permitted,audioIdentity,probeIdentity))break;
                    if(!paused){rxTrack.pause();rxTrack.flush();if(txTrack!=null){txTrack.pause();txTrack.flush();}paused=true;event("AUDIO_PAUSE reason=stale_probe no_forwarding=true");status("Suivi Bluetooth interrompu : audio suspendu, reconnexion en cours…");}
                    for(int drain=0;drain<10&&rxRecord.read(rxSamples,0,rxSamples.length,AudioRecord.READ_NON_BLOCKING)>0;drain++){}
                    if(txRecord!=null)for(int drain=0;drain<10&&txRecord.read(txSamples,0,txSamples.length,AudioRecord.READ_NON_BLOCKING)>0;drain++){}
                    Arrays.fill(rxSamples,(short)0);Arrays.fill(txSamples,(short)0);Thread.sleep(100);continue;
                }
                if(paused){if(projectionFocus==null||projectionFocus.granted())rxTrack.play();if(txTrack!=null)txTrack.play();paused=false;lastRxData=lastTxData=SystemClock.elapsedRealtime();routeLostSince=0;event("AUDIO_RESUME fresh_same_session=true");phaseNotice(phase,labels[phase],-1);}
                long now=SystemClock.elapsedRealtime();
                if(projectionFocus!=null){
                    boolean lost=!projectionFocus.granted();
                    if(lost&&!focusPaused){rxTrack.pause();rxTrack.flush();focusPaused=true;event("D6_FOCUS playback=paused");}
                    else if(!lost&&focusPaused){rxTrack.play();focusPaused=false;routeLostSince=0;event("D6_FOCUS playback=resumed");}
                }
                if(am.getMode()==AudioManager.MODE_IN_CALL)throw new IOException("Un appel local a pris le contrôle audio.");
                if(telecomBridge&&am.getMode()!=AudioManager.MODE_NORMAL)throw new IOException("D5 : Telecom a repris un mode audio incompatible avec le pont.");
                int nextPhase=DuplexSchedule.next(phase,now-phaseStart,progressive);
                if(nextPhase!=phase){
                    if(carBridge&&phase>0)event("D6_RX_WINDOW phase="+phase+" samples="+rxWindow+" rms="+(rxWindow==0?0:(long)Math.sqrt((double)rxEnergy/rxWindow))+" peak="+rxPeak+" focus="+(projectionFocus!=null&&projectionFocus.granted()));
                    event("PHASE_END phase="+phase+" elapsed_ms="+(phase==0?0:now-phaseStart)+" rx_read="+rxRead+" rx_written="+rxWritten+" rx_dropped="+rxDropped+" tx_read="+txRead+" tx_written="+txWritten);
                    phase=nextPhase;
                    if(phase==5){event("SEQUENCE_COMPLETE phases=4");status("D4 : quatre phases terminées. Raccrocher puis exporter le rapport.");break;}
                    event("PHASE_TRANSITION phase="+phase+" ms="+System.currentTimeMillis());
                    if(phase==2){
                        // Do not even construct a SCO AudioTrack before phase 3.
                        txRecord=new AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION).setAudioFormat(inFormat).setBufferSizeInBytes(Math.max(inMin*2,rate/5)).build();
                        if(txRecord.getState()!=AudioRecord.STATE_INITIALIZED)throw new IOException("Capture micro refusée.");
                        if(mic!=null&&!txRecord.setPreferredDevice(mic))throw new IOException("D6 : routage micro véhicule refusé.");
                        txRecord.startRecording();lastTxData=SystemClock.elapsedRealtime();
                    }else if(phase==3){
                        txTrack=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setAudioFormat(outFormat).setBufferSizeInBytes(Math.max(outMin*2,rate/5)).setTransferMode(AudioTrack.MODE_STREAM).build();
                        if(txTrack.getState()!=AudioTrack.STATE_INITIALIZED||!txTrack.setPreferredDevice(scoOut))throw new IOException("Sortie TX SCO refusée.");
                        txTrack.setVolume(0.8f);txTrack.play();
                    }else if(phase==4){
                        // Keep microphone capture active: only the TX output is removed.
                        txTrack.pause();txTrack.flush();txTrack.release();txTrack=null;
                    }
                    phaseStart=SystemClock.elapsedRealtime();lastStats=phaseStart;lastRxData=phaseStart;
                    rxRead=rxWritten=rxDropped=rxEnergy=rxWindow=txRead=txWritten=txEnergy=txWindow=0;rxPeak=txPeak=0;lastRoutes="";
                    event("PHASE_BEGIN phase="+phase+" duration_ms="+(progressive?15000:phase==3?-1:1000)+" label="+labels[phase].replace(' ','_')+" mic_source=VOICE_COMMUNICATION mic_policy=default mic_active="+(txRecord!=null)+" tx_active="+(txTrack!=null)+" ms="+System.currentTimeMillis());
                    phaseNotice(phase,labels[phase],progressive?15:-1);now=phaseStart;
                }
                AudioDeviceInfo ri=rxRecord.getRoutedDevice(),ro=rxTrack.getRoutedDevice(),ti=txRecord==null?null:txRecord.getRoutedDevice(),to=txTrack==null?null:txTrack.getRoutedDevice();
                boolean rxRoute=carBridge?carRxRoute(rxRecord,rxTrack,scoIn,speaker):routed(rxRecord,rxTrack,scoIn.getId(),speaker.getId(),AudioDeviceInfo.TYPE_BLUETOOTH_SCO,AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
                boolean micRoute=ti!=null&&(txInputId<0||ti.getId()==txInputId)&&ti.getType()==micType;
                if(micRoute&&txInputId<0){txInputId=ti.getId();event("MIC_SELECTED source=VOICE_COMMUNICATION policy_default=true id="+txInputId+" address="+Privacy.clean(ti.getAddress()));}
                boolean txRoute=txTrack!=null&&routed(txRecord,txTrack,txInputId,scoOut.getId(),micType,AudioDeviceInfo.TYPE_BLUETOOTH_SCO);
                String routes="phase="+phase+" rx="+rxRoute+" mic="+micRoute+" tx="+txRoute+" rx_in="+(ri==null?-1:ri.getId())+" rx_out="+(ro==null?-1:ro.getId())+" tx_in="+(ti==null?-1:ti.getId())+" tx_out="+(to==null?-1:to.getId());
                if(!routes.equals(lastRoutes)){event("PHASE_ROUTE "+routes);if(carBridge)event("D6_ACTUAL_OUTPUT id="+(ro==null?-1:ro.getId())+" type="+(ro==null?-1:ro.getType())+" address="+(ro==null?"none":Privacy.clean(ro.getAddress()))+" registered_projection="+CarEndpoints.projectionOutput(ro));lastRoutes=routes;}
                if(!progressive&&phase==3){if((rxRoute||focusPaused)&&micRoute&&txRoute)routeLostSince=0;else if(routeLostSince==0)routeLostSince=now;else if(now-routeLostSince>3000)throw new IOException("Route duplex perdue depuis 3 secondes : exporter le rapport.");}
                // Observe route conflicts without retrying preferences or aborting the
                // sequence. Never forward audio to an unexpected endpoint.
                if(!rxRoute&&!focusPaused)rxTrack.write(silence,0,silence.length,AudioTrack.WRITE_NON_BLOCKING);
                if(txTrack!=null&&!txRoute)txTrack.write(silence,0,silence.length,AudioTrack.WRITE_NON_BLOCKING);
                int nr=rxRecord.read(rxSamples,0,rxSamples.length,AudioRecord.READ_NON_BLOCKING);if(nr<0)throw new IOException("AudioRecord RX.read="+nr);
                if(nr>0){lastRxData=now;rxRead+=nr;for(int k=0;k<nr;k++){int v=rxSamples[k];rxEnergy+=(long)v*v;rxPeak=Math.max(rxPeak,Math.abs(v));}rxWindow+=nr;
                    if(allowed()&&(projectionFocus==null||projectionFocus.granted())&&(carBridge?carRxRoute(rxRecord,rxTrack,scoIn,speaker):routed(rxRecord,rxTrack,scoIn.getId(),speaker.getId(),AudioDeviceInfo.TYPE_BLUETOOTH_SCO,AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))){
                        int done=rxTrack.write(rxSamples,0,nr,AudioTrack.WRITE_NON_BLOCKING);if(done<0)throw new IOException("AudioTrack RX.write="+done);rxWritten+=done;rxDropped+=nr-done;
                    }else rxDropped+=nr;Arrays.fill(rxSamples,(short)0);
                }
                int nt=txRecord==null?0:txRecord.read(txSamples,0,txSamples.length,AudioRecord.READ_NON_BLOCKING);if(nt<0)throw new IOException("AudioRecord TX.read="+nt);
                if(nt>0){lastTxData=now;txRead+=nt;for(int k=0;k<nt;k++){int v=txSamples[k];txEnergy+=(long)v*v;txPeak=Math.max(txPeak,Math.abs(v));}txWindow+=nt;
                    if(txTrack!=null&&allowed()&&routed(txRecord,txTrack,txInputId,scoOut.getId(),micType,AudioDeviceInfo.TYPE_BLUETOOTH_SCO)){
                        int done=txTrack.write(txSamples,0,nt,AudioTrack.WRITE_NON_BLOCKING);if(done<0)throw new IOException("AudioTrack TX.write="+done);txWritten+=done;
                    }Arrays.fill(txSamples,(short)0);
                }
                if(now-lastStats>=2000){

                    AudioRecordingConfiguration rc=rxRecord.getActiveRecordingConfiguration(),tc=txRecord==null?null:txRecord.getActiveRecordingConfiguration();
                    if(!progressive&&((rc!=null&&rc.isClientSilenced())||(tc!=null&&tc.isClientSilenced())))throw new IOException("Capture duplex rendue silencieuse par Android.");
                    event("PHASE_STATS "+routes+" rx_read="+rxRead+" rx_written="+rxWritten+" rx_dropped="+rxDropped+" rx_rms="+(rxWindow==0?0:(long)Math.sqrt((double)rxEnergy/rxWindow))+" rx_peak="+rxPeak+" rx_head="+Integer.toUnsignedLong(rxTrack.getPlaybackHeadPosition())+" rx_underruns="+rxTrack.getUnderrunCount()+" rx_silenced="+(rc!=null&&rc.isClientSilenced())+" tx_read="+txRead+" tx_written="+txWritten+" tx_rms="+(txWindow==0?0:(long)Math.sqrt((double)txEnergy/txWindow))+" tx_peak="+txPeak+" tx_silenced="+(tc!=null&&tc.isClientSilenced())+" audio_mode="+am.getMode()+" media_volume="+am.getStreamVolume(AudioManager.STREAM_MUSIC));
                    phaseNotice(phase,labels[phase],progressive?Math.max(0,(int)((15000-(now-phaseStart)+999)/1000)):-1);
                    if(carBridge&&focusPaused)status("D6 : sortie suspendue, priorité audio perdue. Exporter le rapport.");
                    lastStats=now;rxEnergy=rxWindow=txEnergy=txWindow=0;rxPeak=txPeak=0;
                }
                // A missing stream is logged instead of preventing phases 3/4 from
                // testing whether adding/removing TX restores RX. Overall time is 60 s.
                if(!progressive&&(now-lastRxData>8000||(txRecord!=null&&now-lastTxData>8000)))throw new IOException("Capture duplex sans données depuis 8 secondes.");
                if(now-lastRxData>8000&&now-lastStats<10)event("PHASE_NO_DATA phase="+phase+" direction=rx");
                if(txRecord!=null&&now-lastTxData>8000&&now-lastStats<10)event("PHASE_NO_DATA phase="+phase+" direction=tx");
                Thread.sleep(10);
            }
            event("DUPLEX_END progressive="+progressive+" phase="+phase+" elapsed_ms="+(SystemClock.elapsedRealtime()-phaseStart)+" rx_written="+rxWritten+" tx_written="+txWritten);
            if(!stop&&!RxGate.fresh(SystemClock.elapsedRealtime(),received)){event("STOP reason=stale_probe");throw new IOException("État Bluetooth périmé : test arrêté.");}
        }finally{
            try{
            if(txTrack!=null){try{txTrack.pause();txTrack.flush();}catch(Exception ignored){}txTrack.release();}
            if(txRecord!=null){try{txRecord.stop();}catch(Exception ignored){}txRecord.release();}
            if(rxTrack!=null){try{rxTrack.pause();rxTrack.flush();}catch(Exception ignored){}rxTrack.release();}
            if(rxRecord!=null){try{rxRecord.stop();}catch(Exception ignored){}rxRecord.release();}
            }finally{if(projectionFocus!=null)projectionFocus.close();}
        }
    }
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onDestroy(){stop=true;permitted=false;alive=false;autonomousActive=false;getSharedPreferences("rx",0).edit().putBoolean("armed",false).apply();super.onDestroy();}
}
