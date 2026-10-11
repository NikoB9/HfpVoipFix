package com.hfpvoipfix;
/** Preserve RX, MIC, TX opening order. Continuous calls hold duplex until teardown. */
final class DuplexSchedule {
    static int next(int phase,long elapsed,boolean progressive){
        if(phase==0)return 1;
        if(!progressive&&phase>=3)return 3;
        return elapsed>=(progressive?15000:1000)?phase+1:phase;
    }
}
