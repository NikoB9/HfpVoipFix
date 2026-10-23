package com.hfpvoipfix;

import android.os.SystemClock;
import android.util.Log;
import de.robv.android.xposed.*;
import java.io.*;
import java.lang.reflect.*;
import java.security.MessageDigest;
import java.util.*;

/** Exact installed AA binary only. Never changes a car response, call state or PCM. */
final class AaCarFocusHook {
 private static boolean installed;
 private static volatile String status="not_loaded";
 static String status(){return status;}
 private static final Map<Object,String> attempted=new WeakHashMap<>();
 private static final Map<Object,Transport> transports=new WeakHashMap<>();
 private static final class Transport {long start,calls,startedCalls,ackMessages,errors;String session;Transport(long now,String s){start=now;session=s;}}
 private static String prop(String k){return (String)XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.os.SystemProperties",null),"get",k,"");}
 private static String proof(){long now=SystemClock.elapsedRealtime();String p=prop(NativeScoLease.PROP);
  return NativeScoLease.allow(prop(LabModes.PROP),prop("ro.product.device"),prop("ro.build.version.release"),prop("debug.hfpvoipfix.native_split"),p,now)?p:null;}
 private static void event(String s){Log.i("HfpVoipFix","LAB AA_CAR_FOCUS elapsed_ms="+SystemClock.elapsedRealtime()+" "+s);}
 private static Object field(Object o,String n)throws Exception{
  for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException ignored){}
  throw new NoSuchFieldException(n);
 }
 private static int state(Object o){return o instanceof Enum?((Enum<?>)o).ordinal():-1;}
 static boolean exactApk(String path){
  if(path==null)return false;
  try(InputStream in=new FileInputStream(path)){
   MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)md.update(b,0,n);
   StringBuilder hex=new StringBuilder();for(byte v:md.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",v&255));
   return AaCarFocusPolicy.APK_SHA256.equals(hex.toString());
  }catch(Exception e){return false;}
 }
 static synchronized void install(ClassLoader cl,String path){
  if(installed)return;installed=true;
  // Obfuscated names below are meaningful only for the exact APK supplied by the user.
  if(!exactApk(path)){status="apk_mismatch";event("hook_ready=false reason=apk_fingerprint_mismatch normal_aa_unchanged=true");return;}
  installVerified(cl);
 }
 static void installVerified(ClassLoader cl){
  try{
   Class<?> focus=XposedHelpers.findClass("iso",cl),request=XposedHelpers.findClass("xkn",cl),carState=XposedHelpers.findClass("xko",cl);
   final Method send=focus.getDeclaredMethod("l",request,int.class);send.setAccessible(true);
   final Object mayDuck=request.getEnumConstants()[2];
   XposedHelpers.findAndHookMethod(focus,"m",int.class,new XC_MethodHook(){
    @Override protected void afterHookedMethod(MethodHookParam p){
     if(p.hasThrowable())return;
     try{
      String lease=proof();if(lease==null)return;
      Object o=p.thisObject,phone=field(o,"f"),handler=field(o,"m");
      int gain=(Integer)p.args[0],car=state(field(o,"h"));boolean inCall=(Boolean)field(phone,"d");
      boolean pending=field(o,"j")!=null,stopped=(Boolean)XposedHelpers.callMethod(handler,"f");
      Object owner=field(o,"i");String ownerKind="com.hfpvoipfix".equals(owner)?"relay":"other_or_none";
      boolean normalMode=Integer.valueOf(0).equals(XposedHelpers.callMethod(field(o,"o"),"getMode"));
      boolean blocked=AaCarFocusPolicy.blockedRelay(true,gain,owner instanceof String?(String)owner:null,car,inCall,pending,stopped,field(o,"l")!=null,normalMode);
      event("event=android_request branch=iso session="+NativeScoLease.session(lease)+" gain="+gain+" owner="+ownerKind+" car_state="+car+" in_call="+inCall+" pending="+pending+" stopped="+stopped+" normal_mode="+normalMode+" blocked_relay="+blocked);
      if(!blocked)return;
      String session=NativeScoLease.session(lease);
      synchronized(attempted){if(session.equals(attempted.get(o)))return;if(attempted.size()>=8)attempted.clear();attempted.put(o,session);}
      // Recheck transport evidence immediately before invoking AA's ORIGINAL sender.
      String current=proof();if(current==null||!session.equals(NativeScoLease.session(current)))return;
      event("event=relay_request_forward attempt=1 session="+session+" request=may_duck real_car_decision_required=true");
      send.invoke(o,mayDuck,1);
      event("event=relay_sender_return session="+session+" completed=true car_grant_unverified=true");
     }catch(Throwable t){event("event=request_unavailable reason="+t.getClass().getSimpleName());}
    }
   });
   XposedHelpers.findAndHookMethod(focus,"l",request,int.class,new XC_MethodHook(){
    @Override protected void beforeHookedMethod(MethodHookParam p){try{if(proof()!=null)event("event=sender_enter branch=iso request="+state(p.args[0])+" attempt="+p.args[1]+" transport_bound="+(field(p.thisObject,"l")!=null));}catch(Throwable ignored){}}
   });
   XposedHelpers.findAndHookMethod(focus,"i",carState,boolean.class,boolean.class,new XC_MethodHook(){
    @Override protected void afterHookedMethod(MethodHookParam p){try{if(proof()!=null)event("event=car_state_processed branch=iso incoming="+state(p.args[0])+" resulting="+state(field(p.thisObject,"h"))+" flag1="+p.args[1]+" flag2="+p.args[2]+" error="+p.hasThrowable());}catch(Throwable ignored){}}
   });
   status="exact_iso";event("hook_ready=true branch=iso apk_exact=true relay_request=bounded_one_per_sco_session no_grant_override=true");
  }catch(Throwable t){status="iso_unavailable";event("hook_ready=false branch=iso reason="+t.getClass().getSimpleName());}
  // The older implementation has no reliable package attribution here: observe only.
  try{
   Class<?> legacy=XposedHelpers.findClass("qvk",cl);
   XposedHelpers.findAndHookMethod(legacy,"i",int.class,new XC_MethodHook(){
    @Override protected void afterHookedMethod(MethodHookParam p){try{if(proof()!=null)event("event=android_request branch=qvk gain="+p.args[0]+" car_state="+state(field(p.thisObject,"g"))+" workaround_applied=false attribution_unavailable=true");}catch(Throwable ignored){}}
   });
   event("observer_ready=true branch=qvk");
  }catch(Throwable t){event("observer_ready=false branch=qvk reason="+t.getClass().getSimpleName());}
  installTransport(cl);
 }
 private static void installTransport(ClassLoader cl){
  try{
   Class<?> endpoint=XposedHelpers.findClass("jfd",cl),frame=XposedHelpers.findClass("jef",cl);
   XposedHelpers.findAndHookMethod(endpoint,"C",long.class,frame,Runnable.class,new XC_MethodHook(){
    @Override protected void beforeHookedMethod(MethodHookParam p){try{p.setObjectExtra("aa.started",field(p.thisObject,"g"));}catch(Throwable ignored){}}
    @Override protected void afterHookedMethod(MethodHookParam p){observeTransport(p.thisObject,Boolean.TRUE.equals(p.getObjectExtra("aa.started")),false,p.hasThrowable());}
   });
   XposedHelpers.findAndHookMethod(endpoint,"a",int.class,java.nio.ByteBuffer.class,new XC_MethodHook(){
    @Override protected void afterHookedMethod(MethodHookParam p){if(Integer.valueOf(32773).equals(p.args[0]))observeTransport(p.thisObject,false,true,p.hasThrowable());}
   });
   XposedHelpers.findAndHookMethod(endpoint,"w",new XC_MethodHook(){
    @Override protected void afterHookedMethod(MethodHookParam p){try{if("jdz".equals(p.thisObject.getClass().getName())&&proof()!=null)event("event=audio_endpoint_stop started_after="+field(p.thisObject,"g"));}catch(Throwable ignored){}}
   });
   event("transport_observer_ready=true no_buffers_read=true ack_messages_not_playback_proof=true");
  }catch(Throwable t){event("transport_observer_ready=false reason="+t.getClass().getSimpleName());}
 }
 private static void observeTransport(Object endpoint,boolean started,boolean ack,boolean error){
  try{
   if(!"jdz".equals(endpoint.getClass().getName()))return;
   String p=proof();if(p==null)return;String session=NativeScoLease.session(p);long now=SystemClock.elapsedRealtime();
   synchronized(transports){
    Transport s=transports.get(endpoint);if(s==null||!session.equals(s.session)){if(transports.size()>=8)transports.clear();s=new Transport(now,session);transports.put(endpoint,s);}
    if(ack)s.ackMessages++;else{s.calls++;if(started)s.startedCalls++;}if(error)s.errors++;
    if(now-s.start<2000)return;
    event("event=audio_endpoint_window session="+session+" stream="+field(endpoint,"e")+" window_start_elapsed_ms="+s.start+" window_end_elapsed_ms="+now+" submit_calls="+s.calls+" started_at_submit_calls="+s.startedCalls+" ack_messages_received="+s.ackMessages+" errors="+s.errors+" started_now="+field(endpoint,"g")+" audible_unverified=true");
    transports.put(endpoint,new Transport(now,session));
   }
  }catch(Throwable ignored){}
 }
}
