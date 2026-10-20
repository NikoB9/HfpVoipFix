package com.hfpvoipfix;
public final class AaDiagnosticScheduleTest {
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    public static void main(String[] args){
        int[] mic={0,1,1,1,0,0,1,1},tx={0,0,1,0,0,0,0,1},usage={12,12,12,12,12,1,1,1};
        check(AaDiagnosticSchedule.next(0,0)==1,"start RX only");
        for(int p=1;p<=8;p++){
            check(AaDiagnosticSchedule.next(p,7999)==p,"eight seconds without transition");
            check(AaDiagnosticSchedule.next(p,8000)==p+1,"advance once");
            check(AaDiagnosticSchedule.microphone(p)==(mic[p-1]==1),"microphone phase "+p);
            check(AaDiagnosticSchedule.transmit(p)==(tx[p-1]==1),"TX phase "+p);
            check(AaDiagnosticSchedule.usage(p)==usage[p-1],"output phase "+p);
            check(!AaDiagnosticSchedule.transmit(p)||AaDiagnosticSchedule.microphone(p),"TX needs microphone");
            if(p>1){int changes=(mic[p-1]!=mic[p-2]?1:0)+(tx[p-1]!=tx[p-2]?1:0)+(usage[p-1]!=usage[p-2]?1:0);check(changes==1,"one endpoint choice changes at a time");}
        }
        check(AaDiagnosticSchedule.next(9,99999)==9,"terminal state");
        check(AaDiagnosticSchedule.canStart(true,false,"","42:1"),"first call");
        check(!AaDiagnosticSchedule.canStart(true,false,"42:1","42:1"),"do not restart same call on SCO reconnect");
        check(AaDiagnosticSchedule.canStart(true,false,"42:1","42:2"),"fresh call");
        check(!AaDiagnosticSchedule.canStart(false,false,"","42:1")&&!AaDiagnosticSchedule.canStart(true,true,"","42:1")&&!AaDiagnosticSchedule.canStart(true,false,"","42:-1"),"scope, exclusivity, valid call");
        check(DuplexSchedule.next(1,1000,false)==2&&DuplexSchedule.next(3,999999,false)==3,"D5 continuous schedule unchanged");
        System.out.println("PASS AA bounded causal schedule, rollback, output transition, per-call guard and D5 reference schedule");
    }
}
