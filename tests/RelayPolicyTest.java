package com.hfpvoipfix;
public final class RelayPolicyTest {
 static void check(boolean x){if(!x)throw new AssertionError();}
 public static void main(String[] args){String p="STATUS version=1.9.5 busy=false dirty=false route=true calls=true disconnect=true voip=3 requested=observe";
 check(RelayPolicy.prepare(p,true,false,true));check(!RelayPolicy.prepare(p,false,false,true));check(!RelayPolicy.prepare(p,true,true,true));check(!RelayPolicy.prepare(p,true,false,false));
 for(String[] a:new String[][]{{"version=1.9.5","version=1.8.4"},{"busy=false","busy=true"},{"dirty=false","dirty=true"},{"route=true","route=false"},{"calls=true","calls=false"},{"disconnect=true","disconnect=false"},{"voip=3","voip=2"}})check(!RelayPolicy.prepare(p.replace(a[0],a[1]),true,false,true));
 check(!RelayPolicy.prepare("",true,false,true));System.out.println("PASS autonomous preparation: disabled, existing bridge, permission, busy, dirty, hook and version gates");}
}
