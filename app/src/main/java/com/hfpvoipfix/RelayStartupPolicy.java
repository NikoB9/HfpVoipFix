package com.hfpvoipfix;
/** Restore the user's choice only after ordinary boot/unlock, never enable a new install. */
final class RelayStartupPolicy {
 static boolean restore(String action,boolean enabled,boolean automatic,boolean microphone){
  return enabled&&automatic&&microphone&&("android.intent.action.BOOT_COMPLETED".equals(action)||"android.intent.action.MY_PACKAGE_REPLACED".equals(action));
 }
 static String normalMode(String mode){return "telecom_duplex".equals(mode)||"projection_only".equals(mode)?mode:"telecom_aa_control";}
}
