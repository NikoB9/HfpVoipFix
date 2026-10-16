package com.hfpvoipfix;
import android.media.AudioManager;
import android.util.Log;
import java.util.Collection;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedBridge;

/** Scoped HFP audio-mode translation; D6 delegates playback focus to the projected player. */
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
            // Android 11's call focus is locked. A projected player cannot acquire
            // focus underneath it, even when Telecom's audio mode is NORMAL.
            try{XposedHelpers.findAndHookMethod(AudioManager.class,"requestAudioFocusForCall",int.class,int.class,new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    Object machine=focusOwner.get();
                    if(machine==null||!"telecom_aa".equals(prop(LabModes.PROP)))return;
                    try{
                        if(Integer.valueOf(AudioManager.STREAM_VOICE_CALL).equals(p.args[0])&&eligible(machine,3)){
                            // An incoming call may still own the previous ringing lock.
                            XposedHelpers.callMethod(p.thisObject,"abandonAudioFocusForCall");
                            p.setResult(null);event("D6_FOCUS call_lock=suppressed projection_player_must_request=true");
                        }
                    }catch(Throwable t){event("D6_FOCUS unavailable="+t.getClass().getSimpleName());}
                }
            });}catch(Throwable t){event("D6_FOCUS hook_ready=false reason="+t.getClass().getSimpleName());}
            XposedHelpers.findAndHookMethod(AudioManager.class,"setMode",int.class,new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    Object machine=focusOwner.get();if(machine==null||!Integer.valueOf(3).equals(p.args[0]))return;
                    try{if(eligible(machine,(Integer)p.args[0])){p.args[0]=AudioManager.MODE_NORMAL;event("OVERRIDE requested=3 applied=0 account=HFP single_call=true");}}
                    catch(Throwable t){event("UNAVAILABLE reason="+t.getClass().getSimpleName());}
                }
            });event("HOOK_READY version=1.9.11");CarProjectionHook.install(loader);
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
