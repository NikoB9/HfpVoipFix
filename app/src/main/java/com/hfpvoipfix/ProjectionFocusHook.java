package com.hfpvoipfix;

import android.media.AudioAttributes;
import android.os.SystemClock;
import android.util.Log;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import de.robv.android.xposed.*;

/** Android 11 system-server only. Do not fabricate a focus grant or replace AA's request. */
final class ProjectionFocusHook {
 private static boolean installed;
 private static volatile boolean enabled;
 private static Object focusLock;
 // Accessed only under the framework's own mAudioFocusLock.
 private static final Map<Object,Shared> shared=new IdentityHashMap<>();
 private static boolean restoring;
 private static String lastError="";
 private static final class Shared {
  final Object loser,winner,controller;final String session;
  Shared(Object l,Object w,Object c,String s){loser=l;winner=w;controller=c;session=s;}
 }
 private static String prop(String key){return (String)XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.os.SystemProperties",null),"get",key,"");}
 private static void event(String line){Log.i("HfpVoipFix","LAB D6_FOCUS_PAIR "+line);}
 private static String active(){
  String proof=prop(NativeScoLease.PROP);long now=SystemClock.elapsedRealtime();
  return NativeScoLease.allow(prop(LabModes.PROP),prop("ro.product.device"),prop("ro.build.version.release"),
    prop("debug.hfpvoipfix.native_split"),proof,now)&&NativeScoGuard.projectionReady(proof,now)?NativeScoLease.session(proof):"none";
 }
 static synchronized void install(ClassLoader loader){
  if(installed)return;
  final Class<?> requester,control;
  try{requester=XposedHelpers.findClass("com.android.server.audio.FocusRequester",loader);
      control=XposedHelpers.findClass("com.android.server.audio.MediaFocusControl",loader);}
  catch(Throwable absent){return;}
  installed=true;
  try{
   // Resolve the exact Android 11 contract before installing anything.
   for(String field:new String[]{"mPackageName","mAttributes","mFocusGainRequest","mGrantFlags","mFocusLossReceived","mFocusController"})requester.getDeclaredField(field);
   requester.getDeclaredMethod("handleFocusLoss",int.class,requester,boolean.class);
   control.getDeclaredField("mFocusStack");
   java.lang.reflect.Field lock=control.getDeclaredField("mAudioFocusLock");lock.setAccessible(true);focusLock=lock.get(null);
   if(focusLock==null)throw new IllegalStateException("missing_focus_lock");
   XposedHelpers.findAndHookMethod(requester,"handleFocusLoss",int.class,requester,boolean.class,new XC_MethodHook(){
    @Override protected void beforeHookedMethod(MethodHookParam p){
     if(!enabled||restoring||p.args[1]==null)return;
     try{
      Object loser=p.thisObject,winner=p.args[1];
      String lp=(String)XposedHelpers.getObjectField(loser,"mPackageName");if(!"com.hfpvoipfix".equals(lp))return;
      String wp=(String)XposedHelpers.getObjectField(winner,"mPackageName");
      AudioAttributes la=(AudioAttributes)XposedHelpers.getObjectField(loser,"mAttributes"),wa=(AudioAttributes)XposedHelpers.getObjectField(winner,"mAttributes");
      int gain=(Integer)XposedHelpers.getObjectField(winner,"mFocusGainRequest"),flags=(Integer)XposedHelpers.getObjectField(winner,"mGrantFlags");
      Object controller=XposedHelpers.getObjectField(loser,"mFocusController");
      String session=active();
      boolean allow=controller!=null&&controller==XposedHelpers.getObjectField(winner,"mFocusController")&&ProjectionFocusGate.allow(!"none".equals(session),lp,la.getUsage(),la.getContentType(),
       (Integer)XposedHelpers.getObjectField(loser,"mFocusGainRequest"),(Integer)XposedHelpers.getObjectField(loser,"mFocusLossReceived"),
       wp,wa.getUsage(),gain,flags,(Integer)p.args[0],Boolean.TRUE.equals(p.args[2]));
      event("winner="+wp+" usage="+wa.getUsage()+" content="+wa.getContentType()+" gain="+gain+" loss="+p.args[0]+" live_transport="+!"none".equals(session)+" coexist="+allow);
      if(!allow)return;
      synchronized(focusLock){
       if(shared.size()>=4&&!shared.containsKey(loser))return;
       shared.put(loser,new Shared(loser,winner,controller,session));p.setResult(null);
      }
     }catch(Throwable t){event("unavailable="+t.getClass().getSimpleName());}
    }
   });
   Timer timer=new Timer("HfpLab-projection-focus",true);
   timer.schedule(new TimerTask(){public void run(){poll();}},500,500);
   enabled=true;
   event("hook_ready=true version=1.9.11 policy=bounded_AA_transient_coexistence");
  }catch(Throwable t){event("hook_ready=false reason="+t.getClass().getSimpleName());}
 }
 static void poll(){
  if(focusLock==null)return;
  try{
   synchronized(focusLock){
    if(shared.isEmpty())return;
    String session=active();
    Iterator<Shared> entries=shared.values().iterator();
    while(entries.hasNext()){
     Shared s=entries.next();
     Object value=XposedHelpers.getObjectField(s.controller,"mFocusStack");
     if(!(value instanceof Stack))throw new IllegalStateException("focus_stack_contract");
     Stack<?> stack=(Stack<?>)value;
     // Original abandon/death/focus-loss handling owns removal and other winners.
     if(!stack.contains(s.loser)||!stack.contains(s.winner)||stack.empty()||stack.peek()!=s.winner
        ||!Integer.valueOf(0).equals(XposedHelpers.getObjectField(s.loser,"mFocusLossReceived"))){entries.remove();continue;}
     if(s.session.equals(session))continue;
     restoring=true;
     try{XposedHelpers.callMethod(s.loser,"handleFocusLoss",-2,s.winner,false);event("coexist=false reason=transport_or_session_ended normal_loss_restored=true");entries.remove();}
     finally{restoring=false;}
    }
    lastError="";
   }
  }catch(Throwable t){String e=t.getClass().getSimpleName();if(!e.equals(lastError)){lastError=e;event("cleanup_unavailable="+e);}}
 }
}
