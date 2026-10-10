package com.hfpvoipfix;
public final class RxGateTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        String p="STATUS version=1.7.9 epoch=42 sco_seq=2 requested=soft_rx applied=soft_rx sco=1 rate=16000 bypass_ok=true soft_ports=true dirty=false disconnect=true route=true calls=true";
        check(RxGate.ready(p));check(RxGate.ready(p.replace("16000","8000")));
        String tx=p.replace("soft_rx","soft_tx");check(RxGate.ready(tx));check(!RxGate.ready(p.replace("applied=soft_rx","applied=soft_tx")));
        for(String[] change:new String[][]{{"1.7.9","1.5.1"},{"sco=1","sco=0"},{"sco=1","sco=2"},{"16000","0"},{"16000","48000"},{"bypass_ok=true","bypass_ok=false"},{"soft_ports=true","soft_ports=false"},{"dirty=false","dirty=true"},{"applied=soft_rx","applied=bypass"},{"disconnect=true","disconnect=false"},{"route=true","route=false"}})check(!RxGate.ready(p.replace(change[0],change[1])));
        check(!RxGate.ready(""));check(RxGate.identity(p).equals("42:2"));check(!RxGate.identity(p).equals(RxGate.identity(p.replace("sco_seq=2","sco_seq=3"))));
        check(RxGate.fresh(5000,2000));check(!RxGate.fresh(6000,2000));check(!RxGate.fresh(5000,0));check(!RxGate.fresh(5000,6000));
        System.out.println("RX gate: mode, SCO lifecycle, bypass, ports, rate and stale handshake tests passed");
    }
}
