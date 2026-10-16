#!/usr/bin/env python3
"""Execute the real system observer/lease/controller. No device/HAL compatibility claim."""
import pathlib,tempfile,subprocess,os
root=pathlib.Path(__file__).resolve().parents[1]
files={
'android/os/SystemClock.java':'package android.os;public class SystemClock {public static long now=1000;public static long elapsedRealtime(){return now;}}',
'android/os/SystemProperties.java':'''package android.os;public class SystemProperties {
 public static java.util.Map<String,String> p=new java.util.HashMap<>();public static String get(String k,String d){return p.getOrDefault(k,d);}}
''',
'android/util/Log.java':'package android.util;public class Log {public static int i(String a,String b){return 0;}}',
'android/media/AudioSystem.java':'''package android.media;public class AudioSystem {
 public static java.util.List<String> writes=new java.util.ArrayList<>();public static int status;
 public static int setParameters(String p){writes.add(p);return status;}}
''',
'android/media/AudioManager.java':'''package android.media;public class AudioManager {
 public static java.util.List<Object> ports=new java.util.ArrayList<>();public static int listAudioDevicePorts(java.util.List<Object> p){p.addAll(ports);return 0;}
 public static class Port{public int id,type;public String address;public Port(int i,int t,String a){id=i;type=t;address=a;}public int id(){return id;}public int type(){return type;}public String address(){return address;}}}
''',
'de/robv/android/xposed/XC_MethodHook.java':'''package de.robv.android.xposed;public class XC_MethodHook {
 protected void afterHookedMethod(MethodHookParam p){}public void after(MethodHookParam p){afterHookedMethod(p);}
 public static class MethodHookParam{public Object thisObject;public Object[] args;public boolean hasThrowable(){return false;}}}
''',
'de/robv/android/xposed/XposedHelpers.java':'''package de.robv.android.xposed;import java.lang.reflect.*;public class XposedHelpers {
 public static XC_MethodHook hook;public static Class<?> findClass(String n,ClassLoader l){try{return Class.forName(n);}catch(Exception e){throw new RuntimeException(e);}}
 public static Object findAndHookMethod(Class<?> c,String n,Object...a){if(!c.getSimpleName().equals("AudioDeviceBroker")||!n.equals("setBluetoothScoOn"))throw new AssertionError();hook=(XC_MethodHook)a[a.length-1];return null;}
 public static Object getObjectField(Object o,String n){try{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new RuntimeException(e);}}
 public static Object callStaticMethod(Class<?> c,String n,Object...a){return invoke(c,null,n,a);}public static Object callMethod(Object o,String n,Object...a){return invoke(o.getClass(),o,n,a);}
 static Object invoke(Class<?> c,Object o,String n,Object[] a){for(Method m:c.getMethods())if(m.getName().equals(n)&&m.getParameterCount()==a.length)try{return m.invoke(o,a);}catch(Exception e){throw new RuntimeException(e);}throw new RuntimeException(n);}}
''',
'com/android/server/audio/AudioDeviceBroker.java':'''package com.android.server.audio;public class AudioDeviceBroker {
 public AudioService mAudioService=new AudioService();public static class AudioService{public int mode;public Runnable callback;public int getMode(){if(callback!=null){Runnable r=callback;callback=null;r.run();}return mode;}}}
''',
'com/hfpvoipfix/NativeScoGuardTest.java':'''package com.hfpvoipfix;
import android.os.*;import android.media.*;import com.android.server.audio.*;import de.robv.android.xposed.*;import java.util.*;
public class NativeScoGuardTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 static final Map<String,String> p=SystemProperties.p;
 static void broker(AudioDeviceBroker b,boolean on){XC_MethodHook.MethodHookParam m=new XC_MethodHook.MethodHookParam();m.thisObject=b;m.args=new Object[]{on,"fixture"};int n=AudioSystem.writes.size();XposedHelpers.hook.after(m);check(AudioSystem.writes.size()==n);}
 static void poll(String expected){int n=AudioSystem.writes.size();NativeScoGuard.poll();if(expected==null)check(n==AudioSystem.writes.size());else check(AudioSystem.writes.size()==n+1&&expected.equals(AudioSystem.writes.get(n)));}
 static void lease(){p.put(NativeScoLease.PROP,"v1:"+SystemClock.now+":42:1:60:64");}
 static void ports(){AudioManager.ports.clear();AudioManager.ports.add(new AudioManager.Port(60,0x80000008,"MASTER"));AudioManager.ports.add(new AudioManager.Port(64,0x20,"MASTER"));}
 public static void main(String[] args){
 p.put(LabModes.PROP,"telecom_duplex");p.put("ro.product.device","begonia");p.put("ro.build.version.release","11");p.put("debug.hfpvoipfix.native_split","1.9.0-persistent");ports();lease();
 NativeScoGuard.install(NativeScoGuardTest.class.getClassLoader());check(XposedHelpers.hook!=null);AudioDeviceBroker b=new AudioDeviceBroker();
 broker(b,false);poll(null);p.put(LabModes.PROP,"telecom_aa");poll("BT_SCO=on");poll(null);
 SystemClock.now=1100;lease();poll(null);broker(b,false);poll("BT_SCO=on");poll(null);
 SystemClock.now=6100;poll("BT_SCO=off");poll(null);
 lease();poll("BT_SCO=on");AudioManager.ports.clear();poll("BT_SCO=off");poll(null);
 ports();((AudioManager.Port)AudioManager.ports.get(1)).address="CAR";poll(null);ports();poll("BT_SCO=on");
 b.mAudioService.mode=2;poll(null);b.mAudioService.mode=3;poll(null);b.mAudioService.mode=0;broker(b,true);poll(null);
 p.put(NativeScoLease.PROP,"v1:999999:42:1:60:64");broker(b,false);poll(null);lease();
 b.mAudioService.callback=()->broker(b,true);poll(null);broker(b,false);poll("BT_SCO=on");
 p.put(NativeScoLease.PROP,"off");poll("BT_SCO=off");lease();AudioSystem.status=-1;
 poll("BT_SCO=on");poll("BT_SCO=on");poll(null);AudioSystem.status=0;broker(b,false);poll("BT_SCO=on");
 p.put(LabModes.PROP,"telecom_duplex");poll("BT_SCO=off");poll(null);
 String status="STATUS version=1.9.11 requested=telecom_aa applied=telecom_aa status_at=6100 epoch=42 sco_seq=1 call_count=1 call_active=true sco=1 native_sco=true soft_ports=true telecom_ok=true dirty=false sco_in_id=60 sco_out_id=64";
 check(NativeScoLease.proof(status,6100).equals("v1:6100:42:1:60:64"));
 for(String[] change:new String[][]{{"call_count=1","call_count=2"},{"sco=1 ","sco=2 "},{"call_active=true","call_active=false"},{"native_sco=true","native_sco=false"},{"soft_ports=true","soft_ports=false"},{"dirty=false","dirty=true"},{"telecom_ok=true","telecom_ok=false"},{"sco_in_id=60","sco_in_id=-1"},{"requested=telecom_aa","requested=telecom_duplex"},{"applied=telecom_aa","applied=idle"},{"version=1.9.11","version=1.9.9"}})check(NativeScoLease.proof(status.replace(change[0],change[1]),6100).equals("off"));
 check(NativeScoLease.proof(status,6099).equals("off"));check(NativeScoLease.proof(status,11100).equals("off"));
 String proof="v1:6100:42:1:60:64";
 check(NativeScoLease.allow("telecom_aa","begonia","11","1.9.0-persistent",proof,6100));
 check(!NativeScoLease.allow("telecom_duplex","begonia","11","1.9.0-persistent",proof,6100));
 for(String bad:new String[]{"off","v1:999999:42:1:60:64","v1:6100:0:1:60:64","v1:6100:42:0:60:64","v1:6100:42:1:60:64;id"})check(!NativeScoLease.allow("telecom_aa","begonia","11","1.9.0-persistent",bad,6100));
 System.out.println("PASS actual native SCO observer: no work under broker hook, one reconciliation per reset, fresh lease and matching ports, expiry/removal/mode cleanup, SIM/communication exclusion, concurrent event and bounded HAL failures");
 }
}
'''}
with tempfile.TemporaryDirectory(dir=root/'tests') as tmp:
 d=pathlib.Path(tmp);out=d/'classes';out.mkdir()
 for n,s in files.items():
  p=d/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s)
 java=pathlib.Path(os.environ['JAVA_HOME'])/'bin'
 src=[str(p) for p in d.rglob('*.java')]+[str(root/'app/src/main/java/com/hfpvoipfix'/n) for n in ['NativeScoGuard.java','NativeScoLease.java','NativeScoKeeper.java','RxGate.java','LabModes.java']]
 subprocess.run([str(java/'javac'),'-encoding','UTF-8','-d',str(out),*src],check=True)
 subprocess.run([str(java/'java'),'-cp',str(out),'com.hfpvoipfix.NativeScoGuardTest'],check=True)
