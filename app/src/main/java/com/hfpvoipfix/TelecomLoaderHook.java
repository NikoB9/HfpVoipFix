package com.hfpvoipfix;
import android.util.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** Observe only Telecom's APK loader inside a shared framework process. */
final class TelecomLoaderHook {
    private static boolean installed,telecomLoaderAttempted;
    private static void event(String s){String line="HfpVoipFix LAB TELECOM_LOADER "+s;Log.i("HfpVoipFix",line);XposedBridge.log(line);}
    static boolean accepts(String pkg){return "android".equals(pkg)||"system".equals(pkg)||"com.android.server.telecom".equals(pkg);}
    static synchronized void load(String pkg,ClassLoader loader){
        if(!accepts(pkg))return;
        event("ENTRY package="+pkg+" version=1.9.7");
        if("com.android.server.telecom".equals(pkg)){TelecomAudioHook.install(loader);return;}
        if(installed)return;installed=true;
        try{
            XposedHelpers.findAndHookMethod(XposedHelpers.findClass("android.app.LoadedApk",loader),"getClassLoader",new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){
                    if(p.hasThrowable()||!(p.getResult() instanceof ClassLoader))return;
                    try{if(!telecomLoaderAttempted&&"com.android.server.telecom".equals(XposedHelpers.getObjectField(p.thisObject,"mPackageName"))){telecomLoaderAttempted=true;
                        event("APK_LOADER package=com.android.server.telecom");TelecomAudioHook.install((ClassLoader)p.getResult());
                    }}catch(Throwable t){event("UNAVAILABLE loader="+t.getClass().getSimpleName());}
                }
            });event("WAITING_FOR_TELECOM_APK");
        }catch(Throwable t){event("UNAVAILABLE install="+t.getClass().getSimpleName());}
    }
}
