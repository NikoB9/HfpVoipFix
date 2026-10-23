#!/usr/bin/env python3
"""Run production AA hook with synthetic metadata-only endpoints, never the proprietary APK."""
import ast,os,pathlib,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parents[1]
tree=ast.parse((root/'tests/native_sco_guard_test.py').read_text())
files=ast.literal_eval(next(n.value for n in tree.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='files' for t in n.targets)))
files={n:s for n,s in files.items() if n.startswith('android/os/')}
files.update({
'android/util/Log.java':'package android.util;public class Log {public static java.util.List<String> lines=new java.util.ArrayList<>();public static int i(String t,String s){lines.add(s);return 0;}}',
'de/robv/android/xposed/XC_MethodHook.java':'''package de.robv.android.xposed;public class XC_MethodHook {protected void beforeHookedMethod(MethodHookParam p){}protected void afterHookedMethod(MethodHookParam p){}public void before(MethodHookParam p){beforeHookedMethod(p);}public void after(MethodHookParam p){afterHookedMethod(p);}public static class MethodHookParam {public Object thisObject;public Object[] args;public boolean error;private java.util.Map<String,Object> extra=new java.util.HashMap<>();public boolean hasThrowable(){return error;}public void setObjectExtra(String k,Object v){extra.put(k,v);}public Object getObjectExtra(String k){return extra.get(k);}}}''',
'de/robv/android/xposed/XposedHelpers.java':'''package de.robv.android.xposed;public class XposedHelpers {
public static java.util.Map<String,XC_MethodHook> hooks=new java.util.HashMap<>();public static Class<?> findClass(String n,ClassLoader l){try{return Class.forName(n);}catch(Exception e){throw new RuntimeException(e);}}
public static Object findAndHookMethod(Class<?> c,String n,Object... args){hooks.put(c.getName()+"#"+n,(XC_MethodHook)args[args.length-1]);return null;}
public static Object callStaticMethod(Class<?> c,String n,Object...a){return invoke(c,null,n,a);}public static Object callMethod(Object o,String n,Object...a){return invoke(o.getClass(),o,n,a);}
static Object invoke(Class<?> c,Object o,String n,Object[]a){for(java.lang.reflect.Method m:c.getDeclaredMethods())if(m.getName().equals(n)&&m.getParameterCount()==a.length)try{m.setAccessible(true);return m.invoke(o,a);}catch(Exception e){throw new RuntimeException(e);}throw new RuntimeException(n);}}''',
'xko.java':'public enum xko {a,b,c,d,e,f,g,h}',
'xkn.java':'public enum xkn {a,b,c,d}',
'iso.java':'''public class iso {public xko h=xko.f;public xkn j;public String i="com.hfpvoipfix";public Phone f=new Phone();public Waiter m=new Waiter();public Object l=new Object();public Audio o=new Audio();public int sends;public boolean throwSend;public void m(int gain){}public void l(xkn r,int attempt){sends++;if(throwSend)throw new IllegalStateException();j=r;}public void i(xko s,boolean a,boolean b){h=s;}public static class Phone {public boolean d=true;}public static class Waiter {public boolean stopped;public boolean f(){return stopped;}}public static class Audio {public int mode;public int getMode(){return mode;}}}''',
'qvk.java':'public class qvk {public xko g=xko.f;public void i(int gain){}}',
'jfd.java':'public class jfd {public boolean g;public void C(long t,jef f,Runnable r){}public void a(int t,java.nio.ByteBuffer b){}public void w(){g=false;}}',
'jdz.java':'public class jdz extends jfd {public int e=5;}',
'jef.java':'public class jef {}',
'com/hfpvoipfix/AaCarFocusTest.java':'''package com.hfpvoipfix;
import java.util.*;import android.os.*;import android.util.Log;import de.robv.android.xposed.*;import java.lang.reflect.*;
public class AaCarFocusTest {
static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);}
static Object fresh()throws Exception{return Class.forName("iso").getConstructor().newInstance();}
static void put(Object o,String f,Object v)throws Exception{o.getClass().getField(f).set(o,v);}static Object get(Object o,String f)throws Exception{return o.getClass().getField(f).get(o);}
static Object e(String name,int ordinal)throws Exception{return Class.forName(name).getEnumConstants()[ordinal];}
static XC_MethodHook.MethodHookParam call(String key,Object o,Object...args){XC_MethodHook.MethodHookParam p=new XC_MethodHook.MethodHookParam();p.thisObject=o;p.args=args;XposedHelpers.hooks.get(key).before(p);XposedHelpers.hooks.get(key).after(p);return p;}
static void lease(int seq){SystemProperties.p.put(NativeScoLease.PROP,"v1:"+SystemClock.now+":42:"+seq+":60:64");}
static void request(Object o){call("iso#m",o,3);}
static int sends(Object o)throws Exception{return (Integer)get(o,"sends");}
public static void main(String[] args)throws Exception{
Map<String,String> p=SystemProperties.p;p.put(LabModes.PROP,"telecom_aa");p.put("ro.product.device","begonia");p.put("ro.build.version.release","11");p.put("debug.hfpvoipfix.native_split","1.9.0-persistent");lease(1);
check(!AaCarFocusHook.exactApk(null)&&!AaCarFocusHook.exactApk("/not/a/file"),"missing binary excluded");
java.nio.file.Path other=java.nio.file.Files.createTempFile("different-aa", ".apk");java.nio.file.Files.write(other,new byte[]{1,2,3});check(!AaCarFocusHook.exactApk(other.toString()),"different SHA excluded");java.nio.file.Files.delete(other);
AaCarFocusHook.install(AaCarFocusTest.class.getClassLoader(),null);check(XposedHelpers.hooks.isEmpty(),"production entry with wrong APK installs no obfuscated hooks");
AaCarFocusHook.installVerified(AaCarFocusTest.class.getClassLoader());check(XposedHelpers.hooks.containsKey("iso#m")&&XposedHelpers.hooks.containsKey("qvk#i"),"both implementations observable");
Object o=fresh();request(o);check(sends(o)==1&&get(o,"j")==e("xkn",2)&&get(o,"h")==e("xko",5),"original sender invoked with MAY_DUCK; no car grant fabricated");check((Boolean)get(get(o,"f"),"d"),"call state untouched");put(o,"j",null);request(o);check(sends(o)==1,"no request storm even after refusal or timeout");lease(2);request(o);check(sends(o)==2,"new SCO may try once");
for(String why:new String[]{"D5","expired","future","no_lease","other_owner","no_owner","gain","state","not_call","pending","stopped","no_binding","SIM_mode","communication_mode","error"}){
lease(3);p.put(LabModes.PROP,"telecom_aa");Object x=fresh();int gain=3;
switch(why){case "D5":p.put(LabModes.PROP,"telecom_duplex");break;case "expired":p.put(NativeScoLease.PROP,"v1:1:42:3:60:64");SystemClock.now=10000;break;case "future":p.put(NativeScoLease.PROP,"v1:9999999:42:3:60:64");break;case "no_lease":p.put(NativeScoLease.PROP,"off");break;case "other_owner":put(x,"i","private.other.app");break;case "no_owner":put(x,"i",null);break;case "gain":gain=1;break;case "state":put(x,"h",e("xko",3));break;case "not_call":put(get(x,"f"),"d",false);break;case "pending":put(x,"j",e("xkn",1));break;case "stopped":put(get(x,"m"),"stopped",true);break;case "no_binding":put(x,"l",null);break;case "SIM_mode":put(get(x,"o"),"mode",2);break;case "communication_mode":put(get(x,"o"),"mode",3);break;}
XC_MethodHook.MethodHookParam m=new XC_MethodHook.MethodHookParam();m.thisObject=x;m.args=new Object[]{gain};m.error=why.equals("error");XposedHelpers.hooks.get("iso#m").after(m);check(sends(x)==0,"excluded: "+why);
}
lease(4);Object failing=fresh();put(failing,"throwSend",true);request(failing);request(failing);check(sends(failing)==1,"sender errors preserve bound and do not fake pending/grant");
Object legacy=Class.forName("qvk").getConstructor().newInstance();call("qvk#i",legacy,3);check(get(legacy,"g")==e("xko",5),"legacy is observe-only");
Object endpoint=Class.forName("jdz").getConstructor().newInstance();put(endpoint,"g",true);Object frame=Class.forName("jef").getConstructor().newInstance();call("jfd#C",endpoint,1L,frame,null);call("jfd#a",endpoint,32773,null);call("jfd#a",endpoint,32772,null);SystemClock.now+=2000;call("jfd#C",endpoint,2L,frame,null);
String last=Log.lines.get(Log.lines.size()-1);check(last.contains("submit_calls=2")&&last.contains("started_at_submit_calls=2")&&last.contains("ack_messages_received=1")&&last.contains("audible_unverified=true"),"transport metadata only; ACK arrival not audibility");check(!Log.lines.toString().contains("private.other.app"),"other package names not logged");
System.out.println("PASS production AA car-focus hook: exact APK exclusion, real MAY_DUCK request once, real response retained, pending/stopped/mode/owner/D5/lease/error exclusions, legacy observe-only and scalar transport windows. No hardware validation.");
}}
'''
})
with tempfile.TemporaryDirectory(dir=root/'tests') as tmp:
 d=pathlib.Path(tmp);out=d/'classes';out.mkdir()
 for n,s in files.items():
  path=d/n;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(s)
 src=[str(p) for p in d.rglob('*.java')]+[str(root/'app/src/main/java/com/hfpvoipfix'/n) for n in ('AaCarFocusHook.java','AaCarFocusPolicy.java','NativeScoLease.java','RxGate.java','LabModes.java')]
 java=pathlib.Path(os.environ['JAVA_HOME'])/'bin'
 subprocess.run([str(java/'javac'),'-encoding','UTF-8','-d',str(out),*src],check=True)
 subprocess.run([str(java/'java'),'-cp',str(out),'com.hfpvoipfix.AaCarFocusTest'],check=True)
