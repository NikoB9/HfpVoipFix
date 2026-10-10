package com.hfpvoipfix;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
final class Root {
  static String command(String cmd, int limit, int timeout) throws Exception {
    java.lang.Process p=new ProcessBuilder("su","-c",cmd).redirectErrorStream(true).start();
    StringBuilder text=new StringBuilder();
    Thread reader=new Thread(()->{
      try(BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream(),StandardCharsets.UTF_8))){
        String line;while((line=br.readLine())!=null){
          synchronized(text){if(text.length()<limit)text.append(line,0,Math.min(line.length(),limit-text.length())).append('\n');}
        }
      }catch(Exception ignored){}
    },"diag-root-read");
    reader.setDaemon(true);reader.start();
    if(!p.waitFor(timeout,TimeUnit.SECONDS)){p.destroyForcibly();throw new IOException("Timeout: "+cmd);}
    reader.join(1000);
    if(p.exitValue()!=0)throw new IOException("Exit "+p.exitValue()+": "+text);
    synchronized(text){return text.toString()+(text.length()>=limit?"\n[LIMITE DE SORTIE ATTEINTE]\n":"");}
  }
  static String check()throws Exception {
    String id=command("id -u",100,9).trim();
    if(!"0".equals(id))throw new IOException("Magisk n'a pas accordé root (uid="+id+")");
    return id;
  }
  static String snapshot(String stage) {
    StringBuilder result=new StringBuilder("Étape: "+stage+"\n");
    String[] cmds="pendant".equals(stage)?
      new String[]{"dumpsys audio","dumpsys media.audio_policy"}:
      new String[]{"dumpsys audio","dumpsys media.audio_policy","dumpsys media.audio_flinger",
        "dumpsys bluetooth_manager","dumpsys telecom","dumpsys package com.hfpvoipfix",
        "getprop ro.build.fingerprint","getprop ro.build.version.release","pidof com.android.bluetooth"};
    for(String cmd:cmds){
      result.append("\n====== ").append(cmd).append(" ======\n");
      try{result.append(command(cmd,220000,10));}
      catch(Exception e){result.append("ERREUR: ").append(e).append('\n');}
    }
    return result.toString();
  }
  static String config(){
    StringBuilder out=new StringBuilder();
    for(String f:new String[]{"/vendor/etc/audio_policy_configuration.xml","/vendor/etc/audio_device.xml",
       "/odm/etc/audio_policy_configuration.xml","/vendor/etc/audio_policy_configuration_bluetooth_legacy_hal.xml"}){
      out.append("\n===== ").append(f).append(" =====\n");
      try{out.append(command("cat "+f,300000,10));}catch(Exception e){out.append("Lecture indisponible: ").append(e.getMessage()).append('\n');}
    }
    return out.toString();
  }
}
