package com.hfpvoipfix;
/** A relay call can stay in Telecom while being excluded from a vehicle publication. */
final class CallPublicationPolicy {
 static boolean environment(String mode,String device,String release,String patch){
  return LabModes.controlled(mode)&&"begonia".equals(device)&&"11".equals(release)&&"1.9.0-persistent".equals(patch);
 }
 static boolean account(String pkg,String cls){return HfpAccountPolicy.matches(pkg,cls);}
}
