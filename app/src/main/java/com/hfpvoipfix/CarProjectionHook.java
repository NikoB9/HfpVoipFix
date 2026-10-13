package com.hfpvoipfix;
import java.util.Collection;
import android.util.Log;
import de.robv.android.xposed.*;
/** Android 11 Telecom only: keep the local call route while D6 transports RX through projection. */
final class CarProjectionHook {
 private static String prop(String key){return (String)XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.os.SystemProperties",null),"get",key,"");}
 static void install(ClassLoader loader){
  try{
   Class<?> machine=XposedHelpers.findClass("com.android.server.telecom.CallAudioRouteStateMachine",loader);
   XposedHelpers.findAndHookMethod(machine,"setBluetoothOn",String.class,new XC_MethodHook(){
    @Override protected void beforeHookedMethod(MethodHookParam p){
     try{
      Object manager=XposedHelpers.getObjectField(p.thisObject,"mCallsManager");
      Object calls=XposedHelpers.callMethod(manager,"getCalls");
      if(!(calls instanceof Collection)||((Collection<?>)calls).size()!=1)return;
      Object call=((Collection<?>)calls).iterator().next();
      Object account=XposedHelpers.callMethod(call,"getTargetPhoneAccount");if(account==null)return;
      Object component=XposedHelpers.callMethod(account,"getComponentName");if(component==null)return;
      boolean eligible=CarProjectionGate.allow(prop(LabModes.PROP),prop("ro.product.device"),prop("ro.build.version.release"),prop("debug.hfpvoipfix.native_split"),1,
       call==XposedHelpers.callMethod(manager,"getForegroundCall"),(Boolean)XposedHelpers.callMethod(call,"isEmergencyCall"),
       (Integer)XposedHelpers.callMethod(call,"getState"),(String)XposedHelpers.callMethod(component,"getPackageName"),(String)XposedHelpers.callMethod(component,"getClassName"));
      if(!eligible)return;
      int earpiece=machine.getField("USER_SWITCH_EARPIECE").getInt(null);
      XposedHelpers.callMethod(p.thisObject,"sendInternalMessage",earpiece);
      p.setResult(null);Log.i("HfpVoipFix","LAB D6_PROJECTION vehicle_sco_request=suppressed local_call_route=earpiece");
     }catch(Throwable t){Log.i("HfpVoipFix","LAB D6_PROJECTION unavailable="+t.getClass().getSimpleName());}
    }
   });Log.i("HfpVoipFix","LAB D6_PROJECTION hook_ready=true");
  }catch(Throwable t){Log.i("HfpVoipFix","LAB D6_PROJECTION hook_ready=false reason="+t.getClass().getSimpleName());}
 }
}
