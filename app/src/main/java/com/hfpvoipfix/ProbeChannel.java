package com.hfpvoipfix;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Dedicated live logcat subscription, not a dump of the last N system messages. */
final class ProbeChannel implements AutoCloseable {
    private final java.lang.Process shell,log;
    private final BufferedWriter commands;
    private final BlockingQueue<String> replies=new ArrayBlockingQueue<>(64),events=new ArrayBlockingQueue<>(64);
    private volatile boolean broken;
    ProbeChannel()throws Exception{this(new ProcessBuilder("su","-c","exec /system/bin/sh").redirectErrorStream(true).start());}
    private ProbeChannel(java.lang.Process shell)throws Exception{this(shell,openLog(shell));}
    private static java.lang.Process openLog(java.lang.Process shell)throws Exception{try{return new ProcessBuilder("su","-c","exec logcat -b main -v brief -s HfpVoipFix:I '*:S' -T 1").redirectErrorStream(true).start();}catch(Exception e){shell.destroy();throw e;}}
    ProbeChannel(java.lang.Process shell,java.lang.Process log)throws Exception{
        this.shell=shell;this.log=log;commands=new BufferedWriter(new OutputStreamWriter(shell.getOutputStream(),StandardCharsets.UTF_8));
        reader(shell,replies,false);reader(log,events,true);
        try{String marker="UID"+Long.toHexString(System.nanoTime());send("echo "+marker+"$(id -u)");long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);boolean ok=false;
            while(System.nanoTime()<end){String s=next(replies,200);if((marker+"0").equals(s)){ok=true;break;}if(s!=null&&s.startsWith(marker))throw new IOException("Root refusé.");}
            if(!ok)throw new IOException("Aucune réponse uid du canal root.");
        }catch(Exception e){close();throw e;}
    }
    private void reader(java.lang.Process p,BlockingQueue<String> q,boolean filter){Thread t=new Thread(()->{
        try(BufferedReader in=new BufferedReader(new InputStreamReader(p.getInputStream(),StandardCharsets.UTF_8))){String s;while((s=in.readLine())!=null){
            if(filter&&!(s.contains("LAB epoch=")&&s.contains(" STATUS version=")))continue;
            if(!q.offer(s)){broken=true;break;}
        }}catch(IOException ignored){}finally{broken=true;}
    },filter?"hfp-live-status":"hfp-root-replies");t.setDaemon(true);t.start();}
    private void send(String s)throws IOException{commands.write(s);commands.newLine();commands.flush();}
    private String next(BlockingQueue<String> q,long ms)throws Exception{if(broken)throw new IOException("Canal root/logcat fermé ou saturé.");return q.poll(ms,TimeUnit.MILLISECONDS);}
    static String matching(String s,String token){return s!=null&&s.contains("LAB epoch=")&&s.contains(" STATUS version=")&&s.contains(" token="+token+" ")?s:null;}
    String read()throws Exception{
        if(broken)throw new IOException("Suivi logcat interrompu.");
        String token=Long.toHexString(System.nanoTime());events.clear();
        send("setprop debug.hfpvoipfix.probe "+token+" && echo OK"+token+" || echo FAIL"+token);
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);boolean ack=false;
        while(System.nanoTime()<deadline&&!ack){String s=next(replies,100);if(("FAIL"+token).equals(s))throw new IOException("setprop refusé.");ack=("OK"+token).equals(s);}
        if(!ack)throw new IOException("Accusé setprop absent après 3 s.");
        while(System.nanoTime()<deadline){String found=matching(next(events,100),token);if(found!=null)return found;}
        throw new IOException("Aucune réponse au jeton courant sur le flux logcat continu (3 s).");
    }
    @Override public void close(){broken=true;try{commands.close();}catch(IOException ignored){}shell.destroy();log.destroy();}
}
