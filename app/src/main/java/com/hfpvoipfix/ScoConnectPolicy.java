package com.hfpvoipfix;
/** Bounded audio-transfer requests for one active master call; never interrupts an open SCO. */
final class ScoConnectPolicy {
    private Object call;
    private long next;
    private int attempts;
    boolean request(Object key,boolean active,boolean missing,long now){
        if(!active||key==null){call=null;attempts=0;next=0;return false;}
        if(!key.equals(call)){call=key;attempts=0;next=now+2000;}
        if(!missing||attempts>=3||now<next)return false;
        attempts++;next=now+5000;return true;
    }
    int attempts(){return attempts;}
}
