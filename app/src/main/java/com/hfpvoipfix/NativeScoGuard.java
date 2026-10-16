package com.hfpvoipfix;
import android.os.SystemClock;
import android.util.Log;
import java.util.Timer;
import java.util.TimerTask;
import de.robv.android.xposed.*;
/** System-server observer. All service/HAL work runs on its own worker, outside broker locks. */
final class NativeScoGuard {
 private static boolean installed;
 private static volatile Snapshot latest;
 private static final java.util.concurrent.atomic.AtomicLong revision=new java.util.concurrent.atomic.AtomicLong();
 private static String lastError="";
 private static final class ProjectionEvidence {
  final String proof;final long at;
  ProjectionEvidence(String p,long t){proof=p;at=t;}
 }
 private static volatile ProjectionEvidence projection;
 static boolean projectionReady(String proof,long now){
  ProjectionEvidence e=projection;
  if(e==null||now<e.at||now-e.at>=1200)return false;
  String[] a=proof.split(":"),b=e.proof.split(":");
  return a.length==6&&b.length==6&&a[2].equals(b[2])&&a[3].equals(b[3])&&a[4].equals(b[4])&&a[5].equals(b[5]);
 }
 private static final NativeScoKeeper keeper=new NativeScoKeeper();
 private static final class Snapshot {
  final Object broker;final boolean on;final long revision;
  Snapshot(Object b,boolean o,long r){broker=b;on=o;revision=r;}
 }
 private static String prop(String key){return (String)XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.os.SystemProperties",null),"get",key,"");}
 static synchronized void install(ClassLoader loader){
  if(installed)return;
  // This server class is unavailable in normal application/Telecom class loaders.
  final Class<?> broker;
  try{broker=XposedHelpers.findClass("com.android.server.audio.AudioDeviceBroker",loader);}catch(Throwable absent){return;}
  installed=true;
  try{
   XposedHelpers.findAndHookMethod(broker,"setBluetoothScoOn",boolean.class,String.class,new XC_MethodHook(){
    @Override protected void afterHookedMethod(MethodHookParam p){
     if(p.hasThrowable())return;
     latest=new Snapshot(p.thisObject,Boolean.TRUE.equals(p.args[0]),revision.incrementAndGet());
     // Do not call AudioService or the HAL here: the caller can still hold broker locks.
    }
   });
   Timer timer=new Timer("HfpLab-native-sco-lease",true);
   timer.schedule(new TimerTask(){public void run(){poll();}},500,500);
   Log.i("HfpVoipFix","LAB D6_NATIVE_HAL hook_ready=true lease_ms=5000 no_policy_route_override=true");
  }catch(Throwable t){Log.i("HfpVoipFix","LAB D6_NATIVE_HAL hook_ready=false reason="+t.getClass().getSimpleName());}
 }
 static synchronized void poll(){
  Snapshot s=latest;if(s==null){projection=null;return;}
  try{
   String mode=prop(LabModes.PROP),proof=prop(NativeScoLease.PROP);
   if(!"telecom_aa".equals(mode)&&!keeper.owns()){projection=null;return;}
   boolean allowed=NativeScoLease.allow(mode,prop("ro.product.device"),prop("ro.build.version.release"),prop("debug.hfpvoipfix.native_split"),proof,SystemClock.elapsedRealtime())&&portsPresent(proof);
   Object audio=XposedHelpers.getObjectField(s.broker,"mAudioService");
   int audioMode=(Integer)XposedHelpers.callMethod(audio,"getMode");
   projection=allowed&&audioMode==0&&latest==s?new ProjectionEvidence(proof,SystemClock.elapsedRealtime()):null;
   int action=keeper.action(s.on,s.revision,allowed,NativeScoLease.session(proof),audioMode);
   if(action==NativeScoKeeper.NONE||latest!=s)return;
   int status=(Integer)XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.media.AudioSystem",null),"setParameters",action==NativeScoKeeper.ENABLE?"BT_SCO=on":"BT_SCO=off");
   keeper.result(action,s.revision,status);lastError="";
   Log.i("HfpVoipFix","LAB D6_NATIVE_HAL action="+(action==NativeScoKeeper.ENABLE?"preserve_master":"restore_broker")+" result="+status+" broker_revision="+s.revision+" policy_routes_unchanged=true");
  }catch(Throwable t){projection=null;String error=s.revision+":"+t.getClass().getSimpleName();if(!error.equals(lastError)){lastError=error;Log.i("HfpVoipFix","LAB D6_NATIVE_HAL unavailable="+error);}}
 }
 private static boolean portsPresent(String proof){
  try{
   String[] lease=proof.split(":");if(lease.length!=6)return false;
   int input=Integer.parseInt(lease[4]),output=Integer.parseInt(lease[5]);
   java.util.List<Object> ports=new java.util.ArrayList<>();
   if(!Integer.valueOf(0).equals(XposedHelpers.callStaticMethod(android.media.AudioManager.class,"listAudioDevicePorts",ports)))return false;
   String a=null,b=null;
   for(Object port:ports){int id=(Integer)XposedHelpers.callMethod(port,"id"),type=(Integer)XposedHelpers.callMethod(port,"type");
    if(id==input&&type==0x80000008)a=(String)XposedHelpers.callMethod(port,"address");
    if(id==output&&type==0x20)b=(String)XposedHelpers.callMethod(port,"address");
   }
   return a!=null&&!a.isEmpty()&&a.equals(b);
  }catch(Throwable unavailable){return false;}
 }
}
