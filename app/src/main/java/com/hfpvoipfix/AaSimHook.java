package com.hfpvoipfix;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import de.robv.android.xposed.*;
/** Installed ONLY in Android Auto. Never hooks Telecom, the dialer or the phone process. */
final class AaSimHook {
 private static boolean installed;
 private static long nextLog;
 private static String prop(String key){return (String)XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.os.SystemProperties",null),"get",key,"");}
 static synchronized void install(){if(installed)return;installed=true;
  try{
   XposedHelpers.findAndHookMethod(TelephonyManager.class,"getSimState",new XC_MethodHook(){
    @Override protected void afterHookedMethod(MethodHookParam p){
     if(p.hasThrowable()||!(p.getResult() instanceof Integer))return;
     try{int original=(Integer)p.getResult();long now=SystemClock.elapsedRealtime();
      boolean allow=AaRelayPolicy.allow(prop(LabModes.PROP),prop("ro.product.device"),prop("ro.build.version.release"),prop("debug.hfpvoipfix.native_split"),prop(AaRelayPolicy.PROP),now,original);
      if(allow)p.setResult(TelephonyManager.SIM_STATE_READY);
      if(LabModes.managedCar(prop(LabModes.PROP))&&now>=nextLog){nextLog=now+5000;
       Log.i("HfpVoipFix","LAB AA_SIM_READINESS original="+original+" proxy_applied="+allow+" local_telephony_unchanged=true");}
     }catch(Throwable t){Log.i("HfpVoipFix","LAB AA_SIM_READINESS unavailable="+t.getClass().getSimpleName());}
    }
   });Log.i("HfpVoipFix","LAB AA_SIM_READINESS hook_ready=true version=1.9.18");
  }catch(Throwable t){Log.i("HfpVoipFix","LAB AA_SIM_READINESS hook_ready=false reason="+t.getClass().getSimpleName());}
 }
}
