package com.hfpvoipfix;

/** A real navigation focus request may be sent; no focus grant is invented. */
final class AaCarFocusPolicy {
 static final String APK_SHA256="c56e4ebba055e9facdd96c4ea54e80c743a8d184c0d7891f25df98f71119d6ac";
 static boolean blockedRelay(boolean lease,int gain,String owner,int carState,boolean inCall,
   boolean pending,boolean stopped,boolean connected,boolean normalMode){
  return lease&&gain==3&&"com.hfpvoipfix".equals(owner)&&carState==5&&inCall
   &&!pending&&!stopped&&connected&&normalMode;
 }
}
