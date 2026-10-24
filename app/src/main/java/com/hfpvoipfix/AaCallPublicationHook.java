package com.hfpvoipfix;
import java.util.*;
import java.lang.reflect.Field;
import android.util.Log;
import de.robv.android.xposed.*;
/** Exact AA only: a filtered view for HU phone-status serialization, never for the AA UI. */
final class AaCallPublicationHook {
 private static boolean installed;private static volatile boolean ready,publicationExposed;
 private static final ThreadLocal<Integer> depth=new ThreadLocal<>();
 private static String prop(String k){return (String)XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.os.SystemProperties",null),"get",k,"");}
 private static Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 private static boolean allowed(){return CallPublicationPolicy.environment(prop(LabModes.PROP),prop("ro.product.device"),prop("ro.build.version.release"),prop("debug.hfpvoipfix.native_split"));}
 static synchronized void install(ClassLoader cl,String path){
  if(installed)return;installed=true;
  if(!AaCarFocusHook.exactApk(path)){event("apk_mismatch");return;}installVerified(cl);
 }
 static void installVerified(ClassLoader cl){
  try{
   Class<?> notifier=XposedHelpers.findClass("xjd",cl),calls=XposedHelpers.findClass("ojc",cl);
   XposedHelpers.findAndHookMethod(notifier,"aZ",new XC_MethodHook(){
    @Override protected void beforeHookedMethod(MethodHookParam p){Integer n=depth.get();p.setObjectExtra("hfp.publication.depth",n);depth.set(n==null?1:n+1);}
    @Override protected void afterHookedMethod(MethodHookParam p){Integer n=(Integer)p.getObjectExtra("hfp.publication.depth");if(n==null)depth.remove();else depth.set(n);}
   });
   XposedHelpers.findAndHookMethod(calls,"k",new XC_MethodHook(){
    @Override protected void afterHookedMethod(MethodHookParam p){
     if(!ready||depth.get()==null||p.hasThrowable()||!(p.getResult() instanceof List)||!allowed())return;
     try{
      List<?> list=(List<?>)p.getResult();if(list.isEmpty()){publicationExposed=false;return;}if(list.size()!=1){publicationExposed=true;return;}
      Object call=list.get(0),details=field(call,"f"),account=field(details,"h");if(account==null){publicationExposed=true;return;}
      Object component=XposedHelpers.callMethod(account,"getComponentName");
      if(component==null||!CallPublicationPolicy.account((String)XposedHelpers.callMethod(component,"getPackageName"),(String)XposedHelpers.callMethod(component,"getClassName"))){publicationExposed=true;return;}
      if(publicationExposed)return;
      // The caller receives a separate publication list. Do not mutate the source list or CarCall.
      p.setResult(new ArrayList<Object>());event("phone_status_entry_excluded");
     }catch(Throwable t){publicationExposed=true;event("metadata_unavailable");}
    }
   });ready=true;event("hook_ready");
  }catch(Throwable t){event("hook_unavailable_"+t.getClass().getSimpleName());}
 }
 private static String last="";private static long at;
 private static synchronized void event(String kind){long now=android.os.SystemClock.elapsedRealtime();if(kind.equals(last)&&now-at<2000)return;last=kind;at=now;Log.i("HfpVoipFix","LAB D6_CALL_PUBLICATION path=aa_phone_status event="+kind+" ui_calls_unchanged=true");}
}
