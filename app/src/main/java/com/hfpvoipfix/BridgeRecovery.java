package com.hfpvoipfix;
/** Keep the 4 s authorization window. Recovery is a silent wait, never stale forwarding. */
final class BridgeRecovery {
    static boolean waiting(long now,long received,boolean permitted,String expected,String current){
        return permitted&&received>0&&now>=received&&now-received<12000&&expected!=null&&expected.equals(current);
    }
}
