package com.hfpvoipfix;
import java.util.Collection;
import android.util.Log;
import de.robv.android.xposed.*;
/** D6: use Telecom's route API, not OEM-dependent state-machine message fields. */
final class CarProjectionHook {
 private static String prop(String key){return (String)XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.os.SystemProperties",null),"get",key,"");}
 static void install(ClassLoader loader){
  try{
   Class<?> machine=XposedHelpers.findClass("com.android.server.telecom.CallAudioRouteStateMachine",loader);
   XposedHelpers.findAndHookMethod(machine,"setBluetoothOn",String.class,new XC_MethodHook(){
    @Override protected void beforeHookedMethod(MethodHookParam p){
     if(!LabModes.managedCar(prop(LabModes.PROP)))return;
     String stage="audio_manager";
     try{
      Object manager=XposedHelpers.getObjectField(p.thisObject,"mCallAudioManager");
      stage="tracked_calls";
      Object calls=XposedHelpers.callMethod(manager,"getTrackedCalls");
      if(!(calls instanceof Collection)||((Collection<?>)calls).size()!=1)return;
      Object call=((Collection<?>)calls).iterator().next();
      stage="account";
      Object account=XposedHelpers.callMethod(call,"getTargetPhoneAccount");if(account==null)return;
      Object component=XposedHelpers.callMethod(account,"getComponentName");if(component==null)return;
      stage="eligibility";
      boolean eligible=CarProjectionGate.allow(prop(LabModes.PROP),prop("ro.product.device"),prop("ro.build.version.release"),prop("debug.hfpvoipfix.native_split"),1,
       call==XposedHelpers.callMethod(manager,"getForegroundCall"),(Boolean)XposedHelpers.callMethod(call,"isEmergencyCall"),
       (Integer)XposedHelpers.callMethod(call,"getState"),(String)XposedHelpers.callMethod(component,"getPackageName"),(String)XposedHelpers.callMethod(component,"getClassName"));
      if(!eligible)return;
      stage="set_audio_route";
      // Android 11 CallAudioState.ROUTE_EARPIECE=1. Telecom resolves its own internal message.
      XposedHelpers.callMethod(manager,"setAudioRoute",1,(Object)null);
      p.setResult(null);Log.i("HfpVoipFix","LAB D6_PROJECTION vehicle_sco_request=suppressed route_request=earpiece transport_confirmed=false");
     }catch(Throwable t){Log.i("HfpVoipFix","LAB D6_PROJECTION unavailable="+t.getClass().getSimpleName()+" stage="+stage);}
    }
   });Log.i("HfpVoipFix","LAB D6_PROJECTION hook_ready=true route_api=CallAudioManager.setAudioRoute");
  }catch(Throwable t){Log.i("HfpVoipFix","LAB D6_PROJECTION hook_ready=false reason="+t.getClass().getSimpleName());}
 }
}
