package com.hfpvoipfix;

import android.telecom.Connection;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HfpVoipFix implements IXposedHookLoadPackage {
    private static final String TAG = "HfpVoipFix";
    private static final String TARGET_PACKAGE = "com.android.bluetooth";
    private static final String HFP_CONNECTION =
            "com.android.bluetooth.hfpclient.connserv.HfpClientConnection";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        try {
            final Class<?> hfpConnectionClass = XposedHelpers.findClass(
                    HFP_CONNECTION,
                    lpparam.classLoader
            );

            XposedHelpers.findAndHookMethod(
                    Connection.class,
                    "setAudioModeIsVoip",
                    boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!hfpConnectionClass.isInstance(param.thisObject)) {
                                return;
                            }

                            boolean requested = (Boolean) param.args[0];
                            if (!requested) {
                                param.args[0] = true;
                                Log.i(TAG,
                                        "HFP Client: setAudioModeIsVoip(false) -> true");
                                XposedBridge.log(TAG + ": forced VoIP audio mode");
                            }
                        }
                    }
            );

            Log.i(TAG, "Hook installed");
        } catch (Throwable t) {
            Log.e(TAG, "Hook installation failed", t);
            XposedBridge.log(t);
        }
    }
}
