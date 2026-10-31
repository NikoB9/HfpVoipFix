#!/usr/bin/env python3
"""Production boot receiver and activation workflow, synthetic Android/root only."""
import pathlib,tempfile,subprocess,os
root=pathlib.Path(__file__).resolve().parents[1]
files={
'android/Manifest.java':'package android;public class Manifest{public static class permission{public static final String RECORD_AUDIO="record";}}',
'android/content/pm/PackageManager.java':'package android.content.pm;public class PackageManager{public static final int PERMISSION_GRANTED=0;}',
'android/content/BroadcastReceiver.java':'package android.content;public abstract class BroadcastReceiver{public abstract void onReceive(Context c,Intent i);}',
'android/content/Intent.java':'package android.content;public class Intent{public String action;public Class<?> target;public Intent(Context c,Class<?> t){target=t;}public Intent(String a){action=a;}public Intent setAction(String a){action=a;return this;}public String getAction(){return action;}}',
'android/content/Context.java':'''package android.content;import java.util.*;import java.io.*;public class Context{
 public int permission,foreground,services;public boolean reject;public List<String> order=new ArrayList<>();public Map<String,Pref> prefs=new HashMap<>();
 public int checkSelfPermission(String p){return permission;}public Pref getSharedPreferences(String n,int m){return prefs.computeIfAbsent(n,k->new Pref());}
 public Object startForegroundService(Intent i){if(reject)throw new IllegalStateException();foreground++;order.add("start");return null;}public Object startService(Intent i){services++;order.add(i.action);return null;}
 public File getFilesDir(){return new File(".");}
 public static class Pref{public Map<String,Object> values=new HashMap<>();public boolean fail;public String getString(String k,String d){Object v=values.get(k);return v instanceof String?(String)v:d;}public boolean getBoolean(String k,boolean d){Object v=values.get(k);return v instanceof Boolean?(Boolean)v:d;}public Edit edit(){return new Edit(this);}}
 public static class Edit{Pref p;Map<String,Object> v=new HashMap<>();Edit(Pref p){this.p=p;}public Edit putString(String k,String s){v.put(k,s);return this;}public Edit putBoolean(String k,boolean s){v.put(k,s);return this;}public Edit putInt(String k,int s){v.put(k,s);return this;}public void apply(){p.values.putAll(v);}public boolean commit(){if(p.fail)return false;apply();return true;}}
}''',
'com/hfpvoipfix/RelayService.java':'''package com.hfpvoipfix;import android.content.Context;class RelayService{static boolean enabled(Context c){return c.getSharedPreferences("relay",0).getBoolean("enabled",false);}static String mode(Context c){return c.getSharedPreferences("relay",0).getString("mode","telecom_aa_control");}}''',
'com/hfpvoipfix/RxBridgeService.java':'package com.hfpvoipfix;class RxBridgeService{static boolean alive;}',
'com/hfpvoipfix/LabProbe.java':'package com.hfpvoipfix;class LabProbe{static String status="STATUS version=1.9.18 busy=false dirty=false route=true calls=true disconnect=true voip=3";static String read(android.content.Context c){return status;}}',
'com/hfpvoipfix/BridgeJournal.java':'package com.hfpvoipfix;class BridgeJournal{static void write(java.io.File f,String s){}}',
'com/hfpvoipfix/RelayStartupTest.java':'''package com.hfpvoipfix;import android.content.*;
public class RelayStartupTest{
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}interface Op{void run()throws Exception;}static void refuses(Op o)throws Exception{try{o.run();throw new AssertionError("expected refusal");}catch(java.io.IOException expected){}}
 public static void main(String[]args)throws Exception{
 Context c=new Context();RelayBootReceiver boot=new RelayBootReceiver();Intent event=new Intent("android.intent.action.BOOT_COMPLETED");
 boot.onReceive(c,event);check(c.foreground==0,"new install stays disabled");boot.onReceive(c,null);boot.onReceive(c,new Intent("other"));
 RelaySetup.activate(c,"telecom_aa_control");check(c.foreground==1&&RelayService.enabled(c),"user activation starts supervisor without capture");check(RelayService.mode(c).equals("telecom_aa_control"),"controlled AA is default");check(c.getSharedPreferences("relay",0).getBoolean("auto_start",true),"automatic startup defaults on after explicit activation");
 c.getSharedPreferences("aa_diagnostic",0).edit().putBoolean("enabled",true).apply();boot.onReceive(c,event);check(c.foreground==2,"restore after boot");check(!c.getSharedPreferences("aa_diagnostic",0).getBoolean("enabled",true),"boot does not rearm optional diagnostics");boot.onReceive(c,new Intent("android.intent.action.MY_PACKAGE_REPLACED"));check(c.foreground==3,"restore package replacement");
 c.getSharedPreferences("relay",0).edit().putBoolean("auto_start",false).apply();boot.onReceive(c,event);check(c.foreground==3,"autostart opt-out respected");
 c.getSharedPreferences("relay",0).edit().putBoolean("auto_start",true).apply();c.permission=-1;boot.onReceive(c,event);check(c.foreground==3,"missing microphone never starts service");refuses(()->RelaySetup.activate(c,"telecom_duplex"));c.permission=0;
 c.reject=true;boot.onReceive(c,event);check(c.getSharedPreferences("relay",0).getString("status","").contains("Ouvrir"),"boot restriction handled without crash");c.reject=false;
 LabProbe.status=LabProbe.status.replace("busy=false","busy=true");refuses(()->RelaySetup.activate(c,"telecom_duplex"));check(RelayService.mode(c).equals("telecom_aa_control"),"live call prevents reconfiguration");LabProbe.status=LabProbe.status.replace("busy=true","busy=false");
 RxBridgeService.alive=true;RelaySetup.activate(c,"telecom_duplex");check(c.services==1&&c.order.indexOf("reconfigure")<c.order.lastIndexOf("start"),"idle mode switch reconfigures bridge and keeps supervisor");check(RelayService.mode(c).equals("telecom_duplex"),"D5 explicit choice persisted");
 c.getSharedPreferences("relay",0).edit().putBoolean("enabled",false).apply();refuses(()->RelaySetup.activate(c,"projection_only"));RxBridgeService.alive=false;
 c.getSharedPreferences("relay",0).fail=true;refuses(()->RelaySetup.activate(c,"telecom_aa_control"));c.getSharedPreferences("relay",0).fail=false;
 for(String action:new String[]{null,"other","android.intent.action.LOCKED_BOOT_COMPLETED"})check(!RelayStartupPolicy.restore(action,true,true,true),"no action before unlock or unrelated broadcast");
 check(RelayStartupPolicy.normalMode("unknown").equals("telecom_aa_control"),"invalid normal choice falls back to controlled AA");
 System.out.println("PASS production startup/setup: explicit activation, default AA, boot/update restore, opt-out, microphone and boot refusal, busy-call guard, idle reconfigure, persistence failure and no diagnostic dependency");
 }}'''}
with tempfile.TemporaryDirectory(dir=root/'tests') as tmp:
 d=pathlib.Path(tmp);out=d/'classes';out.mkdir()
 for n,s in files.items():
  p=d/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s)
 src=[str(p) for p in d.rglob('*.java')]+[str(root/'app/src/main/java/com/hfpvoipfix'/n) for n in ['RelayStartupPolicy.java','RelayBootReceiver.java','RelaySetup.java','RxGate.java','LabModes.java']]
 java=pathlib.Path(os.environ['JAVA_HOME'])/'bin'
 subprocess.run([str(java/'javac'),'-encoding','UTF-8','-d',str(out),*src],check=True)
 subprocess.run([str(java/'java'),'-cp',str(out),'com.hfpvoipfix.RelayStartupTest'],check=True)
