package com.hfpvoipfix;
import java.io.*;
import java.nio.file.*;
import java.lang.reflect.*;
public final class CallDiagnosticsStorageTest {
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory("aa-diagnostic-test");
  try{
   CallDiagnostics d=new CallDiagnostics(dir.toFile());
   Method write=CallDiagnostics.class.getDeclaredMethod("write",String.class);write.setAccessible(true);
   String payload="number=0612345678, addr=AA:BB:CC:DD:EE:FF "+new String(new char[3000]).replace('\0','x');
   for(int i=0;i<1800;i++)write.invoke(d,"sample="+i+" "+payload);
   d.close();
   byte[] first=Files.readAllBytes(dir.resolve("aa-events-0.log"));d.close();
   if(!java.util.Arrays.equals(first,Files.readAllBytes(dir.resolve("aa-events-0.log"))))throw new AssertionError("double close truncated journal");
   long total=0;
   for(int i=0;i<3;i++){
    Path p=dir.resolve("aa-events-"+i+".log");total+=Files.size(p);
    String s=new String(Files.readAllBytes(p),java.nio.charset.StandardCharsets.UTF_8);
    if(s.contains("0612345678")||s.contains("AA:BB:CC"))throw new AssertionError("raw personal payload persisted");
   }
   if(total>3145728)throw new AssertionError("unbounded journal "+total);
   String state=new String(Files.readAllBytes(dir.resolve("aa-diagnostic-state.txt")),java.nio.charset.StandardCharsets.UTF_8);
   if(state.contains("discarded_tail_lines=0")||!state.contains("running=false"))throw new AssertionError(state);
   System.out.println("AA journal bounds, redaction, loss metadata and idempotent close passed");
  }finally{try(java.util.stream.Stream<Path> s=Files.walk(dir)){s.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(IOException e){throw new java.io.UncheckedIOException(e);}});}}
 }
}
