package com.hfpvoipfix;

import android.media.AudioSystem;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Experimental MediaTek HFP Client bridge.
 *
 * Note: android.media.AudioSystem is a hidden platform API. A normal Gradle
 * rebuild needs platform/hidden-API stubs in addition to the public SDK.
 */
public class HfpVoipFix implements IXposedHookLoadPackage {
    private static final String TAG = "HfpVoipFix";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.android.bluetooth".equals(lpparam.packageName)) {
            return;
        }

        Class<?> stateMachine = XposedHelpers.findClass(
                "com.android.bluetooth.hfpclient.HeadsetClientStateMachine",
                lpparam.classLoader);

        XposedHelpers.findAndHookMethod(
                stateMachine,
                "routeHfpAudio",
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        boolean enable = (Boolean) param.args[0];

                        AudioSystem.setParameters(enable ? "bt_wbs=on" : "bt_wbs=off");

                        Log.i(TAG, enable
                                ? "routeHfpAudio(true): bt_wbs=on"
                                : "routeHfpAudio(false): bt_wbs=off");
                    }
                });

        Log.i(TAG, "MTK bt_wbs bridge hook installed");
    }
}
