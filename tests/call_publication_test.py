#!/usr/bin/env python3
"""Production publication hooks with synthetic call metadata; no car or proprietary APK execution."""
import pathlib,tempfile,subprocess,os
root=pathlib.Path(__file__).resolve().parents[1]
files={
'android/os/SystemClock.java':'package android.os;public class SystemClock{public static long elapsedRealtime(){return 10000;}}',
'android/os/SystemProperties.java':'package android.os;public class SystemProperties{public static java.util.Map<String,String> p=new java.util.HashMap<>();public static String get(String k,String d){return p.getOrDefault(k,d);}}',
'android/util/Log.java':'package android.util;public class Log{public static int i(String t,String s){return 0;}}',
'de/robv/android/xposed/XC_MethodHook.java':'''package de.robv.android.xposed;public class XC_MethodHook{
 protected void beforeHookedMethod(MethodHookParam p){}protected void afterHookedMethod(MethodHookParam p){}
 public void before(MethodHookParam p){beforeHookedMethod(p);}public void after(MethodHookParam p){afterHookedMethod(p);}
 public static class MethodHookParam{public Object thisObject,result;public Object[] args;public boolean returned,error;java.util.Map<String,Object>x=new java.util.HashMap<>();public void setResult(Object r){result=r;returned=true;}public Object getResult(){return result;}public boolean hasThrowable(){return error;}public void setObjectExtra(String k,Object v){x.put(k,v);}public Object getObjectExtra(String k){return x.get(k);}}
}''',
'de/robv/android/xposed/XposedHelpers.java':'''package de.robv.android.xposed;import java.lang.reflect.*;public class XposedHelpers{
 public static java.util.Map<String,XC_MethodHook> hooks=new java.util.HashMap<>();public static String fail="";
 public static Class<?> findClass(String n,ClassLoader l){try{return Class.forName(n);}catch(Exception e){throw new RuntimeException(e);}}
 public static Object findAndHookMethod(Class<?> c,String n,Object...a){if(n.equals(fail))throw new RuntimeException("missing");hooks.put(c.getName()+"#"+n,(XC_MethodHook)a[a.length-1]);return null;}
 public static Object getObjectField(Object o,String n){try{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new RuntimeException(e);}}
 public static Object callStaticMethod(Class<?> c,String n,Object...a){return invoke(c,null,n,a);}public static Object callMethod(Object o,String n,Object...a){return invoke(o.getClass(),o,n,a);}
 static Object invoke(Class<?> c,Object o,String n,Object[] a){for(Method m:c.getMethods())if(m.getName().equals(n)&&m.getParameterCount()==a.length)try{return m.invoke(o,a);}catch(Exception e){throw new RuntimeException(e);}throw new RuntimeException(n);}
}''',
'com/android/server/telecom/Call.java':'''package com.android.server.telecom;public class Call{
 public boolean emergency;public Account account=new Account();public boolean isEmergencyCall(){return emergency;}public Account getTargetPhoneAccount(){return account;}
 public static class Account{public Component c=new Component();public Component getComponentName(){return c;}}
 public static class Component{public String pkg="com.android.bluetooth",cls="com.android.bluetooth.hfpclient.connserv.HfpClientConnectionService";public String getPackageName(){return pkg;}public String getClassName(){return cls;}}
}''',
'com/android/server/telecom/BluetoothPhoneServiceImpl.java':'''package com.android.server.telecom;public class BluetoothPhoneServiceImpl{public Manager mCallsManager=new Manager();public static class Manager{public java.util.List<Call> calls=new java.util.ArrayList<>();public java.util.List<Call> getCalls(){return calls;}}}''',
'xjd.java':'public class xjd{}','ojc.java':'public class ojc{}',
'com/hfpvoipfix/AaCarFocusHook.java':'package com.hfpvoipfix;class AaCarFocusHook{static boolean exactApk(String p){return "exact".equals(p);}}',
'com/hfpvoipfix/CallPublicationTest.java':'''package com.hfpvoipfix;
import java.util.*;import android.os.*;import de.robv.android.xposed.*;import com.android.server.telecom.*;
public class CallPublicationTest{
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static XC_MethodHook.MethodHookParam param(Object o,Object result,Object...a){XC_MethodHook.MethodHookParam p=new XC_MethodHook.MethodHookParam();p.thisObject=o;p.result=result;p.args=a;return p;}
 static boolean aggregate(BluetoothPhoneServiceImpl s){XC_MethodHook.MethodHookParam p=param(s,null,false);XposedHelpers.hooks.get("com.android.server.telecom.BluetoothPhoneServiceImpl#updateHeadsetWithCallState").before(p);return p.returned;}
 static boolean clcc(BluetoothPhoneServiceImpl s,Call c){XC_MethodHook.MethodHookParam p=param(s,null,c,false);XposedHelpers.hooks.get("com.android.server.telecom.BluetoothPhoneServiceImpl#sendClccForCall").before(p);return p.returned;}
 public static class AaCall{public Details f=new Details();}public static class Details{public Call.Account h=new Call.Account();}
 static List<?> view(List<?> list,boolean error){XC_MethodHook.MethodHookParam p=param(null,list);p.error=error;XposedHelpers.hooks.get("ojc#k").after(p);return (List<?>)p.result;}
 public static void main(String[]args)throws Exception{
 Map<String,String> props=SystemProperties.p;props.put(LabModes.PROP,"telecom_aa_control");props.put("ro.product.device","begonia");props.put("ro.build.version.release","11");props.put("debug.hfpvoipfix.native_split","1.9.0-persistent");
 ClassLoader loader=CallPublicationTest.class.getClassLoader();BluetoothPhoneServiceImpl s=new BluetoothPhoneServiceImpl();Call c=new Call();s.mCallsManager.calls.add(c);
 if(args.length>0){XposedHelpers.fail="sendClccForCall";VehicleCallPublicationHook.install(loader);check(!aggregate(s),"partial install must remain inactive");System.out.println("PASS partial AG hook installation leaves publications intact");return;}
 VehicleCallPublicationHook.install(loader);check(aggregate(s)&&clcc(s,c),"sole owned HFP publication excluded");check(s.mCallsManager.calls.size()==1&&s.mCallsManager.calls.get(0)==c,"real call never removed");
 check(!clcc(s,new Call()),"different call excluded from ownership");c.emergency=true;check(!aggregate(s)&&!clcc(s,c),"emergency AG passthrough");c.emergency=false;
 c.account.c.pkg="com.android.phone";check(!aggregate(s)&&!clcc(s,c),"SIM passthrough");c.account.c.pkg="com.android.bluetooth";
 s.mCallsManager.calls.add(new Call());check(!aggregate(s)&&!clcc(s,c),"multiple-call passthrough");s.mCallsManager.calls.remove(1);check(!aggregate(s),"do not hide an already exposed remaining call");s.mCallsManager.calls.clear();check(!aggregate(s),"idle original publication clears exposure");s.mCallsManager.calls.add(c);check(aggregate(s),"new sole call eligible after idle");
 for(String mode:new String[]{"telecom_duplex","projection_only","telecom_aa","observe"}){props.put(LabModes.PROP,mode);check(!aggregate(s),"other modes unchanged");}props.put(LabModes.PROP,"telecom_aa_control");
 props.put("ro.product.device","other");check(!aggregate(s),"model exclusion");props.put("ro.product.device","begonia");
 AaCallPublicationHook.install(loader,"mismatch");check(!XposedHelpers.hooks.containsKey("xjd#aZ"),"APK mismatch installs no AA filter");AaCallPublicationHook.installVerified(loader);
 AaCall aa=new AaCall();List<AaCall> original=Collections.singletonList(aa);check(view(original,false)==original,"AA UI sees original list outside serializer");
 XC_MethodHook scope=XposedHelpers.hooks.get("xjd#aZ");XC_MethodHook.MethodHookParam outer=param(null,null),inner=param(null,null);scope.before(outer);
 check(view(original,false).isEmpty()&&original.size()==1&&original.get(0)==aa,"serializer receives a separate empty view, real call retained");check(view(original,true)==original,"failed getter unchanged");
 scope.before(inner);scope.after(inner);check(view(original,false).isEmpty(),"nested serializer scope preserved");
 aa.f.h.c.pkg="com.android.phone";check(view(original,false)==original,"AA SIM metadata unchanged");aa.f.h.c.pkg="com.android.bluetooth";check(view(original,false)==original,"exposed call remains published until idle");view(Collections.emptyList(),false);check(view(original,false).isEmpty(),"idle starts fresh AA publication session");
 List<AaCall> multiple=Arrays.asList(aa,new AaCall());check(view(multiple,false)==multiple,"AA multiple-call metadata unchanged");
 props.put(LabModes.PROP,"telecom_duplex");check(view(original,false)==original,"D5 unchanged inside scope");props.put(LabModes.PROP,"telecom_aa_control");
 scope.after(outer);check(view(original,false)==original,"scope removed after normal or exceptional serializer exit");
 check(CallPublicationPolicy.environment("telecom_aa_control","begonia","11","1.9.0-persistent"),"environment");check(!CallPublicationPolicy.environment("telecom_aa_control","begonia","12","1.9.0-persistent"),"Android exclusion");
 System.out.println("PASS production AG/AA publication filters: account, single-call, AG emergency, device/version/mode/APK exclusions, exact scope, immutable source/UI, nesting and partial installation");
 }}'''}
with tempfile.TemporaryDirectory(dir=root/'tests') as tmp:
 d=pathlib.Path(tmp);out=d/'classes';out.mkdir()
 for n,s in files.items():
  p=d/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s)
 sources=[str(p) for p in d.rglob('*.java')]+[str(root/'app/src/main/java/com/hfpvoipfix'/n) for n in ['LabModes.java','HfpAccountPolicy.java','CallPublicationPolicy.java','VehicleCallPublicationHook.java','AaCallPublicationHook.java']]
 java=pathlib.Path(os.environ['JAVA_HOME'])/'bin'
 subprocess.run([str(java/'javac'),'-encoding','UTF-8','-d',str(out),*sources],check=True)
 for args in [[],['partial']]:subprocess.run([str(java/'java'),'-cp',str(out),'com.hfpvoipfix.CallPublicationTest',*args],check=True)
