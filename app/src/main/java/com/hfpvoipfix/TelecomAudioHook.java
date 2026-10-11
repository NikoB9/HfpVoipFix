package com.hfpvoipfix;
import android.media.AudioManager;
import android.util.Log;
import java.util.Collection;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedBridge;

/** Telecom keeps call control/focus; only its scoped HFP VoIP setMode request is translated. */
final class TelecomAudioHook {
    private static final ThreadLocal<Object> focusOwner=new ThreadLocal<>();
    private static boolean installed;
    private static void event(String s){Log.i("HfpVoipFix","LAB TELECOM_AUDIO "+s);XposedBridge.log("HfpVoipFix LAB TELECOM_AUDIO "+s);}
    private static String prop(String name){return (String)XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.os.SystemProperties",null),"get",name,"");}
    static synchronized void install(ClassLoader loader){
        if(installed)return;installed=true;
        try{
            Class<?> machine=XposedHelpers.findClass("com.android.server.telecom.CallAudioModeStateMachine",loader);
            machine.getDeclaredField("mCallAudioManager");
            for(String state:new String[]{"VoipCallFocusState","OtherFocusState"}){
                Class<?> focus=XposedHelpers.findClass(machine.getName()+"$"+state,loader);focus.getDeclaredField("this$0");
                XposedHelpers.findAndHookMethod(focus,"enter",new XC_MethodHook(){
                    @Override protected void beforeHookedMethod(MethodHookParam p){
                        try{focusOwner.set(XposedHelpers.getObjectField(p.thisObject,"this$0"));}catch(Throwable t){focusOwner.remove();}
                    }
                    @Override protected void afterHookedMethod(MethodHookParam p){focusOwner.remove();}
                });
            }
            XposedHelpers.findAndHookMethod(AudioManager.class,"setMode",int.class,new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    Object machine=focusOwner.get();if(machine==null||!Integer.valueOf(3).equals(p.args[0]))return;
                    try{if(eligible(machine,(Integer)p.args[0])){p.args[0]=AudioManager.MODE_NORMAL;event("OVERRIDE requested=3 applied=0 account=HFP single_call=true");}}
                    catch(Throwable t){event("UNAVAILABLE reason="+t.getClass().getSimpleName());}
                }
            });event("HOOK_READY version=1.8.3");
        }catch(Throwable t){event("UNAVAILABLE install="+t.getClass().getSimpleName());}
    }
    private static boolean eligible(Object machine,int requested){
        Object manager=XposedHelpers.getObjectField(machine,"mCallAudioManager");
        Object tracked=XposedHelpers.callMethod(manager,"getTrackedCalls");
        if(!(tracked instanceof Collection)||((Collection<?>)tracked).size()!=1){event("SKIP reason=tracked_call_count");return false;}
        Object call=((Collection<?>)tracked).iterator().next();
        Object foreground=XposedHelpers.callMethod(manager,"getForegroundCall");
        Object account=XposedHelpers.callMethod(call,"getTargetPhoneAccount");if(account==null)return false;
        Object component=XposedHelpers.callMethod(account,"getComponentName");if(component==null)return false;
        boolean allowed=TelecomModeGate.allow(prop(LabModes.PROP),prop("ro.product.device"),prop("ro.build.version.release"),
            prop("debug.hfpvoipfix.native_split"),1,call==foreground,(Boolean)XposedHelpers.callMethod(call,"isEmergencyCall"),
            (Integer)XposedHelpers.callMethod(call,"getState"),(String)XposedHelpers.callMethod(component,"getPackageName"),
            (String)XposedHelpers.callMethod(component,"getClassName"),requested);
        if(!allowed)event("SKIP reason=gate mode="+prop(LabModes.PROP)+" state="+XposedHelpers.callMethod(call,"getState")+" foreground="+(call==foreground)+" account_package="+XposedHelpers.callMethod(component,"getPackageName")+" account_class="+XposedHelpers.callMethod(component,"getClassName"));
        return allowed;
    }
}
