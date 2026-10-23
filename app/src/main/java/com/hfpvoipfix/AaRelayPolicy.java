package com.hfpvoipfix;
import java.util.Map;
/** A proxy readiness lease, not a change to the Redmi's telephony service. */
final class AaRelayPolicy {
 static final String PROP="debug.hfpvoipfix.aa_ready";
 static boolean fresh(long now,long at){return at>0&&now>=at&&now-at<5000;}
 static String proof(String status,long now){
  Map<String,String> v=RxGate.parse(status);
  try{long at=Long.parseLong(v.get("network_at"));
   if("1.9.15".equals(v.get("version"))&&"telecom_aa".equals(v.get("requested"))&&"false".equals(v.get("dirty"))
    &&"1".equals(v.get("master_connected"))&&"1".equals(v.get("master_service"))&&"true".equals(v.get("hfp_default"))
    &&"true".equals(v.get("route"))&&"true".equals(v.get("calls"))&&"true".equals(v.get("disconnect"))&&"3".equals(v.get("voip"))&&fresh(now,at))return "v1:"+at+":1:1:1";
  }catch(RuntimeException ignored){}return "off";
 }
 static boolean allow(String mode,String device,String release,String nativePatch,String proof,long now,int original){
  if(!"telecom_aa".equals(mode)||!"begonia".equals(device)||!"11".equals(release)||(original!=0&&original!=1))return false;
  if(!"1.9.0-persistent".equals(nativePatch)&&!"1.7.22-one-boot".equals(nativePatch))return false;
  String[] p=proof.split(":",-1);if(p.length!=5||!"v1".equals(p[0])||!"1".equals(p[2])||!"1".equals(p[3])||!"1".equals(p[4]))return false;
  try{return fresh(now,Long.parseLong(p[1]));}catch(RuntimeException e){return false;}
 }
}
