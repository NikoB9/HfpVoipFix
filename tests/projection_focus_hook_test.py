#!/usr/bin/env python3
"""Execute production focus coexistence + native evidence against Android 11 stack contracts.

The fixture keeps the AOSP order: propagate losses, then push the new winner;
abandon removes the winner and grants focus to the previous top. Not hardware QA.
"""
import ast,os,pathlib,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parents[1]
# Reuse the small broker/HAL fakes, without running their independent test program.
module=ast.parse((root/'tests/native_sco_guard_test.py').read_text())
files=ast.literal_eval(next(n.value for n in module.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='files' for t in n.targets)))
files.pop('com/hfpvoipfix/NativeScoGuardTest.java')
files['com/android/server/audio/AudioDeviceBroker.java']=files['com/android/server/audio/AudioDeviceBroker.java'].replace('public int mode;', 'public int mode,reads;').replace('public int getMode(){', 'public int getMode(){reads++;')
files['android/media/AudioManager.java']=files['android/media/AudioManager.java'].replace('public static int listAudioDevicePorts', 'public static int enumerations;public static int listAudioDevicePorts').replace('p.addAll(ports);', 'enumerations++;p.addAll(ports);')
files.update({
'android/media/AudioAttributes.java':'''package android.media;public class AudioAttributes {
 public int usage,content=1;public AudioAttributes(int u){usage=u;}public int getUsage(){return usage;}public int getContentType(){return content;}}''',
'de/robv/android/xposed/XC_MethodHook.java':'''package de.robv.android.xposed;public class XC_MethodHook {
 protected void beforeHookedMethod(MethodHookParam p){}protected void afterHookedMethod(MethodHookParam p){}
 public void before(MethodHookParam p){beforeHookedMethod(p);}public void after(MethodHookParam p){afterHookedMethod(p);}
 public static class MethodHookParam{public Object thisObject;public Object[] args;public boolean returned;public Object result;
 public boolean hasThrowable(){return false;}public void setResult(Object r){returned=true;result=r;}}}''',
'de/robv/android/xposed/XposedHelpers.java':'''package de.robv.android.xposed;import java.lang.reflect.*;public class XposedHelpers {
 public static java.util.Map<String,XC_MethodHook> hooks=new java.util.HashMap<>();
 public static Class<?> findClass(String n,ClassLoader l){try{return Class.forName(n);}catch(Exception e){throw new RuntimeException(e);}}
 public static Object findAndHookMethod(Class<?> c,String n,Object...a){hooks.put(n,(XC_MethodHook)a[a.length-1]);return null;}
 public static Object getObjectField(Object o,String n){try{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new RuntimeException(e);}}
 public static Object callStaticMethod(Class<?> c,String n,Object...a){return invoke(c,null,n,a);}public static Object callMethod(Object o,String n,Object...a){return invoke(o.getClass(),o,n,a);}
 static Object invoke(Class<?> c,Object o,String n,Object[] a){for(Method m:c.getDeclaredMethods())if(m.getName().equals(n)&&m.getParameterCount()==a.length)try{m.setAccessible(true);return m.invoke(o,a);}catch(Exception e){throw new RuntimeException(e);}throw new RuntimeException(n);}}''',
'com/android/server/audio/MediaFocusControl.java':'''package com.android.server.audio;public class MediaFocusControl {
 public static final Object mAudioFocusLock=new Object();public java.util.Stack<FocusRequester> mFocusStack=new java.util.Stack<>();
 public int grants;
 public void request(FocusRequester f){synchronized(mAudioFocusLock){for(FocusRequester l:mFocusStack)l.handleFocusLoss(f.mFocusGainRequest==1?-1:f.mFocusGainRequest==3?-3:-2,f,false);mFocusStack.push(f);grants++;}}
 public void abandon(FocusRequester f){synchronized(mAudioFocusLock){boolean top=!mFocusStack.empty()&&mFocusStack.peek()==f;mFocusStack.remove(f);if(top&&!mFocusStack.empty()){mFocusStack.peek().mFocusLossReceived=0;mFocusStack.peek().regains++;}}}
}''',
'com/android/server/audio/FocusRequester.java':'''package com.android.server.audio;import android.media.*;import de.robv.android.xposed.*;
public class FocusRequester {
 public String mPackageName;public AudioAttributes mAttributes;public int mFocusGainRequest,mGrantFlags,mFocusLossReceived;
 public MediaFocusControl mFocusController;public int losses,regains;
 public FocusRequester(MediaFocusControl c,String p,int u,int g){mFocusController=c;mPackageName=p;mAttributes=new AudioAttributes(u);mFocusGainRequest=g;}
 public void handleFocusLoss(int loss,FocusRequester winner,boolean force){synchronized(MediaFocusControl.mAudioFocusLock){
  XC_MethodHook h=XposedHelpers.hooks.get("handleFocusLoss");XC_MethodHook.MethodHookParam p=new XC_MethodHook.MethodHookParam();p.thisObject=this;p.args=new Object[]{loss,winner,force};
  if(h!=null)h.before(p);if(!p.returned&&loss!=mFocusLossReceived){mFocusLossReceived=loss;losses++;}
 }}
}''',
'com/hfpvoipfix/ProjectionFocusHookTest.java':'''package com.hfpvoipfix;
import android.os.*;import android.media.*;import com.android.server.audio.*;import de.robv.android.xposed.*;import java.util.*;
public class ProjectionFocusHookTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static Map<String,String> p=SystemProperties.p;static AudioDeviceBroker broker=new AudioDeviceBroker();
 static void proof(long seq){p.put(NativeScoLease.PROP,"v1:"+SystemClock.now+":42:"+seq+":60:64");}
 static FocusRequester relay(MediaFocusControl c){return new FocusRequester(c,"com.hfpvoipfix",12,3);}
 static FocusRequester aa(MediaFocusControl c,int usage){return new FocusRequester(c,"com.google.android.projection.gearhead",usage,2);}
 static void nativePoll(){NativeScoGuard.poll();}
 static void verifyPolicy(){
  check(ProjectionFocusGate.allow(true,"com.hfpvoipfix",12,1,3,0,"com.google.android.projection.gearhead",2,2,1,-2,false),"target policy");
  for(int u:new int[]{3,4,5,6,7,8,9,10,11,13,14,15,16,17})check(!ProjectionFocusGate.allow(true,"com.hfpvoipfix",12,1,3,0,"com.google.android.projection.gearhead",u,2,1,-2,false),"unrelated usage "+u);
  for(int gain:new int[]{1,3,4})check(!ProjectionFocusGate.allow(true,"com.hfpvoipfix",12,1,3,0,"com.google.android.projection.gearhead",2,gain,1,-2,false),"unrelated gain");
 }
 public static void main(String[] args){
  p.put(LabModes.PROP,"telecom_aa");p.put("ro.product.device","begonia");p.put("ro.build.version.release","11");p.put("debug.hfpvoipfix.native_split","1.9.0-persistent");proof(1);
  AudioManager.ports.add(new AudioManager.Port(60,0x80000008,"MASTER"));AudioManager.ports.add(new AudioManager.Port(64,0x20,"MASTER"));
  NativeScoGuard.install(ProjectionFocusHookTest.class.getClassLoader());
  XC_MethodHook.MethodHookParam b=new XC_MethodHook.MethodHookParam();b.thisObject=broker;b.args=new Object[]{false,"fixture"};XposedHelpers.hooks.get("setBluetoothScoOn").after(b);nativePoll();
  ProjectionFocusHook.install(ProjectionFocusHookTest.class.getClassLoader());check(XposedHelpers.hooks.containsKey("handleFocusLoss"),"hook installed");verifyPolicy();
  for(int usage:new int[]{0,1,2,12}){
   MediaFocusControl c=new MediaFocusControl();FocusRequester l=relay(c),w=aa(c,usage);c.request(l);int n=AudioSystem.writes.size(),queries=broker.mAudioService.reads,enumerations=AudioManager.enumerations;c.request(w);
   check(c.grants==2&&c.mFocusStack.peek()==w&&l.losses==0,"both requests run normally; relay shared");check(AudioSystem.writes.size()==n&&queries==broker.mAudioService.reads&&enumerations==AudioManager.enumerations,"no HAL/service/port work under focus lock");
   ProjectionFocusHook.poll();check(l.losses==0,"live lease retained");c.abandon(w);ProjectionFocusHook.poll();check(l.regains==1&&l.losses==0,"normal AA abandon/regain");
  }
  MediaFocusControl c=new MediaFocusControl();FocusRequester l=relay(c),w=aa(c,2);c.request(l);c.request(w);
  SystemClock.now=6000;ProjectionFocusHook.poll();check(l.losses==1&&l.mFocusLossReceived==-2,"expired lease restores original transient loss");ProjectionFocusHook.poll();check(l.losses==1,"restore once");
  proof(2);nativePoll();c=new MediaFocusControl();l=relay(c);w=aa(c,2);c.request(l);c.request(w);proof(3);nativePoll();ProjectionFocusHook.poll();check(l.losses==1,"new call cannot inherit coexistence");
  c=new MediaFocusControl();l=relay(c);w=aa(c,2);c.request(l);c.request(w);broker.mAudioService.mode=2;nativePoll();ProjectionFocusHook.poll();check(l.losses==1,"local call mode revokes sharing");broker.mAudioService.mode=0;nativePoll();
  c=new MediaFocusControl();l=relay(c);w=aa(c,2);c.request(l);c.request(w);c.request(new FocusRequester(c,"assistant",16,4));check(l.losses==1,"other winner remains exclusive");ProjectionFocusHook.poll();
  c=new MediaFocusControl();l=relay(c);w=aa(c,2);c.request(l);c.request(w);c.abandon(l);p.put(NativeScoLease.PROP,"off");ProjectionFocusHook.poll();check(l.losses==0,"released relay not called");proof(3);nativePoll();
  for(String kind:new String[]{"permanent","exclusive","assistant","notification","locked","other","D5","force","previous","direct","wrong_usage","wrong_content","wrong_gain","ports","stale_worker","changed_ports"}){
   proof(3);p.put(LabModes.PROP,"telecom_aa");nativePoll();c=new MediaFocusControl();l=relay(c);w=aa(c,2);c.request(l);
   if(kind.equals("permanent"))w.mFocusGainRequest=1;if(kind.equals("exclusive"))w.mFocusGainRequest=4;
   if(kind.equals("assistant"))w.mAttributes.usage=16;if(kind.equals("notification"))w.mAttributes.usage=5;
   if(kind.equals("locked"))w.mGrantFlags=4;if(kind.equals("other"))w.mPackageName="com.android.server.telecom";
   if(kind.equals("D5"))p.put(LabModes.PROP,"telecom_duplex");if(kind.equals("wrong_usage"))l.mAttributes.usage=1;
   if(kind.equals("wrong_content"))l.mAttributes.content=2;if(kind.equals("wrong_gain"))l.mFocusGainRequest=1;
   if(kind.equals("previous"))l.mFocusLossReceived=-3;
   if(kind.equals("ports")){AudioManager.ports.clear();nativePoll();AudioManager.ports.add(new AudioManager.Port(60,0x80000008,"MASTER"));AudioManager.ports.add(new AudioManager.Port(64,0x20,"MASTER"));}
   if(kind.equals("stale_worker"))SystemClock.now+=1200;
   if(kind.equals("changed_ports"))p.put(NativeScoLease.PROP,"v1:"+SystemClock.now+":42:3:61:65");
   if(kind.equals("force"))l.handleFocusLoss(-2,w,true);else if(kind.equals("direct"))l.handleFocusLoss(-2,null,false);else c.request(w);
   check(l.losses==1,"fail closed: "+kind);ProjectionFocusHook.poll();
  }
  System.out.println("PASS production focus pair: normal grant stack, no HAL/service calls, four proxy usages, abandon/death, expiry, changed call, local call, unrelated owners, locked/exclusive/permanent requests, stale/changed/missing ports and bounded cleanup");
 }
}'''
})
with tempfile.TemporaryDirectory(dir=root/'tests') as tmp:
 d=pathlib.Path(tmp);out=d/'classes';out.mkdir()
 for n,s in files.items():
  p=d/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s)
 java=pathlib.Path(os.environ['JAVA_HOME'])/'bin'
 src=[str(p) for p in d.rglob('*.java')]+[str(root/'app/src/main/java/com/hfpvoipfix'/n) for n in ['NativeScoGuard.java','NativeScoLease.java','NativeScoKeeper.java','RxGate.java','LabModes.java','ProjectionFocusGate.java','ProjectionFocusHook.java']]
 subprocess.run([str(java/'javac'),'-encoding','UTF-8','-d',str(out),*src],check=True)
 subprocess.run([str(java/'java'),'-cp',str(out),'com.hfpvoipfix.ProjectionFocusHookTest'],check=True)
