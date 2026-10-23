package com.hfpvoipfix;
public final class RxGateTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        check(LabModes.IDS.length==LabModes.NAMES.length&&LabModes.IDS.length==LabModes.DETAILS.length);
        check(LabModes.softwareBridge("soft_duplex"));
        String p="STATUS version=1.9.15 epoch=42 sco_seq=2 requested=soft_rx applied=soft_rx sco=1 rate=16000 bypass=true bypass_ok=true soft_ports=true dirty=false disconnect=true route=true calls=true busy=true";
        check(RxGate.ready(p));check(RxGate.ready(p.replace("16000","8000")));
        String d5=p.replace("soft_rx","telecom_duplex").replace("bypass_ok=true","bypass_ok=false")+" telecom_ok=true voip=3 call_active=true";
        check(LabModes.softwareBridge("telecom_duplex")&&!LabModes.bypass("telecom_duplex")&&LabModes.voip("telecom_duplex"));
        check(RxGate.ready(d5));check(!RxGate.ready(d5.replace("call_active=true","call_active=false")));check(!RxGate.ready(d5.replace(" call_active=true","")));check(!RxGate.ready(d5.replace("telecom_ok=true","telecom_ok=false")));
        String d5idle=d5.replace("busy=true","busy=false");check(RxGate.armError(d5idle,"telecom_duplex","telecom_duplex")==null);check(RxGate.armError(d5idle.replace("voip=3","voip=2"),"telecom_duplex","telecom_duplex")!=null);
        String tx=p.replace("soft_rx","soft_tx");check(RxGate.ready(tx));check(RxGate.ready(p.replace("soft_rx","soft_duplex")));check(!RxGate.ready(p.replace("applied=soft_rx","applied=soft_tx")));
        for(String[] change:new String[][]{{"1.9.15","1.5.1"},{"sco=1","sco=0"},{"sco=1","sco=2"},{"16000","0"},{"16000","48000"},{"bypass_ok=true","bypass_ok=false"},{"soft_ports=true","soft_ports=false"},{"dirty=false","dirty=true"},{"applied=soft_rx","applied=bypass"},{"disconnect=true","disconnect=false"},{"route=true","route=false"}})check(!RxGate.ready(p.replace(change[0],change[1])));
        String idle=p.replace("busy=true","busy=false").replace("sco=1","sco=0");
        check(RxGate.armError(idle,"soft_rx","soft_rx")==null);
        check(RxGate.armError(idle.replace("requested=soft_rx","requested=observe"),"soft_rx","soft_rx").contains("Appliquer"));
        check(RxGate.armError(idle,"soft_duplex","soft_rx").contains("Appliquer"));
        check(RxGate.armError(idle.replace("1.9.15","1.7.10"),"soft_rx","soft_rx").contains("redémarre"));
        check(RxGate.armError(idle.replace("busy=false","busy=true"),"soft_rx","soft_rx").contains("actif"));
        check(RxGate.armError(idle.replace("route=true","route=false"),"soft_rx","soft_rx").contains("hooks"));
        check(!RxGate.ready(""));check(RxGate.identity(p).equals("42:2"));check(!RxGate.identity(p).equals(RxGate.identity(p.replace("sco_seq=2","sco_seq=3"))));
        check(RxGate.fresh(5000,2000));check(!RxGate.fresh(6000,2000));check(!RxGate.fresh(5000,0));check(!RxGate.fresh(5000,6000));
        System.out.println("RX gate: mode, SCO lifecycle, bypass, ports, rate and stale handshake tests passed");
    }
}
