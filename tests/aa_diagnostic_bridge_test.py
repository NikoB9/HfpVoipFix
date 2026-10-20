#!/usr/bin/env python3
"""Execute the production bridge's one-call sequence using deterministic Android fakes.
No emulator, MIUI HAL, Bluetooth transport or acoustic-car validation is claimed.
"""
import ast,os,pathlib,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parents[1]
module=ast.parse((root/'tests/projection_focus_test.py').read_text())
base=ast.literal_eval(next(n.value for n in module.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='files' for t in n.targets)))
files={k:v for k,v in base.items() if k in ['android/os/Looper.java','android/os/Handler.java','android/media/AudioFocusRequest.java']}
files.update({
'android/R.java':'package android;public class R{public static class drawable{public static int ic_media_play=1;}}',
'android/content/SharedPreferences.java':'''package android.content;public class SharedPreferences{java.util.Map<String,Object> p=new java.util.HashMap<>();public String getString(String k,String d){return (String)p.getOrDefault(k,d);}public boolean getBoolean(String k,boolean d){return (Boolean)p.getOrDefault(k,d);}public int getInt(String k,int d){return (Integer)p.getOrDefault(k,d);}public Editor edit(){return new Editor();}public class Editor{public Editor putString(String k,String v){p.put(k,v);return this;}public Editor putBoolean(String k,boolean v){p.put(k,v);return this;}public Editor putInt(String k,int v){p.put(k,v);return this;}public Editor putLong(String k,long v){p.put(k,v);return this;}public void apply(){}}}''',
'android/content/Context.java':'''package android.content;public class Context{public static final String AUDIO_SERVICE="audio",NOTIFICATION_SERVICE="notify";public android.media.AudioManager audio=new android.media.AudioManager();java.util.Map<String,SharedPreferences> prefs=new java.util.HashMap<>();public Object getSystemService(String s){return s.equals(AUDIO_SERVICE)?audio:new android.app.NotificationManager();}public SharedPreferences getSharedPreferences(String n,int m){return prefs.computeIfAbsent(n,k->new SharedPreferences());}public java.io.File getFilesDir(){return new java.io.File(".");}}''',
'android/content/Intent.java':'package android.content;public class Intent{public Intent(Context c,Class<?> t){}public Intent setAction(String a){return this;}public String getAction(){return "";}public boolean getBooleanExtra(String k,boolean d){return d;}public String getStringExtra(String k){return "";}}',
'android/app/Service.java':'package android.app;public class Service extends android.content.Context{public static int START_NOT_STICKY=2,STOP_FOREGROUND_REMOVE=1;public void onCreate(){}public int onStartCommand(android.content.Intent i,int f,int d){return 2;}public void startForeground(int n,Notification x){}public void stopSelf(){}public void stopForeground(int n){}public android.os.IBinder onBind(android.content.Intent i){return null;}public void onDestroy(){}}',
'android/app/PendingIntent.java':'package android.app;public class PendingIntent{public static int FLAG_UPDATE_CURRENT=1,FLAG_IMMUTABLE=2;public static PendingIntent getService(android.content.Context c,int n,android.content.Intent i,int f){return new PendingIntent();}}',
'android/app/Notification.java':'package android.app;public class Notification{public static class Builder{public Builder(android.content.Context c,String s){}public Builder setSmallIcon(int n){return this;}public Builder setContentTitle(String s){return this;}public Builder setContentText(String s){return this;}public Builder setOngoing(boolean b){return this;}public Builder setOnlyAlertOnce(boolean b){return this;}public Builder addAction(Action a){return this;}public Notification build(){return new Notification();}}public static class Action{public static class Builder{public Builder(Object o,String s,PendingIntent p){}public Action build(){return new Action();}}}}',
'android/app/NotificationChannel.java':'package android.app;public class NotificationChannel{public NotificationChannel(String a,String b,int c){}}',
'android/app/NotificationManager.java':'package android.app;public class NotificationManager{public static int IMPORTANCE_LOW=1;public void createNotificationChannel(NotificationChannel c){}public void notify(int n,Notification x){}}',
'android/os/IBinder.java':'package android.os;public interface IBinder{}',
'android/os/SystemClock.java':'package android.os;public class SystemClock{public static long now=1000;public static long elapsedRealtime(){return now;}}',
'android/util/Log.java':'package android.util;public class Log{public static java.util.List<String> lines=new java.util.ArrayList<>();public static int i(String t,String s){lines.add(s);return 0;}}',
'android/media/AudioAttributes.java':'''package android.media;public class AudioAttributes{public static final int USAGE_MEDIA=1,USAGE_VOICE_COMMUNICATION=2,CONTENT_TYPE_SPEECH=1;public int usage;public static class Builder{AudioAttributes a=new AudioAttributes();public Builder setUsage(int u){a.usage=u;return this;}public Builder setContentType(int n){return this;}public AudioAttributes build(){return a;}}}''',
'android/media/AudioFormat.java':'package android.media;public class AudioFormat{public static final int CHANNEL_IN_MONO=16,CHANNEL_OUT_MONO=4,ENCODING_PCM_16BIT=2;public static class Builder{public Builder setSampleRate(int n){return this;}public Builder setEncoding(int n){return this;}public Builder setChannelMask(int n){return this;}public AudioFormat build(){return new AudioFormat();}}}',
'android/media/MediaRecorder.java':'package android.media;public class MediaRecorder{public static class AudioSource{public static final int MIC=1,VOICE_COMMUNICATION=7,VOICE_RECOGNITION=6;}}',
'android/media/AudioDeviceInfo.java':'''package android.media;public class AudioDeviceInfo{public static final int TYPE_BUILTIN_MIC=15,TYPE_BLUETOOTH_SCO=7,TYPE_BUILTIN_SPEAKER=2;int id,type;String address;public AudioDeviceInfo(int i,int t,String a){id=i;type=t;address=a;}public int getId(){return id;}public int getType(){return type;}public String getAddress(){return address;}public int[] getSampleRates(){return new int[]{16000};}public CharSequence getProductName(){return "test";}}''',
'android/media/AudioRecordingConfiguration.java':'package android.media;public class AudioRecordingConfiguration{public boolean isClientSilenced(){return false;}}',
'android/media/AudioManager.java':'''package android.media;public class AudioManager{
 public static final int GET_DEVICES_INPUTS=1,GET_DEVICES_OUTPUTS=2,MODE_NORMAL=0,MODE_IN_CALL=2,STREAM_MUSIC=3,AUDIOFOCUS_GAIN=1,AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK=3,AUDIOFOCUS_REQUEST_GRANTED=1;
 public interface OnAudioFocusChangeListener{void onAudioFocusChange(int n);}public AudioFocusRequest active;public int requests,abandons;public boolean denyMedia;
 public static AudioDeviceInfo si=new AudioDeviceInfo(70,7,"MASTER"),so=new AudioDeviceInfo(74,7,"MASTER"),mic=new AudioDeviceInfo(15,15,"bottom"),nav=new AudioDeviceInfo(60,25,"1506431339:ap:0mixp:1"),media=new AudioDeviceInfo(54,25,"1506431339:ap:0mixp:0"),speaker=new AudioDeviceInfo(3,2,"");
 public AudioDeviceInfo[] getDevices(int f){return f==1?new AudioDeviceInfo[]{si,mic}:new AudioDeviceInfo[]{so,nav,media,speaker};}public int getMode(){return 0;}public int getStreamVolume(int n){return 10;}public int getStreamMaxVolume(int n){return 15;}
 public int requestAudioFocus(AudioFocusRequest r){active=r;requests++;return denyMedia&&r.attributes.usage==1?0:1;}public int abandonAudioFocusRequest(AudioFocusRequest r){abandons++;return 1;}
}''',
'android/media/AudioRecord.java':'''package android.media;public class AudioRecord{
 public static final int STATE_INITIALIZED=1,READ_NON_BLOCKING=1;public static java.util.List<AudioRecord> all=new java.util.ArrayList<>();public static Runnable tick=()->{};public boolean active,released;public int source;AudioDeviceInfo preferred;
 public static int getMinBufferSize(int r,int c,int e){return 640;}public int getState(){return 1;}public boolean setPreferredDevice(AudioDeviceInfo d){preferred=d;return true;}public AudioDeviceInfo getRoutedDevice(){return preferred!=null?preferred:AudioManager.mic;}
 public void startRecording(){active=true;}public void stop(){active=false;}public void release(){if(released)throw new AssertionError("double record release");released=true;active=false;}public AudioRecordingConfiguration getActiveRecordingConfiguration(){return new AudioRecordingConfiguration();}
 public int read(short[] s,int o,int n,int f){if(released)throw new AssertionError("read released");android.os.SystemClock.now+=250;tick.run();java.util.Arrays.fill(s,o,o+n,(short)1000);return n;}
 public static class Builder{int source;public Builder setAudioSource(int s){source=s;return this;}public Builder setAudioFormat(AudioFormat f){return this;}public Builder setBufferSizeInBytes(int n){return this;}public AudioRecord build(){AudioRecord r=new AudioRecord();r.source=source;all.add(r);return r;}}
}''',
'android/media/AudioTrack.java':'''package android.media;public class AudioTrack{
 public static final int STATE_INITIALIZED=1,MODE_STREAM=1,WRITE_NON_BLOCKING=1;public static java.util.List<AudioTrack> all=new java.util.ArrayList<>();public static boolean wrongRoute;public boolean released,playing;public int usage,nonzeroWrites;AudioDeviceInfo preferred;
 public static int getMinBufferSize(int r,int c,int e){return 640;}public int getState(){return 1;}public boolean setPreferredDevice(AudioDeviceInfo d){preferred=d;return true;}public AudioDeviceInfo getRoutedDevice(){return wrongRoute?AudioManager.speaker:preferred!=null?preferred:usage==1?AudioManager.media:AudioManager.nav;}
 public void setVolume(float v){}public void play(){playing=true;}public void pause(){playing=false;}public void flush(){}public void release(){if(released)throw new AssertionError("double track release");released=true;playing=false;}
 public int getUnderrunCount(){return 0;}public int getPlaybackHeadPosition(){return nonzeroWrites*320;}public int write(short[] s,int o,int n,int f){if(released)throw new AssertionError("write released");if(playing&&s[o]!=0)nonzeroWrites++;return n;}
 public static class Builder{AudioAttributes a;public Builder setAudioAttributes(AudioAttributes x){a=x;return this;}public Builder setAudioFormat(AudioFormat f){return this;}public Builder setBufferSizeInBytes(int n){return this;}public Builder setTransferMode(int n){return this;}public AudioTrack build(){AudioTrack t=new AudioTrack();t.usage=a.usage;all.add(t);return t;}}
}''',
'com/hfpvoipfix/CaptureService.java':'package com.hfpvoipfix;public class CaptureService{static boolean alive=true;}',
'com/hfpvoipfix/RelayService.java':'package com.hfpvoipfix;public class RelayService{static boolean alive=true;static boolean enabled(android.content.Context c){return true;}}',
'com/hfpvoipfix/Root.java':'package com.hfpvoipfix;public class Root{static String command(String s,int t,int n){return "";}}',
'com/hfpvoipfix/ProbeChannel.java':'package com.hfpvoipfix;public class ProbeChannel{String read(){return "";}void publishAaReadiness(String s){}void publishNativeSco(String s){}void close(){}}',
'com/hfpvoipfix/AaRelayPolicy.java':'package com.hfpvoipfix;public class AaRelayPolicy{static String proof(String s,long n){return "off";}}',
'com/hfpvoipfix/BridgeJournal.java':'package com.hfpvoipfix;public class BridgeJournal{static void write(java.io.File f,String s){}}',
'com/hfpvoipfix/AaDiagnosticBridgeTest.java':r'''package com.hfpvoipfix;
import android.media.*;import android.os.*;import android.util.Log;import java.lang.reflect.*;import java.util.*;
public class AaDiagnosticBridgeTest{
 static RxBridgeService service;static String mode;static boolean diagnostic;static int scenario;
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static void field(String n,Object v){try{Field f=RxBridgeService.class.getDeclaredField(n);f.setAccessible(true);f.set(service,v);}catch(Exception e){throw new RuntimeException(e);}}
 static String status(){return "STATUS version=1.9.13 epoch=42 call_id=1 sco_seq=1 requested="+mode+" applied="+mode+" rate=16000 sco_in_id=70 sco_out_id=74 status_at="+SystemClock.now+" call_count=1 call_active=true sco=1 native_sco=true soft_ports=true telecom_ok=true dirty=false";}
 static void reset(boolean diag,boolean local,int which){
  diagnostic=diag;scenario=which;mode=local?"telecom_duplex":"telecom_aa";SystemClock.now=1000;AudioRecord.all.clear();AudioTrack.all.clear();Log.lines.clear();AudioTrack.wrongRoute=which==3;
  service=new RxBridgeService();service.getSharedPreferences("capture",0).edit().putString("phase","running").apply();service.getSharedPreferences("aa_diagnostic",0).edit().putBoolean("enabled",diag).apply();
  field("permitted",true);field("probe",status());field("probeIdentity","42:1");field("received",SystemClock.now);
  service.audio.denyMedia=which==2;
  AudioRecord.tick=()->{
   field("received",SystemClock.now);field("probe",status());
   if(!diagnostic&&SystemClock.now>7000)field("stop",true);
   if(scenario==4&&SystemClock.now>9000)field("stop",true);
   if(scenario==5&&SystemClock.now>9000)field("probeIdentity","42:2");
  };
 }
 static Throwable run(){try{Method m=RxBridgeService.class.getDeclaredMethod("runDuplexBridge",boolean.class);m.setAccessible(true);m.invoke(service,false);return null;}catch(InvocationTargetException e){return e.getCause();}catch(Exception e){throw new RuntimeException(e);}}
 static void closed(){for(AudioRecord r:AudioRecord.all)check(r.released,"every record released");for(AudioTrack t:AudioTrack.all)check(t.released,"every track released");check(service.audio.requests==service.audio.abandons,"all acquired/requested focus released");}
 static long count(String s){return Log.lines.stream().filter(x->x.contains(s)).count();}
 public static void main(String[] args){
  reset(true,false,0);check(run()==null,"complete sequence");
  check(count("PHASE_BEGIN")==8&&count("SEQUENCE_COMPLETE phases=8")==1,"all eight phases exactly once");
  check(AudioRecord.all.size()==3&&AudioTrack.all.size()==4,"one SCO receiver, two microphone instances, nav/media plus two TX tracks");
  int[] mic={0,1,1,1,0,0,1,1},tx={0,0,1,0,0,0,0,1};int p=0;
  for(String line:Log.lines)if(line.contains("PHASE_BEGIN")){check(line.contains("phase="+(p+1)+" ")&&line.contains("mic_active="+(mic[p]==1))&&line.contains("tx_active="+(tx[p]==1)),"real resources match phase "+p);p++;}
  check(count("D6_OUTPUT_SWITCH usage=1")==1,"one projection output switch");closed();
  // A same-call SCO restart must refuse before opening any stream.
  field("stop",false);int before=AudioRecord.all.size();Throwable duplicate=run();check(duplicate instanceof java.io.IOException&&AudioRecord.all.size()==before,"same-call restart blocked");
  reset(true,false,2);check(run() instanceof java.io.IOException,"media focus denial refuses playback");check(count("PHASE_BEGIN")==5,"failure before media audio starts");closed();
  reset(true,false,3);check(run()==null,"wrong route remains observable through rollback");for(AudioTrack t:AudioTrack.all)check(t.nonzeroWrites==0,"no PCM to wrong endpoint");closed();
  reset(true,false,4);check(run()==null&&count("PHASE_BEGIN")<8,"stop cancels sequence");closed();
  reset(true,false,5);check(run()==null&&count("PHASE_BEGIN")<8,"new SCO identity cancels sequence");closed();
  reset(false,true,0);check(run()==null,"D5 baseline");check(count("PHASE_BEGIN")==3&&count("D6_OUTPUT_SWITCH")==0&&service.audio.requests==0,"D5 opens RX, MIC, TX without projection focus");closed();
  reset(false,false,0);check(run()==null&&count("PHASE_BEGIN")==3&&count("D6_OUTPUT_SWITCH")==0,"normal D6 unchanged");closed();
  System.out.println("PASS production RxBridgeService: eight phases/resource rollback, same receiver, media switch, same-call rejection, focus denial, wrong-route PCM exclusion, stop/session cancellation, D5 and normal D6");
 }
}'''
})
with tempfile.TemporaryDirectory(dir=root/'tests') as tmp:
 d=pathlib.Path(tmp);out=d/'classes';out.mkdir()
 for name,content in files.items():
  p=d/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content)
 java=pathlib.Path(os.environ['JAVA_HOME'])/'bin'
 production=['RxBridgeService.java','AaDiagnosticSchedule.java','DuplexSchedule.java','AaAudioTrial.java','ProjectionAudioFocus.java','CarEndpoints.java','LabModes.java','RxGate.java','NativeScoLease.java','Privacy.java','BridgeRecovery.java']
 subprocess.run([str(java/'javac'),'-encoding','UTF-8','-d',str(out),*[str(p) for p in d.rglob('*.java')],*[str(root/'app/src/main/java/com/hfpvoipfix'/s) for s in production]],check=True)
 subprocess.run([str(java/'java'),'-cp',str(out),'com.hfpvoipfix.AaDiagnosticBridgeTest'],check=True,timeout=40)
