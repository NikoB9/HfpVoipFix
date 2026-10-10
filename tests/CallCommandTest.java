package com.hfpvoipfix;
public final class CallCommandTest {
 public static void main(String[] args){
  for(String s:new String[]{"ab12:dial:666","ab12:dial:+33123456789","ab12:answer:-","ab12:hangup:-"})if(CallCommand.parse(s)==null)throw new AssertionError(s);
  for(String s:new String[]{"ab12:dial:666;reboot","ab12:dial:*21#","ab12:dial:$(id)","ab12:answer:666","ab12:dial:","bad!:dial:666","ab12:hold:-","ab12:dial:123456789012345678901"})if(CallCommand.parse(s)!=null)throw new AssertionError(s);
  System.out.println("PASS call protocol: allowlisted actions, no shell metacharacters or supplementary-service codes");
 }
}
