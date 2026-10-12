package com.hfpvoipfix;
public final class ScoConnectPolicyTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        ScoConnectPolicy p=new ScoConnectPolicy();
        check(!p.request("a",false,true,0));check(!p.request("a",true,true,0));
        check(!p.request("a",true,true,1999));check(p.request("a",true,true,2000));
        check(!p.request("a",true,true,6999));check(p.request("a",true,true,7000));
        check(!p.request("a",true,false,12000));check(p.request("a",true,true,12000));
        check(!p.request("a",true,true,17000));check(p.attempts()==3);
        check(!p.request("b",true,true,18000));check(p.request("b",true,true,20000));
        check(!p.request(null,false,true,21000));check(p.attempts()==0);
        System.out.println("PASS SCO requests: active call, initial delay, cooldown, open-route preservation, cap and new-call reset");
    }
}
