package com.hfpvoipfix;
public final class HfpAccountPolicyTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        check(HfpAccountPolicy.matches("com.android.bluetooth","com.android.bluetooth.hfpclient.connserv.HfpClientConnectionService"));
        check(!HfpAccountPolicy.matches("com.android.phone","com.android.bluetooth.hfpclient.connserv.HfpClientConnectionService"));
        check(!HfpAccountPolicy.matches("com.android.bluetooth","other.ConnectionService"));
        System.out.println("PASS only the native HFP Client account can become the automatic outgoing account");
    }
}
