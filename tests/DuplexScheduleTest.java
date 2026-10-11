package com.hfpvoipfix;
public final class DuplexScheduleTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] a){
        check(DuplexSchedule.next(0,0,false)==1);
        check(DuplexSchedule.next(1,999,false)==1);
        check(DuplexSchedule.next(1,1000,false)==2);
        check(DuplexSchedule.next(2,1000,false)==3);
        check(DuplexSchedule.next(3,15000,false)==3);
        check(DuplexSchedule.next(3,3600000,false)==3);
        check(DuplexSchedule.next(1,14999,true)==1);
        check(DuplexSchedule.next(1,15000,true)==2);
        check(DuplexSchedule.next(3,15000,true)==4);
        check(DuplexSchedule.next(4,15000,true)==5);
        System.out.println("PASS continuous duplex and diagnostic scheduling");
    }
}
