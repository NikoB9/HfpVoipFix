#!/usr/bin/env python3
"""Production command controller: real protocol, synthetic root/Android endpoints."""
import pathlib,tempfile,subprocess,os
root=pathlib.Path(__file__).resolve().parents[1]
files={
'android/content/Context.java':'''package android.content;public class Context{public Pref getSharedPreferences(String n,int m){return new Pref();}public static class Pref{public boolean getBoolean(String k,boolean d){return com.hfpvoipfix.ControllerTest.armed;}}}''',
'android/os/SystemClock.java':'package android.os;public class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000;}}',
'com/hfpvoipfix/RelayService.java':'package com.hfpvoipfix;class RelayService{static boolean enabled(android.content.Context c){return true;}static String mode(android.content.Context c){return "projection_only";}}',
'com/hfpvoipfix/RxBridgeService.java':'package com.hfpvoipfix;class RxBridgeService{static boolean alive=true;}',
'com/hfpvoipfix/LabProbe.java':'''package com.hfpvoipfix;class LabProbe{static String read(android.content.Context c)throws Exception{if(ControllerTest.block){ControllerTest.entered.countDown();ControllerTest.release.await();}return ControllerTest.status();}}''',
'com/hfpvoipfix/Root.java':'''package com.hfpvoipfix;class Root{static String command(String cmd,int a,int b){ControllerTest.commands.add(cmd);if(cmd.startsWith("setprop")){ControllerTest.token=cmd.split("'")[1].split(":")[0];}return "";}}''',
'com/hfpvoipfix/ControllerTest.java':'''package com.hfpvoipfix;import java.util.*;import java.util.concurrent.*;import android.content.Context;
public class ControllerTest{
 public static boolean armed=true;static boolean block;static String token="none",result="queued",mode="projection_only";static List<String> commands=new ArrayList<>();static CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
 static String status(){return "STATUS version=1.9.18 control=true requested="+mode+" busy=false dirty=false ctrl_token="+token+" ctrl_result="+result+" ctrl_error=fixture";}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}interface Op{void run()throws Exception;}
 static void refuses(Op o)throws Exception{try{o.run();throw new AssertionError("expected refusal");}catch(java.io.IOException expected){}}
 public static void main(String[]args)throws Exception{
 Context c=new Context();String r=RemoteCallController.send(c,"dial","+33123456789");check(r.contains("transmise")&&!r.contains("connecté"),"ack never asserts call completion");check(commands.size()==2&&commands.get(1).startsWith("if ["),"conditional ownership cleanup");
 int before=commands.size();refuses(()->RemoteCallController.send(c,"dial","1;id"));refuses(()->RemoteCallController.send(c,"dial","$(id)"));check(commands.size()==before,"invalid commands never reach root");
 armed=false;refuses(()->RemoteCallController.send(c,"dial","123"));check(commands.size()==before,"unarmed dial refused");armed=true;
 mode="telecom_duplex";refuses(()->RemoteCallController.send(c,"answer","-"));check(commands.size()==before,"D5 untouched");mode="projection_only";
 result="unavailable";refuses(()->RemoteCallController.send(c,"hangup","-"));check(commands.size()==before+2,"refusal cleans up owned command");result="queued";
 block=true;Throwable[] error={null};Thread worker=new Thread(()->{try{RemoteCallController.send(c,"answer","-");}catch(Throwable e){error[0]=e;}});worker.start();entered.await();refuses(()->RemoteCallController.send(c,"answer","-"));block=false;release.countDown();worker.join();check(error[0]==null,"first command survives concurrent refusal");
 System.out.println("PASS production command controller: safe grammar, readiness, D5/mode refusal, genuine token ACK, rejection, single-flight concurrency and owned cleanup");
 }}'''}
with tempfile.TemporaryDirectory(dir=root/'tests') as tmp:
 d=pathlib.Path(tmp);out=d/'classes';out.mkdir()
 for n,s in files.items():
  p=d/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s)
 sources=[str(p) for p in d.rglob('*.java')]+[str(root/'app/src/main/java/com/hfpvoipfix'/n) for n in ['RemoteCallController.java','CallCommand.java','LabModes.java','RxGate.java']]
 java=pathlib.Path(os.environ['JAVA_HOME'])/'bin'
 subprocess.run([str(java/'javac'),'-encoding','UTF-8','-d',str(out),*sources],check=True)
 subprocess.run([str(java/'java'),'-cp',str(out),'com.hfpvoipfix.ControllerTest'],check=True,timeout=20)
