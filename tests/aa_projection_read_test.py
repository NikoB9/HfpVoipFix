#!/usr/bin/env python3
"""Production AA observer, bounded level meter, and trial order. No car/device claim."""
import ast,os,pathlib,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parents[1]
module=ast.parse((root/'tests/native_sco_guard_test.py').read_text())
files=ast.literal_eval(next(n.value for n in module.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='files' for t in n.targets)))
files.pop('com/hfpvoipfix/NativeScoGuardTest.java')
files.update({
'com/hfpvoipfix/AaCarFocusHook.java':'package com.hfpvoipfix;final class AaCarFocusHook {static String status(){return "fixture_observer_only";}}',
'android/util/Log.java':'''package android.util;public class Log {public static java.util.List<String> lines=new java.util.ArrayList<>();public static int i(String a,String b){lines.add(b);return 0;}}''',
'android/media/AudioRecordingConfiguration.java':'''package android.media;public class AudioRecordingConfiguration {public boolean silenced;public boolean isClientSilenced(){return silenced;}}''',
'android/media/AudioDeviceInfo.java':'''package android.media;public class AudioDeviceInfo {public String address="1506431339:ap:0mixp:1";public int getId(){return 46;}public int getType(){return 25;}public String getAddress(){return address;}}''',
'android/media/AudioRecord.java':'''package android.media;public class AudioRecord {public int source=8,encoding=2;public AudioDeviceInfo device=new AudioDeviceInfo();public AudioRecordingConfiguration config;public boolean configError;public AudioRecordingConfiguration getActiveRecordingConfiguration(){if(configError)throw new IllegalStateException();return config;}public int getAudioSource(){return source;}public int getAudioFormat(){return encoding;}public int getSampleRate(){return 16000;}public int getChannelCount(){return 1;}public AudioDeviceInfo getRoutedDevice(){return device;}}''',
'de/robv/android/xposed/XC_MethodHook.java':'''package de.robv.android.xposed;public class XC_MethodHook {
 public static class Unhook {}protected void beforeHookedMethod(MethodHookParam p){}protected void afterHookedMethod(MethodHookParam p){}
 public void before(MethodHookParam p){beforeHookedMethod(p);}public void after(MethodHookParam p){afterHookedMethod(p);}
 public static class MethodHookParam {public Object thisObject,result;public Object[] args;public boolean error;private java.util.Map<String,Object> extra=new java.util.HashMap<>();public Object getResult(){return result;}public boolean hasThrowable(){return error;}public void setObjectExtra(String k,Object v){extra.put(k,v);}public Object getObjectExtra(String k){return extra.get(k);}}}''',
'de/robv/android/xposed/XposedBridge.java':'''package de.robv.android.xposed;public class XposedBridge {public static XC_MethodHook hook;public static java.util.Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> c,String n,XC_MethodHook h){if(!n.equals("read")||!c.getSimpleName().equals("AudioRecord"))throw new AssertionError();hook=h;return java.util.Collections.singleton(new XC_MethodHook.Unhook());}}''',
'com/hfpvoipfix/AaProjectionReadTest.java':'''package com.hfpvoipfix;
import android.os.*;import android.media.*;import android.util.Log;import java.nio.*;import java.util.*;import de.robv.android.xposed.*;
public class AaProjectionReadTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void meter(){
  short[] a=new short[20000];Arrays.fill(a,(short)-32768);short[] before=a.clone();PcmLevel m=new PcmLevel();m.add(a,0,Integer.MAX_VALUE,2);check(m.inspected<=256&&m.peak==32768&&m.rms()==32768&&Arrays.equals(a,before),"bounded shorts, fullscale and immutable input");
  byte[] bytes=new byte[18];ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());for(int i=0;i<9;i++)b.putShort(2*i,(short)1000);byte[] old=bytes.clone();m=new PcmLevel();m.add(bytes,2,12,2);check(m.inspected==6&&m.rms()==1000&&Arrays.equals(bytes,old),"byte offset and native order");
  ByteBuffer direct=ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());for(int i=0;i<32;i++)direct.putShort(2*i,(short)2000);direct.position(13);direct.limit(19);m=new PcmLevel();m.add(direct,0,64,2);check(m.inspected==32&&m.rms()==2000&&direct.position()==13&&direct.limit()==19,"direct read starts at buffer base; preserve cursor and limit");
  m=new PcmLevel();m.add(new float[]{Float.NaN,Float.POSITIVE_INFINITY,0.5f,-0.5f},0,4,4);check(m.inspected==2&&m.rms()==16383,"float finite samples");
  m=new PcmLevel();m.add(a,-1,4,2);m.add(a,30000,4,2);m.add(a,0,-1,2);m.add(a,0,4,4);check(m.inspected==0,"invalid and unsupported inputs ignored");
 }
 static void trial(){
  check(AaAudioTrial.select(true,1,"42:1",0,"42:1")==0,"same call keeps trial on SCO reconnect");
  check(AaAudioTrial.select(true,1,"42:1",0,"42:2")==1,"new call advances once");
  check(AaAudioTrial.select(true,4,"42:4",3,"42:4")==3,"fourth call reconnect keeps fourth trial");
  check(AaAudioTrial.select(true,4,"42:4",3,"42:5")==-1,"fifth call normal baseline");
  check(AaAudioTrial.select(false,0,"",-1,"42:1")==-1&&AaAudioTrial.select(true,0,"",-1,"42:-1")==-1,"disabled/missing call id");
  int[][] expected={{12,7},{1,7},{1,1},{12,1}};
  for(int i=0;i<4;i++){AaAudioTrial t=AaAudioTrial.at(i);check(t.index==i&&t.usage==expected[i][0]&&t.micSource==expected[i][1],"trial choice");if(i>0)check((expected[i][0]!=expected[i-1][0])!=(expected[i][1]!=expected[i-1][1]),"one variable at a time");}
  check(!AaAudioTrial.continueTrial(true,0,false)&&!AaAudioTrial.continueTrial(false,0,true)&&!AaAudioTrial.continueTrial(true,4,true)&&AaAudioTrial.continueTrial(true,3,true),"started and bounded trials");
 }
 static XC_MethodHook.MethodHookParam begin(AudioRecord r,Object[] a,int n){XC_MethodHook.MethodHookParam p=new XC_MethodHook.MethodHookParam();p.thisObject=r;p.args=a;p.result=n;XposedBridge.hook.before(p);return p;}
 static void read(AudioRecord r,Object[] a,int n){XposedBridge.hook.after(begin(r,a,n));flush();}
 static void flush(){try{java.lang.reflect.Field f=AaProjectionReadHook.class.getDeclaredField("logging");f.setAccessible(true);((java.util.concurrent.ThreadPoolExecutor)f.get(null)).submit(()->{}).get(2,java.util.concurrent.TimeUnit.SECONDS);}catch(Exception e){throw new RuntimeException(e);}}
 public static void main(String[] args){meter();trial();
  Map<String,String> p=SystemProperties.p;p.put(LabModes.PROP,"telecom_aa");p.put("ro.product.device","begonia");p.put("ro.build.version.release","11");p.put("debug.hfpvoipfix.native_split","1.9.0-persistent");p.put(NativeScoLease.PROP,"v1:1000:42:1:60:64");
  AaProjectionReadHook.install();check(XposedBridge.hook!=null,"observer installed");AudioRecord r=new AudioRecord();short[] data=new short[1024];Arrays.fill(data,(short)1000);Object[] call={data,0,data.length,0};
  read(r,call,1024);SystemClock.now=3000;read(r,call,1024);String log=Log.lines.get(Log.lines.size()-1);check(log.contains("sampled_rms=1000")&&log.contains("returned_units=2048")&&log.contains("address=1506431339:ap:0mixp:1"),"observe AA consumption");check(log.contains("reader_silenced=unknown")&&log.contains("window_start_elapsed_ms=1000")&&log.contains("window_end_elapsed_ms=3000"),"unknown is not false and windows use read time");
  SystemClock.now=3100;XC_MethodHook.MethodHookParam outer=begin(r,call,1024),inner=begin(r,call,1024);XposedBridge.hook.after(inner);XposedBridge.hook.after(outer);SystemClock.now=5000;read(r,call,0);log=Log.lines.get(Log.lines.size()-1);check(log.contains("returned_units=1024")&&log.contains("reads=2"),"nested overloads counted once; zero read counted");
  int count=Log.lines.size();SystemClock.now=6000;read(r,call,1024);SystemClock.now=9000;read(r,call,1024);check(Log.lines.size()==count,"expired lease no observation");
  p.put(NativeScoLease.PROP,"v1:9000:42:2:60:64");r.source=1;read(r,call,1024);SystemClock.now=11000;read(r,call,1024);check(Log.lines.size()==count,"no microphone hook observation");
  r.source=8;p.put(LabModes.PROP,"telecom_duplex");read(r,call,1024);check(Log.lines.size()==count,"D5 excluded");
  p.put(LabModes.PROP,"telecom_aa");p.put(NativeScoLease.PROP,"v1:11000:42:2:60:64");r.device.address="PRIVATE_MAC";read(r,call,-3);SystemClock.now=13000;read(r,call,-3);log=Log.lines.get(Log.lines.size()-1);check(log.contains("last_result=-3")&&log.contains("inspected=0")&&!log.contains("PRIVATE_MAC"),"errors and unknown address redaction");
  r.config=new AudioRecordingConfiguration();r.config.silenced=true;SystemClock.now=14000;p.put(NativeScoLease.PROP,"v1:14000:42:3:60:64");read(r,call,1024);SystemClock.now=16000;read(r,call,1024);log=Log.lines.get(Log.lines.size()-1);check(log.contains("reader_silenced=true"),"observe AA reader silencing itself");
  r.config.silenced=false;SystemClock.now=18000;read(r,call,1024);log=Log.lines.get(Log.lines.size()-1);check(log.contains("reader_silenced=false"),"reader not silenced is explicit");
  r.configError=true;p.put(NativeScoLease.PROP,"v1:18000:42:3:60:64");SystemClock.now=20000;read(r,call,1024);log=Log.lines.get(Log.lines.size()-1);check(log.contains("reader_silenced=unknown"),"OEM metadata failure is unknown");
  check(Arrays.equals(data,new short[1024])==false&&data[0]==1000&&data[1023]==1000,"audio unchanged");
  System.out.println("PASS actual AA observer and meter: scalar-only bounded samples, direct-buffer cursor/base, byte offsets, nested overloads, negative/zero results, source/lease/D5 exclusions, address redaction and one-variable trial order");
 }
}'''
})
with tempfile.TemporaryDirectory(dir=root/'tests') as tmp:
 d=pathlib.Path(tmp);out=d/'classes';out.mkdir()
 for n,s in files.items():
  p=d/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s)
 java=pathlib.Path(os.environ['JAVA_HOME'])/'bin'
 src=[str(p) for p in d.rglob('*.java')]+[str(root/'app/src/main/java/com/hfpvoipfix'/n) for n in ['AaProjectionReadHook.java','AaAudioTrial.java','PcmLevel.java','NativeScoLease.java','RxGate.java','LabModes.java']]
 subprocess.run([str(java/'javac'),'-encoding','UTF-8','-d',str(out),*src],check=True)
 subprocess.run([str(java/'java'),'-cp',str(out),'com.hfpvoipfix.AaProjectionReadTest'],check=True)
