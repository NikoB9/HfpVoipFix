package com.hfpvoipfix;
import java.util.Collection;
import android.util.Log;
import de.robv.android.xposed.*;
/** Exclude only a sole non-emergency HFP relay from AG publication, never from Telecom/UI. */
final class VehicleCallPublicationHook {
 private static boolean installed;private static volatile boolean ready;private static boolean publicationExposed;
 private static String prop(String k){return (String)XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.os.SystemProperties",null),"get",k,"");}
 private static boolean owned(Object call){
  if(call==null||Boolean.TRUE.equals(XposedHelpers.callMethod(call,"isEmergencyCall")))return false;
  Object a=XposedHelpers.callMethod(call,"getTargetPhoneAccount");if(a==null)return false;
  Object c=XposedHelpers.callMethod(a,"getComponentName");
  return c!=null&&CallPublicationPolicy.account((String)XposedHelpers.callMethod(c,"getPackageName"),(String)XposedHelpers.callMethod(c,"getClassName"));
 }
 private static synchronized boolean sole(Object service,Object expected){
  try{
   if(!ready||!CallPublicationPolicy.environment(prop(LabModes.PROP),prop("ro.product.device"),prop("ro.build.version.release"),prop("debug.hfpvoipfix.native_split")))return false;
   Object manager=XposedHelpers.getObjectField(service,"mCallsManager");
   Object list=XposedHelpers.callMethod(manager,"getCalls");
   if(!(list instanceof Collection))return false;
   Collection<?> calls=(Collection<?>)list;
   if(calls.isEmpty()){publicationExposed=false;return false;}
   if(calls.size()!=1){publicationExposed=true;return false;}
   Object call=calls.iterator().next();
   if(!owned(call)){publicationExposed=true;return false;}
   return !publicationExposed&&(expected==null||expected==call);
  }catch(Throwable unavailable){return false;}
 }
 static synchronized void install(ClassLoader cl){
  if(installed)return;
  try{
   Class<?> service=XposedHelpers.findClass("com.android.server.telecom.BluetoothPhoneServiceImpl",cl);
   Class<?> call=XposedHelpers.findClass("com.android.server.telecom.Call",cl);
   XposedHelpers.findAndHookMethod(service,"updateHeadsetWithCallState",boolean.class,new XC_MethodHook(){
    @Override protected void beforeHookedMethod(MethodHookParam p){if(sole(p.thisObject,null)){p.setResult(null);event("aggregate_excluded");}}
   });
   XposedHelpers.findAndHookMethod(service,"sendClccForCall",call,boolean.class,new XC_MethodHook(){
    @Override protected void beforeHookedMethod(MethodHookParam p){if(sole(p.thisObject,p.args[0])){p.setResult(null);event("clcc_entry_excluded");}}
   });
   ready=true;installed=true;event("hook_ready");
  }catch(Throwable t){Log.i("HfpVoipFix","LAB D6_CALL_PUBLICATION ag_hook_unavailable="+t.getClass().getSimpleName());}
 }
 private static String last="";private static long at;
 private static synchronized void event(String kind){long now=android.os.SystemClock.elapsedRealtime();if(kind.equals(last)&&now-at<2000)return;last=kind;at=now;Log.i("HfpVoipFix","LAB D6_CALL_PUBLICATION path=vehicle_hfp event="+kind+" telecom_call_unchanged=true");}
}
