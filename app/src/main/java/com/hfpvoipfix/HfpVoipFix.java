package com.hfpvoipfix;

import android.media.AudioManager;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HfpVoipFix implements IXposedHookLoadPackage {
    private static final String TAG = "HfpVoipFix";
    private static final String BT_PACKAGE = "com.android.bluetooth";
    private static final String HFP_CONNECTION_SERVICE =
            "com.android.bluetooth.hfpclient.connserv.HfpClientConnectionService";

    private static final int OUT_SCO_HEADSET = 0x20;
    private static final int IN_SCO_HEADSET = 0x80000008;
    private static final int OUT_SPEAKER = 0x2;
    private static final int IN_BUILTIN_MIC = 0x80000004;

    private static Object sRxPatch;
    private static Object sTxPatch;
    private static boolean sTelecomForcedLogged;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            if (BT_PACKAGE.equals(lpparam.packageName)) {
                installBluetoothHooks(lpparam);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to install hooks in " + lpparam.packageName, t);
        }
    }

    private static void installBluetoothHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> sm = XposedHelpers.findClass(
                "com.android.bluetooth.hfpclient.HeadsetClientStateMachine",
                lpparam.classLoader);
        XposedHelpers.findAndHookMethod(sm, "routeHfpAudio", boolean.class, new RouteHook());

        Class<?> service = XposedHelpers.findClass(HFP_CONNECTION_SERVICE, lpparam.classLoader);
        Class<?> phoneAccountHandle = XposedHelpers.findClass("android.telecom.PhoneAccountHandle", null);
        Class<?> connectionRequest = XposedHelpers.findClass("android.telecom.ConnectionRequest", null);
        XC_MethodHook earlyVoip = new EarlyConnectionVoipHook();
        XposedHelpers.findAndHookMethod(service, "onCreateIncomingConnection",
                phoneAccountHandle, connectionRequest, earlyVoip);
        XposedHelpers.findAndHookMethod(service, "onCreateOutgoingConnection",
                phoneAccountHandle, connectionRequest, earlyVoip);
        XposedHelpers.findAndHookMethod(service, "onCreateUnknownConnection",
                phoneAccountHandle, connectionRequest, earlyVoip);

        Log.i(TAG, "MTK v1.3.1 Bluetooth hooks installed (early Connection VoIP + SCO bridge)");
    }

    private static Class<?> audioSystemClass() {
        return XposedHelpers.findClass("android.media.AudioSystem", null);
    }

    private static Class<?> audioPortConfigClass() {
        return XposedHelpers.findClass("android.media.AudioPortConfig", null);
    }

    private static Class<?> audioPatchClass() {
        return XposedHelpers.findClass("android.media.AudioPatch", null);
    }

    private static int asInt(Object value) {
        return value instanceof Integer ? ((Integer) value).intValue() : -1;
    }

    private static void logStatus(String prefix, int status) {
        Log.i(TAG, prefix + status);
    }

    private static void setParameters(String value) {
        XposedHelpers.callStaticMethod(audioSystemClass(), "setParameters", value);
    }

    public static void setScoState(Object stateMachine, boolean available) {
        try {
            Object dev = XposedHelpers.getObjectField(stateMachine, "mCurrentDevice");
            String addr = "";
            if (dev != null) {
                Object a = XposedHelpers.callMethod(dev, "getAddress");
                if (a instanceof String) addr = (String) a;
            }

            int state = available ? 1 : 0;
            int out = asInt(XposedHelpers.callStaticMethod(
                    audioSystemClass(),
                    "setDeviceConnectionState",
                    OUT_SCO_HEADSET, state, addr, "HfpClient SCO", 0));
            int in = asInt(XposedHelpers.callStaticMethod(
                    audioSystemClass(),
                    "setDeviceConnectionState",
                    IN_SCO_HEADSET, state, addr, "HfpClient SCO", 0));

            logStatus("SCO OUT setDeviceConnectionState status=", out);
            logStatus("SCO IN setDeviceConnectionState status=", in);
        } catch (Throwable t) {
            Log.e(TAG, "setScoState failed", t);
        }
    }

    private static Object findPort(ArrayList<Object> ports, int type) {
        for (Object p : ports) {
            try {
                if (asInt(XposedHelpers.callMethod(p, "type")) == type) return p;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object oneElementArray(String className, Object value) {
        Class<?> c = XposedHelpers.findClass(className, null);
        Object array = Array.newInstance(c, 1);
        Array.set(array, 0, value);
        return array;
    }

    public static void createPatches() {
        try {
            releasePatches();

            ArrayList<Object> ports = new ArrayList<>();
            int status = asInt(XposedHelpers.callStaticMethod(
                    AudioManager.class, "listAudioDevicePorts", ports));
            logStatus("listAudioDevicePorts status=", status);
            if (status != 0) return;

            Object scoIn = findPort(ports, IN_SCO_HEADSET);
            Object scoOut = findPort(ports, OUT_SCO_HEADSET);
            Object speaker = findPort(ports, OUT_SPEAKER);
            Object mic = findPort(ports, IN_BUILTIN_MIC);

            if (scoIn == null || scoOut == null || speaker == null || mic == null) {
                Log.i(TAG, "missing required audio device port(s)"
                        + " scoIn=" + (scoIn != null)
                        + " scoOut=" + (scoOut != null)
                        + " speaker=" + (speaker != null)
                        + " mic=" + (mic != null));
                return;
            }

            Object rxSourceConfig = XposedHelpers.callMethod(scoIn, "buildConfig", 0, 0, 0, null);
            Object rxSinkConfig = XposedHelpers.callMethod(speaker, "buildConfig", 0, 0, 0, null);
            Object rxPatches = Array.newInstance(audioPatchClass(), 1);
            Object rxSources = oneElementArray("android.media.AudioPortConfig", rxSourceConfig);
            Object rxSinks = oneElementArray("android.media.AudioPortConfig", rxSinkConfig);

            status = asInt(XposedHelpers.callStaticMethod(
                    AudioManager.class, "createAudioPatch", rxPatches, rxSources, rxSinks));
            logStatus("patch SCO_IN -> SPEAKER status=", status);
            if (status == 0) sRxPatch = Array.get(rxPatches, 0);

            Object txSourceConfig = XposedHelpers.callMethod(mic, "buildConfig", 0, 0, 0, null);
            Object txSinkConfig = XposedHelpers.callMethod(scoOut, "buildConfig", 0, 0, 0, null);
            Object txPatches = Array.newInstance(audioPatchClass(), 1);
            Object txSources = oneElementArray("android.media.AudioPortConfig", txSourceConfig);
            Object txSinks = oneElementArray("android.media.AudioPortConfig", txSinkConfig);

            status = asInt(XposedHelpers.callStaticMethod(
                    AudioManager.class, "createAudioPatch", txPatches, txSources, txSinks));
            logStatus("patch MIC -> SCO_OUT status=", status);
            if (status == 0) sTxPatch = Array.get(txPatches, 0);
        } catch (Throwable t) {
            Log.e(TAG, "createPatches failed", t);
        }
    }

    public static void releasePatches() {
        try {
            if (sTxPatch != null) {
                int status = asInt(XposedHelpers.callStaticMethod(
                        AudioManager.class, "releaseAudioPatch", sTxPatch));
                logStatus("release TX patch status=", status);
                sTxPatch = null;
            }
            if (sRxPatch != null) {
                int status = asInt(XposedHelpers.callStaticMethod(
                        AudioManager.class, "releaseAudioPatch", sRxPatch));
                logStatus("release RX patch status=", status);
                sRxPatch = null;
            }
        } catch (Throwable t) {
            Log.e(TAG, "releasePatches failed", t);
            sTxPatch = null;
            sRxPatch = null;
        }
    }

    public static final class EarlyConnectionVoipHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            Object connection = param.getResult();
            if (connection == null) return;
            try {
                XposedHelpers.callMethod(connection, "setAudioModeIsVoip", true);
                Log.i(TAG, "Early HFP Connection VoIP=true before returning to Telecom");
            } catch (Throwable t) {
                Log.e(TAG, "Failed to set early HFP Connection VoIP mode", t);
            }
        }
    }

    public static final class RouteHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            boolean enable = (Boolean) param.args[0];
            if (enable) {
                setParameters("bt_wbs=on");
                setScoState(param.thisObject, true);
                Log.i(TAG, "routeHfpAudio(true): bt_wbs=on + SCO endpoints");
            } else {
                releasePatches();
                Log.i(TAG, "routeHfpAudio(false): patches released");
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            boolean enable = (Boolean) param.args[0];
            if (enable) {
                createPatches();
            } else {
                setScoState(param.thisObject, false);
                setParameters("bt_wbs=off");
                Log.i(TAG, "routeHfpAudio(false): SCO endpoints removed + bt_wbs=off");
            }
        }
    }


}
