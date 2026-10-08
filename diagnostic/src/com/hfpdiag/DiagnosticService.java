package com.hfpdiag;
import android.app.*;import android.content.*;import android.os.*;import android.util.Log;
import java.io.*;import java.util.*;import java.util.concurrent.*;import java.util.zip.*;
public final class DiagnosticService extends Service {
 private ExecutorService worker=Executors.newSingleThreadExecutor();private Process logger;
 private File file(String s){return new File(getFilesDir(),s);}
 private void write(String name,String value)throws Exception{try(FileOutputStream out=new FileOutputStream(file(name))){out.write(value.getBytes("UTF-8"));}}
 @Override public void onCreate(){super.onCreate();if(Build.VERSION.SDK_INT>=26){NotificationManager m=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);m.createNotificationChannel(new NotificationChannel("hfpdiag","HFP Diagnostic",NotificationManager.IMPORTANCE_LOW));}}
 private void notifyUser(){Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"hfpdiag"):new Notification.Builder(this);startForeground(311,b.setSmallIcon(android.R.drawable.ic_menu_info_details).setContentTitle("HFP Diagnostic active").setContentText("Recording diagnostic logcat").build());}
 @Override public int onStartCommand(Intent i,int flags,int id){if(i==null)return START_NOT_STICKY;notifyUser();String a=i.getAction();worker.execute(()->{try{
 if("start".equals(a)){if(logger!=null)return;for(String n:new String[]{"before.txt","during.txt","after.txt","vendor-config.txt","events.txt"})file(n).delete();write("before.txt",Root.snapshot(false));write("vendor-config.txt",Root.vendorConfig());logger=new ProcessBuilder("su","-c","exec logcat -b main -b system -v threadtime -f "+file("events.txt").getAbsolutePath()).start();}
 if("during".equals(a))write("during.txt",Root.snapshot(true));
 if("stop".equals(a)){if(logger!=null){logger.destroy();logger.waitFor(2,TimeUnit.SECONDS);logger=null;}write("after.txt",Root.snapshot(false));makeZip();stopForeground(true);stopSelf();}
 }catch(Exception e){Log.e("HfpDiag","Diagnostic failure",e);try{write("error.txt",e.toString());}catch(Exception ignored){}if("stop".equals(a)){stopForeground(true);stopSelf();}}});return START_NOT_STICKY;}
 private void makeZip()throws Exception{String[] names={"before.txt","during.txt","after.txt","vendor-config.txt","events.txt","error.txt"};try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(file("HFP-Diagnostic.zip")))){for(String name:names){File f=file(name);if(!f.isFile())continue;zip.putNextEntry(new ZipEntry(name));try(BufferedReader r=new BufferedReader(new FileReader(f))){String line;int count=0;while((line=r.readLine())!=null&&count<100000){line=line.replaceAll("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}","[MAC]").replaceAll("(?i)tel:[^ ,;\\]]+","tel:[REDACTED]");byte[] bytes=(line+"\n").getBytes("UTF-8");zip.write(bytes);count++;}}zip.closeEntry();}}}
 @Override public IBinder onBind(Intent i){return null;}
 @Override public void onDestroy(){if(logger!=null)logger.destroy();worker.shutdown();super.onDestroy();}
}
