package com.hfpvoipfix;
import android.media.AudioRecord;
import android.media.AudioDeviceInfo;
import android.os.SystemClock;
import android.util.Log;
import java.util.WeakHashMap;
import de.robv.android.xposed.*;
/** AA process only. Observe existing submix reads; never start a recorder or change its data. */
final class AaProjectionReadHook {
 private static boolean installed;private static String lastError="";
 private static final ThreadLocal<Integer> depth=new ThreadLocal<>();
 private static final WeakHashMap<AudioRecord,Stats> states=new WeakHashMap<>();
 private static final java.util.concurrent.ThreadPoolExecutor logging=new java.util.concurrent.ThreadPoolExecutor(1,1,0,java.util.concurrent.TimeUnit.MILLISECONDS,
  new java.util.concurrent.ArrayBlockingQueue<Runnable>(4),r->{Thread t=new Thread(r,"HfpLab-AA-levels");t.setDaemon(true);return t;},new java.util.concurrent.ThreadPoolExecutor.DiscardPolicy());
 private static final class Stats {String session;long start,reads,units;PcmLevel level=new PcmLevel();Stats(String s,long t){session=s;start=t;}}
 private static String prop(String key){return (String)XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.os.SystemProperties",null),"get",key,"");}
 private static void event(String s){Log.i("HfpVoipFix","LAB AA_SUBMIX_PCM "+s);}
 static synchronized void install(){if(installed)return;installed=true;
  try{
   java.util.Set<XC_MethodHook.Unhook> hooks=XposedBridge.hookAllMethods(AudioRecord.class,"read",new XC_MethodHook(){
    @Override protected void beforeHookedMethod(MethodHookParam p){Integer d=depth.get();depth.set(d==null?1:d+1);p.setObjectExtra("aa.pcm.outer",d==null);}
    @Override protected void afterHookedMethod(MethodHookParam p){
     Integer d=depth.get();if(d==null||d<=1)depth.remove();else depth.set(d-1);
     if(!Boolean.TRUE.equals(p.getObjectExtra("aa.pcm.outer"))||p.hasThrowable()||!(p.getResult() instanceof Integer))return;
     try{observe((AudioRecord)p.thisObject,p.args,(Integer)p.getResult());}
     catch(Throwable t){String e=t.getClass().getSimpleName();if(!e.equals(lastError)){lastError=e;event("unavailable="+e);}}
    }
   });event("hook_ready="+!hooks.isEmpty()+" public_read_overloads="+hooks.size()+" sampled_max_per_read=256 no_pcm_saved=true");
  }catch(Throwable t){event("hook_ready=false reason="+t.getClass().getSimpleName());}
 }
 static void observe(AudioRecord record,Object[] args,int returned){
  if(record.getAudioSource()!=8||args.length<2)return;
  long now=SystemClock.elapsedRealtime();String proof=prop(NativeScoLease.PROP);
  boolean allowed=NativeScoLease.allow(prop(LabModes.PROP),prop("ro.product.device"),prop("ro.build.version.release"),prop("debug.hfpvoipfix.native_split"),proof,now);
  synchronized(states){
   if(!allowed){states.remove(record);return;}
   String session=NativeScoLease.session(proof);Stats s=states.get(record);
   if(s==null||!s.session.equals(session)){if(states.size()>=8)states.clear();s=new Stats(session,now);states.put(record,s);}
   s.reads++;if(returned>0){s.units+=returned;s.level.add(args[0],args[0] instanceof java.nio.ByteBuffer?0:(Integer)args[1],returned,record.getAudioFormat());}
   if(now-s.start<2000)return;
   final long reads=s.reads,units=s.units,inspected=s.level.inspected,rms=s.level.rms();final int peak=s.level.peak;
   final java.lang.ref.WeakReference<AudioRecord> ref=new java.lang.ref.WeakReference<>(record);
   states.put(record,new Stats(session,now));
   logging.execute(()->emit(ref,session,reads,units,inspected,rms,peak,returned));
  }
 }
 private static void emit(java.lang.ref.WeakReference<AudioRecord> ref,String session,long reads,long units,long inspected,long rms,int peak,int returned){
  try{
   AudioRecord record=ref.get();if(record==null)return;
   AudioDeviceInfo device=record.getRoutedDevice();
   String address=device==null?"none":device.getAddress();
   // Do not print arbitrary device addresses from another recorder.
   if(address==null||!address.matches("[0-9]+:ap:[0-9]+mixp:[0-9]+"))address="unidentified";
   event("session="+session+" input_id="+(device==null?-1:device.getId())+" input_type="+(device==null?-1:device.getType())+" address="+address+
    " rate="+record.getSampleRate()+" encoding="+record.getAudioFormat()+" channels="+record.getChannelCount()+" reads="+reads+" returned_units="+units+
    " inspected="+inspected+" sampled_rms="+rms+" sampled_peak="+peak+" last_result="+returned+" content_identity_unverified=true");
  }catch(Throwable t){event("metadata_unavailable="+t.getClass().getSimpleName());}
 }
}
