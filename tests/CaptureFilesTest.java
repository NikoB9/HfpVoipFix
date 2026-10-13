package com.hfpvoipfix;
import java.nio.file.*;
public final class CaptureFilesTest {
 public static void main(String[] args)throws Exception{
  Path d=Files.createTempDirectory("capture-clean");
  try{
   for(String n:ReportArchive.REQUIRED)Files.write(d.resolve(n),new byte[]{1});
   for(String n:ReportArchive.OPTIONAL)Files.write(d.resolve(n),new byte[]{1});
   for(String n:new String[]{"events-00000.log","sessions.json","HFP-Rapport.zip","previous-session.zip","HFP-Firmware.zip","other.txt"})Files.write(d.resolve(n),new byte[]{1});
   CaptureFiles.clearSession(d.toFile());
   for(String n:ReportArchive.REQUIRED)if(Files.exists(d.resolve(n)))throw new AssertionError(n);
   for(String n:ReportArchive.OPTIONAL)if(Files.exists(d.resolve(n)))throw new AssertionError(n);
   for(String n:new String[]{"events-00000.log","sessions.json","HFP-Rapport.zip"})if(Files.exists(d.resolve(n)))throw new AssertionError(n);
   for(String n:new String[]{"previous-session.zip","HFP-Firmware.zip","other.txt"})if(!Files.exists(d.resolve(n)))throw new AssertionError(n);
   System.out.println("PASS stale diagnostic isolation and previous report/firmware preservation");
  }finally{try(java.util.stream.Stream<Path>s=Files.walk(d)){s.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(Exception e){throw new RuntimeException(e);}});}}
 }
}
