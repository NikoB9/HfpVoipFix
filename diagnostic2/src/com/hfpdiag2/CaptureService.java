package com.hfpdiag2;
import android.app.*;import android.content.*;import android.os.*;import java.io.*;import java.nio.charset.StandardCharsets;
import java.util.*;import java.util.concurrent.*;import java.util.zip.*;
public final class CaptureService extends Service {
 public static final String START="hfp.start",STOP="hfp.stop",SNAP="hfp.snap";
 private final ExecutorService work=Executors.newSingleThreadExecutor();
 private java.lang.Process logProcess;
 private Thread logThread;
 private volatile boolean capturing=false;
 private volatile int count=0;
 private volatile String error="";
 private File file(String n){return new File(getFilesDir(),n);}
 private void state(String phase,String msg){
  getSharedPreferences("capture",0).edit().putString("phase",phase).putString("status",msg).putInt("count",count).apply();
 }
 private void save(String n,String s)throws Exception {
  try(FileOutputStream out=new FileOutputStream(file(n))){out.write(s.getBytes(StandardCharsets.UTF_8));}
 }
 @Override public void onCreate(){super.onCreate();if(Build.VERSION.SDK_INT>=26)
  ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(
    new NotificationChannel("diag","Capture HFP",NotificationManager.IMPORTANCE_LOW));}
 private void foreground(){
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"diag"):new Notification.Builder(this);
  startForeground(703,b.setContentTitle("Diagnostic HFP").setContentText("Capture en cours")
    .setSmallIcon(android.R.drawable.ic_menu_info_details).build());
 }
 @Override public int onStartCommand(Intent intent,int flags,int id){
  if(intent==null)return START_NOT_STICKY;
  foreground();String action=intent.getAction();
  if(START.equals(action))work.execute(this::start);
  else if(SNAP.equals(action))work.execute(()->{try{save("pendant.txt",Root.snapshot("pendant"));state("running","Instantané audio enregistré");}catch(Exception e){error+="instantané: "+e+"\n";}});
  else if(STOP.equals(action))work.execute(this::stop);
  return START_NOT_STICKY;
 }
 private void start(){
  if(capturing)return;
  count=0;error="";
  for(String f:new String[]{"logcat.txt","avant.txt","pendant.txt","apres.txt","config.txt","HFP-Rapport.zip"})file(f).delete();
  state("starting","Vérification des droits Magisk...");
  try{
   Root.check();
   capturing=true;
   logProcess=new ProcessBuilder("su","-c","exec logcat -b main -b system -v threadtime -T 1")
     .redirectErrorStream(true).start();
   logThread=new Thread(()->{
    try(BufferedReader in=new BufferedReader(new InputStreamReader(logProcess.getInputStream(),StandardCharsets.UTF_8));
        BufferedWriter out=new BufferedWriter(new FileWriter(file("logcat.txt")))){
     String line;while(capturing&&(line=in.readLine())!=null){
      if(count<30000){out.write(line);out.newLine();count++;}
      if(count%100==0)out.flush();
     }
     out.flush();
    }catch(Exception e){error+="logcat: "+e+"\n";}
   },"hfp-log-reader");logThread.start();
   state("running","Capture active, appeler le 666 depuis le Redmi.");
   save("avant.txt",Root.snapshot("avant"));
   save("config.txt",Root.config());
  }catch(Exception e){error+="démarrage: "+e+"\n";state("error","Capture impossible : "+e.getMessage());capturing=false;stopForeground(true);}
 }
 private void stop(){
  state("processing","Création du rapport...");
  capturing=false;
  if(logProcess!=null)logProcess.destroy();
  if(logThread!=null)try{logThread.join(2500);}catch(Exception ignored){}
  try{save("apres.txt",Root.snapshot("apres"));}catch(Exception e){error+="apres: "+e+"\n";}
  try{
   save("etat.txt","Événements collectés : "+count+"\nErreurs : "+(error.isEmpty()?"aucune":error)+"\n"+
     "Root: "+(("0".equals(rootStatus())?"oui":"non"))+"\nUn compteur nul signifie que logcat n'a pas fourni de lignes.\n");
   try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(file("HFP-Rapport.zip")))){
    for(String f:new String[]{"etat.txt","logcat.txt","avant.txt","pendant.txt","apres.txt","config.txt"}){
     File source=file(f);if(!source.exists())continue;
     zip.putNextEntry(new ZipEntry(f));
     try(BufferedReader reader=new BufferedReader(new FileReader(source))){
      String line;int rows=0;while((line=reader.readLine())!=null&&rows++<30000){
       String safe=line.replaceAll("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}","[MAC]")
         .replaceAll("(?i)tel:[^ ,;]+","tel:[masqué]");
       zip.write((safe+"\n").getBytes(StandardCharsets.UTF_8));
      }
     }zip.closeEntry();
    }
   }
   state("ready","Rapport ZIP prêt : "+count+" lignes de logs. Partager ci-dessous.");
  }catch(Exception e){state("error","Rapport échoué : "+e.getMessage());}
  stopForeground(true);stopSelf();
 }
 private String rootStatus(){try{return Root.check();}catch(Exception e){return "non";}}
 @Override public IBinder onBind(Intent intent){return null;}
 @Override public void onDestroy(){capturing=false;if(logProcess!=null)logProcess.destroy();work.shutdown();super.onDestroy();}
}
