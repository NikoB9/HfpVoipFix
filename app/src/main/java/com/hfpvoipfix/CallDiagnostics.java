package com.hfpvoipfix;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Session-only, per-PID logcat. One first segment and two rotating tail segments. */
final class CallDiagnostics {
 private final File directory;
 private final Map<Integer,Reader> readers=new LinkedHashMap<>();
 private final Map<Integer,Integer> attempts=new HashMap<>();
 private final List<Thread> threads=new ArrayList<>();
 private volatile boolean running;
 private boolean closed;
 private Thread watcher;
 private BufferedWriter writer;
 private int segment;
 private long bytes,lines,discarded;
 private final long[] segmentLines=new long[3];
 private String error="none";
 CallDiagnostics(File directory){this.directory=directory;}
 void start()throws IOException{
  running=true;write("DIAGNOSTIC_START ms="+System.currentTimeMillis()+" max_readers=6 first_and_tail_limit_bytes=3145728");
  watcher=new Thread(()->{
   while(running){try{
    Map<Integer,String> found=CallDiagnosticPolicy.processes(Root.command("ps -A -o PID,NAME",100000,3));
    synchronized(readers){
     threads.removeIf(t->!t.isAlive());
     Iterator<Map.Entry<Integer,Reader>> it=readers.entrySet().iterator();
     while(it.hasNext()){Map.Entry<Integer,Reader> e=it.next();if(!found.containsKey(e.getKey())||!e.getValue().thread.isAlive()){e.getValue().process.destroyForcibly();it.remove();}}
     for(Map.Entry<Integer,String> e:found.entrySet())if(running&&!readers.containsKey(e.getKey())){
      int attempt=attempts.getOrDefault(e.getKey(),0);if(attempt>=3)continue;
      if(readers.size()>=6){write("READER_LIMIT pid="+e.getKey());break;}
      final int pid=e.getKey();
      attempts.put(pid,attempt+1);
      java.lang.Process process=new ProcessBuilder("su","-c","exec logcat -b main -b system -v threadtime -T 200 --pid="+pid+" '*:V'").redirectErrorStream(true).start();
      Thread thread=new Thread(()->read(pid,process),"hfp-aa-log-"+pid);
      readers.put(pid,new Reader(process,thread));threads.add(thread);
      write("READER_START ms="+System.currentTimeMillis()+" pid="+pid+" name="+e.getValue()+" attempt="+(attempt+1)+" lookback_lines=200");thread.start();
     }
    }
    status();
   }catch(Exception e){noteError(e);}
   try{Thread.sleep(2000);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}
   }
  },"hfp-aa-process-watch");watcher.start();
 }
 private void read(int pid,java.lang.Process process){
  try(BufferedReader in=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))){
   String line;while(running&&(line=in.readLine())!=null)write(line);
   if(running)write("READER_ENDED ms="+System.currentTimeMillis()+" pid="+pid+" logs_may_be_incomplete=true");
  }catch(Exception e){if(running)noteError(e);}
 }
 private synchronized void noteError(Exception e){error=e.getClass().getSimpleName();try{write("DIAGNOSTIC_ERROR type="+error);status();}catch(IOException ignored){}}
 private synchronized void write(String raw)throws IOException{
  String safe=Privacy.clean(raw);long n=safe.getBytes(StandardCharsets.UTF_8).length+1;
  if(writer==null||bytes+n>1048576){
   if(writer!=null){writer.close();segment=segment==0?1:segment==1?2:1;discarded+=segmentLines[segment];}
   bytes=0;segmentLines[segment]=0;
   writer=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(directory,"aa-events-"+segment+".log")),StandardCharsets.UTF_8));
   writer.write("SEGMENT_STARTED ms="+System.currentTimeMillis()+" slot="+segment+"\n");
  }
  writer.write(safe);writer.newLine();bytes+=n;lines++;segmentLines[segment]++;
 }
 private synchronized void status()throws IOException{
  if(writer!=null)writer.flush();
  Files.write(new File(directory,"aa-diagnostic-state.txt").toPath(),("running="+running+"\nlines="+lines+"\ndiscarded_tail_lines="+discarded+"\nerror="+error+"\n"+
   "scope=Android_Auto_and_dialer_main_system_per_PID\nprocess_poll_ms=2000\nfirst_and_tail_limit_bytes=3145728\n"+
   "No matching app logs is not proof that no request was made. Release builds may omit logs.\n").getBytes(StandardCharsets.UTF_8));
 }
 void close()throws IOException{
  if(closed)return;
  running=false;
  try{
   // The root ps command has a 3s deadline; do not interrupt it and orphan its process.
   if(watcher!=null)watcher.join(5500);
   synchronized(readers){for(Reader r:readers.values())r.process.destroyForcibly();}
   long until=System.currentTimeMillis()+3000;
   for(Thread t:threads)t.join(Math.max(1,until-System.currentTimeMillis()));
   if(watcher!=null&&watcher.isAlive())throw new IOException("AA watcher still running");
   for(Thread t:threads)if(t.isAlive())throw new IOException("AA reader still running");
   synchronized(this){write("DIAGNOSTIC_STOP ms="+System.currentTimeMillis());status();if(writer!=null){writer.close();writer=null;}closed=true;}
  }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("AA capture stop interrupted",e);}
 }
 private static final class Reader{final java.lang.Process process;final Thread thread;Reader(java.lang.Process p,Thread t){process=p;thread=t;}}
}
