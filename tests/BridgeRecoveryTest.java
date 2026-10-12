package com.hfpvoipfix;
public final class BridgeRecoveryTest {
 public static void main(String[] args){
  if(RxGate.fresh(5000,1000))throw new AssertionError("stale forwarding");
  if(!BridgeRecovery.waiting(5000,1000,true,"a:1","a:1"))throw new AssertionError("brief failure must wait silently");
  if(!RxGate.fresh(5500,5400))throw new AssertionError("fresh recovery");
  if(BridgeRecovery.waiting(13000,1000,true,"a:1","a:1"))throw new AssertionError("bounded recovery");
  if(BridgeRecovery.waiting(5000,1000,true,"a:1","a:2"))throw new AssertionError("different SCO");
  if(BridgeRecovery.waiting(5000,1000,false,"a:1","a:1"))throw new AssertionError("revoked authorization");
  if(BridgeRecovery.waiting(5000,0,true,"a:1","a:1"))throw new AssertionError("no handshake");
  System.out.println("PASS silent bounded probe recovery; stale, different session and revoked forwarding stay blocked");
 }
}
