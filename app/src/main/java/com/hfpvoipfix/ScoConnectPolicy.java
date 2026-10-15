package com.hfpvoipfix;
/** D6 master: three real connect requests and one cancellation of a stuck pending request. */
final class ScoConnectPolicy {
    static final int WAIT=0,CONNECT=1,CANCEL_PENDING=2;
    private Object call;
    private long next,pendingSince=-1;
    private int attempts;
    private boolean cancelled;
    int action(Object key,boolean active,boolean missing,int state,long now){
        if(!active||key==null){call=null;attempts=0;next=0;pendingSince=-1;cancelled=false;return WAIT;}
        if(!key.equals(call)){call=key;attempts=0;next=now+2000;pendingSince=-1;cancelled=false;}
        if(!missing){pendingSince=-1;return WAIT;}
        if(state==1){
            if(pendingSince<0)pendingSince=now;
            if(!cancelled&&now-pendingSince>=12000&&now>=next){cancelled=true;next=now+5000;return CANCEL_PENDING;}
            return WAIT;
        }
        pendingSince=-1;
        // CONNECTED without owned ports is diagnostic-only; never tear down an open channel.
        if(state!=0||attempts>=3||now<next)return WAIT;
        attempts++;next=now+5000;return CONNECT;
    }
    int attempts(){return attempts;}
}
