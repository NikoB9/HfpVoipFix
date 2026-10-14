package com.hfpvoipfix;
import java.util.*;
/** Exact process names, never a broad substring or a shell-supplied package name. */
final class CallDiagnosticPolicy {
 static boolean target(String name){
  if(name.equals("com.google.android.projection.gearhead")||name.equals("com.google.android.projection.gearhead:projection"))return true;
  for(String p:new String[]{"com.google.android.dialer","com.android.dialer"})
   if(name.equals(p)||name.startsWith(p+":"))return true;
  return false;
 }
 static Map<Integer,String> processes(String ps){
  Map<Integer,String> out=new LinkedHashMap<>();
  for(String line:ps.split("\n")){String[] v=line.trim().split("\\s+");
   if(v.length!=2||!v[0].matches("[0-9]{1,9}")||!target(v[1]))continue;
   int pid=Integer.parseInt(v[0]);if(pid>0)out.put(pid,v[1]);
  }return out;
 }
}
