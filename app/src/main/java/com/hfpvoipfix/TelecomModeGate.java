package com.hfpvoipfix;
/** Restrict the experiment to one non-emergency HFP call on the validated Android 11 device. */
final class TelecomModeGate {
    // AOSP Android 11 CallState: DIALING=3, RINGING=4, ACTIVE=5 (not HFP call states).
    static boolean allow(String mode,String device,String release,String marker,int count,boolean foreground,
                         boolean emergency,int state,String accountPackage,String accountClass,int requested){
        return "telecom_duplex".equals(mode)&&"begonia".equals(device)&&"11".equals(release)
            &&("1.7.22-one-boot".equals(marker)||"1.9.0-persistent".equals(marker))&&count==1&&foreground&&!emergency&&(state==3||state==5)
            &&"com.android.bluetooth".equals(accountPackage)
            &&"com.android.bluetooth.hfpclient.connserv.HfpClientConnectionService".equals(accountClass)&&requested==3;
    }
}
