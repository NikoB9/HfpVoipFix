package com.hfpvoipfix;
public final class AaRelayPolicyTest {
 public static void main(String[] args){
  String s="version=1.9.10 requested=telecom_aa dirty=false master_connected=1 master_service=1 hfp_default=true network_at=10000 route=true calls=true disconnect=true voip=3";
  String proof=AaRelayPolicy.proof(s,12000);
  if(!proof.equals("v1:10000:1:1:1"))throw new AssertionError(proof);
  for(String[] c:new String[][]{{"telecom_aa","telecom_duplex"},{"dirty=false","dirty=true"},{"master_connected=1","master_connected=0"},{"master_service=1","master_service=0"},{"hfp_default=true","hfp_default=false"},{"route=true","route=false"},{"network_at=10000","network_at=0"},{"voip=3","voip=2"}})
   if(!AaRelayPolicy.proof(s.replace(c[0],c[1]),12000).equals("off"))throw new AssertionError(c[0]);
  for(long now:new long[]{0,9999,15000,16000})if(AaRelayPolicy.allow("telecom_aa","begonia","11","1.9.0-persistent",proof,now,1))throw new AssertionError("stale/future proof");
  if(!AaRelayPolicy.allow("telecom_aa","begonia","11","1.9.0-persistent",proof,12000,1))throw new AssertionError("valid");
  for(int state:new int[]{2,3,4,5,6,7,8,9,10,11})if(AaRelayPolicy.allow("telecom_aa","begonia","11","1.9.0-persistent",proof,12000,state))throw new AssertionError("local SIM state "+state);
  for(String bad:new String[]{"off","","v1:10000:0:1:1","v1:10000:1:1:0","v1:bad:1:1:1"})if(AaRelayPolicy.allow("telecom_aa","begonia","11","1.9.0-persistent",bad,12000,1))throw new AssertionError(bad);
  if(AaRelayPolicy.allow("telecom_duplex","begonia","11","1.9.0-persistent",proof,12000,1)||AaRelayPolicy.allow("telecom_aa","other","11","1.9.0-persistent",proof,12000,1))throw new AssertionError("scope");
  System.out.println("PASS AA readiness: freshness, connected master, real network, selected HFP account, D5 and SIM lock guards");
 }
}
