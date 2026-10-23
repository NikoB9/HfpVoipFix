package com.hfpvoipfix;
import java.util.*;
public final class CallDiagnosticPolicyTest {
 public static void main(String[] args){
  Map<Integer,String> p=CallDiagnosticPolicy.processes("PID NAME\n42 com.google.android.projection.gearhead:projection\n43 com.google.android.dialer\n44 com.google.android.projection.gearhead.fake\n45 other\n0 com.android.dialer\nnotpid com.android.dialer\n");
  if(p.size()!=2||!p.containsKey(42)||!p.containsKey(43))throw new AssertionError(p);
  if(CallDiagnosticPolicy.target("com.google.android.projection.gearhead;touch"))throw new AssertionError("unsafe name");
  p=CallDiagnosticPolicy.processes("1 com.google.android.dialer\n2 com.google.android.projection.gearhead\n3 com.google.android.projection.gearhead:projection\n4 com.google.android.projection.gearhead:car\n5 com.google.android.projection.gearhead:shared\n");
  if(!new ArrayList<>(p.keySet()).equals(Arrays.asList(4,3)))throw new AssertionError("AA car/projection priority and two-reader cap "+p);
  if(CallDiagnosticPolicy.target("com.google.android.projection.gearhead:car.fake")||CallDiagnosticPolicy.target("com.google.android.projection.gearhead:shared"))throw new AssertionError("unrelated AA process");
  System.out.println("Call diagnostic process scope passed");
 }
}
