#!/usr/bin/env python3
"""Exercise production endpoint selection against narrow public AudioDeviceInfo fakes."""
import pathlib,tempfile,subprocess,os
root=pathlib.Path(__file__).resolve().parents[1]
files={
'android/media/AudioDeviceInfo.java':'''package android.media; public class AudioDeviceInfo {
 public static final int TYPE_BLUETOOTH_SCO=7;int id,type;String address;
 public AudioDeviceInfo(int id,int type,String address){this.id=id;this.type=type;this.address=address;}
 public int getId(){return id;}public int getType(){return type;}public String getAddress(){return address;}}
''',
'android/media/AudioManager.java':'''package android.media;public class AudioManager {
 public static final int GET_DEVICES_INPUTS=1,GET_DEVICES_OUTPUTS=2;
 public AudioDeviceInfo[] in={},out={};public AudioDeviceInfo[] getDevices(int flags){return flags==1?in:out;}}
''',
'com/hfpvoipfix/CarEndpointsTest.java':'''package com.hfpvoipfix;import android.media.*;import java.io.*;
public class CarEndpointsTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 interface Op{void run()throws IOException;}static void refuses(Op o)throws IOException{try{o.run();throw new AssertionError();}catch(IOException expected){}}
 public static void main(String[] args)throws Exception{
 AudioManager a=new AudioManager();AudioDeviceInfo mi=new AudioDeviceInfo(101,7,"MASTER"),mo=new AudioDeviceInfo(102,7,"MASTER"),ci=new AudioDeviceInfo(33,7,"CAR"),co=new AudioDeviceInfo(32,7,"CAR");
 a.in=new AudioDeviceInfo[]{ci,mi};a.out=new AudioDeviceInfo[]{co,mo};
 check(CarEndpoints.master(a,1,101)==mi);check(CarEndpoints.master(a,2,102)==mo);
 check(CarEndpoints.vehicle(a,mi,mo)[0]==ci);check(CarEndpoints.vehicle(a,mi,mo)[1]==co);
 refuses(()->CarEndpoints.master(a,1,-1));refuses(()->CarEndpoints.master(a,1,32));
 a.in=new AudioDeviceInfo[]{mi};refuses(()->CarEndpoints.vehicle(a,mi,mo));
 a.out=new AudioDeviceInfo[]{mo};check(CarEndpoints.vehicle(a,mi,mo)==null);
 a.out=new AudioDeviceInfo[]{mo,new AudioDeviceInfo(32,7,"MASTER")};refuses(()->CarEndpoints.vehicle(a,mi,mo));
 a.out=new AudioDeviceInfo[]{co,mo,new AudioDeviceInfo(66,7,"CAR2")};refuses(()->CarEndpoints.vehicle(a,mi,mo));
 a.out=new AudioDeviceInfo[]{new AudioDeviceInfo(53,25,"1506431339:ap:0mixp:0")};check(CarEndpoints.projection(a));check(CarEndpoints.projectionOutput(a.out[0]));check(!CarEndpoints.projectionOutput(null));check(!CarEndpoints.projectionOutput(mo));
 a.out=new AudioDeviceInfo[]{new AudioDeviceInfo(53,25,"0")};check(!CarEndpoints.projection(a));check(!CarEndpoints.projectionOutput(a.out[0]));
 check(LabModes.car("telecom_aa"));check(!LabModes.car("telecom_duplex"));check(LabModes.telecom("telecom_aa"));check(!LabModes.bypass("telecom_aa"));
 System.out.println("PASS master IDs, separate vehicle pair, ambiguous/missing/identity collision refusal, projection address and D5/D6 isolation");
 }}'''}
with tempfile.TemporaryDirectory(dir=root/'tests') as d:
 d=pathlib.Path(d);out=d/'classes';out.mkdir()
 for name,content in files.items():
  p=d/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content)
 src=[str(p) for p in d.rglob('*.java')]+[str(root/'app/src/main/java/com/hfpvoipfix'/s) for s in ('CarEndpoints.java','LabModes.java')]
 java=pathlib.Path(os.environ['JAVA_HOME'])/'bin'
 subprocess.run([str(java/'javac'),'-encoding','UTF-8','-d',str(out),*src],check=True)
 subprocess.run([str(java/'java'),'-cp',str(out),'com.hfpvoipfix.CarEndpointsTest'],check=True)
