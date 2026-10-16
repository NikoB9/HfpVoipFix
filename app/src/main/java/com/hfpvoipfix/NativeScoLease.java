package com.hfpvoipfix;
import java.util.Map;
/** Short-lived evidence of an already open D6 master transport, never permission to open one. */
final class NativeScoLease {
 static final String PROP="debug.hfpvoipfix.sco_lease";
 static boolean fresh(long now,long at){return at>0&&now>=at&&now-at<5000;}
 static String proof(String status,long now){
  Map<String,String> v=RxGate.parse(status);
  try{
   long at=Long.parseLong(v.get("status_at")),epoch=Long.parseLong(v.get("epoch")),seq=Long.parseLong(v.get("sco_seq"));
   if("1.9.11".equals(v.get("version"))&&"telecom_aa".equals(v.get("requested"))&&"telecom_aa".equals(v.get("applied"))
    &&"1".equals(v.get("call_count"))&&"true".equals(v.get("call_active"))&&"1".equals(v.get("sco"))
    &&"true".equals(v.get("native_sco"))&&"true".equals(v.get("soft_ports"))&&"true".equals(v.get("telecom_ok"))
    &&"false".equals(v.get("dirty"))&&Integer.parseInt(v.get("sco_in_id"))>=0&&Integer.parseInt(v.get("sco_out_id"))>=0
    &&epoch>0&&seq>0&&fresh(now,at))return "v1:"+at+":"+epoch+":"+seq+":"+v.get("sco_in_id")+":"+v.get("sco_out_id");
  }catch(RuntimeException ignored){}return "off";
 }
 static boolean allow(String mode,String device,String release,String patch,String proof,long now){
  if(!"telecom_aa".equals(mode)||!"begonia".equals(device)||!"11".equals(release)
   ||(!"1.9.0-persistent".equals(patch)&&!"1.7.22-one-boot".equals(patch)))return false;
  if(!proof.matches("v1:[0-9]{1,16}:[0-9]{1,16}:[0-9]{1,16}:[0-9]{1,8}:[0-9]{1,8}"))return false;
  String[] p=proof.split(":");try{return fresh(now,Long.parseLong(p[1]))&&Long.parseLong(p[2])>0&&Long.parseLong(p[3])>0;}catch(RuntimeException e){return false;}
 }
 static String session(String proof){String[] p=proof.split(":");return p.length==6?p[2]+":"+p[3]:"none";}
}
