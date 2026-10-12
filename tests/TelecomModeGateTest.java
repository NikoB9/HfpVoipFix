package com.hfpvoipfix;
public final class TelecomModeGateTest {
 static String pkg="com.android.bluetooth",cls="com.android.bluetooth.hfpclient.connserv.HfpClientConnectionService";
 static boolean gate(String mode,String device,String release,String marker,int count,boolean fg,boolean emergency,int state,String p,String c,int requested){return TelecomModeGate.allow(mode,device,release,marker,count,fg,emergency,state,p,c,requested);}
 static void check(boolean v){if(!v)throw new AssertionError();}
 public static void main(String[] args){
  check(gate("telecom_duplex","begonia","11","1.7.22-one-boot",1,true,false,5,pkg,cls,3));
  check(gate("telecom_duplex","begonia","11","1.7.22-one-boot",1,true,false,3,pkg,cls,3));
  check(!gate("soft_duplex","begonia","11","1.7.22-one-boot",1,true,false,5,pkg,cls,3));
  check(!gate("telecom_duplex","other","11","1.7.22-one-boot",1,true,false,5,pkg,cls,3));
  check(!gate("telecom_duplex","begonia","12","1.7.22-one-boot",1,true,false,5,pkg,cls,3));
  check(!gate("telecom_duplex","begonia","11","",1,true,false,5,pkg,cls,3));
  check(!gate("telecom_duplex","begonia","11","1.7.22-one-boot",2,true,false,5,pkg,cls,3));
  check(!gate("telecom_duplex","begonia","11","1.7.22-one-boot",0,true,false,5,pkg,cls,3));
  check(!gate("telecom_duplex","begonia","11","1.7.22-one-boot",1,false,false,5,pkg,cls,3));
  check(!gate("telecom_duplex","begonia","11","1.7.22-one-boot",1,true,true,5,pkg,cls,3));
  for(int state:new int[]{0,1,2,4,6,7,8,9})check(!gate("telecom_duplex","begonia","11","1.7.22-one-boot",1,true,false,state,pkg,cls,3));
  check(!gate("telecom_duplex","begonia","11","1.7.22-one-boot",1,true,false,5,"com.android.phone",cls,3));
  check(!gate("telecom_duplex","begonia","11","1.7.22-one-boot",1,true,false,5,pkg,"OtherConnectionService",3));
  for(int mode:new int[]{0,1,2,4})check(!gate("telecom_duplex","begonia","11","1.7.22-one-boot",1,true,false,5,pkg,cls,mode));
  System.out.println("PASS Telecom mode gate: HFP only, device, native trial, single foreground, emergency and unrelated modes");
 }
}
