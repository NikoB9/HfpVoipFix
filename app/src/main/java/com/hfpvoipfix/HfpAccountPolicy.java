package com.hfpvoipfix;

/** Restricts automatic outgoing selection to Android's native HFP Client account. */
final class HfpAccountPolicy {
    static boolean matches(String packageName,String className){
        return "com.android.bluetooth".equals(packageName)
            &&"com.android.bluetooth.hfpclient.connserv.HfpClientConnectionService".equals(className);
    }
}
