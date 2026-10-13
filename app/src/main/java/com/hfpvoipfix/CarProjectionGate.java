package com.hfpvoipfix;
/** D6 projection must not create a second SCO; scope excludes all SIM/emergency/multiple calls. */
final class CarProjectionGate {
 static boolean allow(String mode,String device,String release,String marker,int count,boolean foreground,
  boolean emergency,int state,String pkg,String cls){
  return "telecom_aa".equals(mode)&&state>=1&&state<=5
   &&TelecomModeGate.allow(mode,device,release,marker,count,foreground,emergency,3,pkg,cls,3);
 }
}
