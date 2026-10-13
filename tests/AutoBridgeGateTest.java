package com.hfpvoipfix;
public class AutoBridgeGateTest {
    static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        String p="STATUS version=1.9.4 busy=false dirty=false requested=soft_duplex disconnect=true route=true calls=true bypass=true";
        check(AutoBridgeGate.prepare(p,"soft_duplex",0,true,false,true));
        check(!AutoBridgeGate.prepare(p,"soft_duplex",4000,true,false,true));
        check(!AutoBridgeGate.prepare(p,"soft_duplex",-1,true,false,true));
        check(!AutoBridgeGate.prepare(p,"soft_duplex",0,false,false,true));
        check(!AutoBridgeGate.prepare(p,"soft_duplex",0,true,true,true));
        check(!AutoBridgeGate.prepare(p,"soft_duplex",0,true,false,false));
        check(!AutoBridgeGate.prepare(p.replace("busy=false","busy=true"),"soft_duplex",0,true,false,true));
        check(!AutoBridgeGate.prepare(p.replace("1.9.4","1.7.19"),"soft_duplex",0,true,false,true));
        check(!AutoBridgeGate.prepare(p,"soft_rx",0,true,false,true));
        check(!AutoBridgeGate.prepare(p.replace("route=true","route=false"),"soft_duplex",0,true,false,true));
        check(!AutoBridgeGate.prepare(p.replace("dirty=false","dirty=true"),"soft_duplex",0,true,false,true));
        check(!AutoBridgeGate.prepare(p,"observe",0,true,false,true));
        System.out.println("PASS automatic preparation: fresh idle, mode, permission, explicit stop, lifecycle");
    }
}
