package com.hfpvoipfix;
public final class ScoConnectPolicyTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
 ScoConnectPolicy p=new ScoConnectPolicy();
 check(p.action("a",false,true,0,0)==0);check(p.action("a",true,true,0,0)==0);
 check(p.action("a",true,true,0,1999)==0);check(p.action("a",true,true,0,2000)==1);
 check(p.action("a",true,true,1,2100)==0);check(p.action("a",true,true,1,14099)==0);
 check(p.attempts()==1);check(p.action("a",true,true,1,14100)==2);
 check(p.action("a",true,true,1,100000)==0);check(p.attempts()==1);
 check(p.action("a",true,true,2,100001)==0);check(p.action("a",true,true,-1,100002)==0);
 check(p.action("a",true,true,0,100003)==1);check(p.action("a",true,true,0,105003)==1);
 check(p.action("a",true,true,0,110003)==0&&p.attempts()==3);
 check(p.action("b",true,true,1,120000)==0);check(p.action("b",true,true,1,132000)==2);
 check(p.action("b",true,true,0,132001)==0);check(p.action("b",true,true,0,137000)==1);
 check(p.action("b",true,false,1,200000)==0);check(p.action(null,false,true,1,210000)==0&&p.attempts()==0);
 System.out.println("PASS bounded master recovery: waits do not consume connects, one pending cancellation per call, cooldown, connected/unknown/owned route guards and new-call reset");
 }
}
