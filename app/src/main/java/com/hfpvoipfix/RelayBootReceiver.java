package com.hfpvoipfix;
import android.content.*;
public final class RelayBootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){if((Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())||Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction()))&&RelayService.enabled(c))c.startForegroundService(new Intent(c,RelayService.class));}
}
