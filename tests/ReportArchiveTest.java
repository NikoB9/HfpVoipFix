package com.hfpvoipfix;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.zip.*;
public final class ReportArchiveTest {
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("hfp-export-test");
        try{
            for(String name:ReportArchive.REQUIRED)Files.write(dir.resolve(name),"écriture témoin\n".getBytes(StandardCharsets.UTF_8));
            Files.write(dir.resolve("events-00000.log"),"PREMIER\nAudioALSA BT_SCO_RX_Start\n".getBytes(StandardCharsets.UTF_8));
            Files.write(dir.resolve("events-00008.log"),"DERNIER\n".getBytes(StandardCharsets.UTF_8));
            ReportArchive.build(dir.toFile());
            Path apk=dir.resolve("HFP-Rapport.zip");byte[] first=Files.readAllBytes(apk);
            try(ZipFile z=new ZipFile(apk.toFile())){
                for(String name:ReportArchive.REQUIRED)if(z.getEntry(name)==null)throw new AssertionError(name);
                String events=new String(z.getInputStream(z.getEntry("hfp-events.log")).readAllBytes(),StandardCharsets.UTF_8);
                if(events.indexOf("PREMIER")>=events.indexOf("DERNIER"))throw new AssertionError("chronology");
                String mtk=new String(z.getInputStream(z.getEntry("mediatek-audio.log")).readAllBytes(),StandardCharsets.UTF_8);
                if(!mtk.contains("BT_SCO_RX_Start"))throw new AssertionError("MTK extraction");
            }
            Files.delete(dir.resolve("sessions.json"));
            try{ReportArchive.build(dir.toFile());throw new AssertionError("missing input accepted");}catch(IOException expected){}
            if(!java.util.Arrays.equals(first,Files.readAllBytes(apk)))throw new AssertionError("previous report overwritten on failure");
            System.out.println("PASS ZIP required entries, UTF-8, chronology, MediaTek extraction, CRC and failed-export preservation");
        }finally{try(java.util.stream.Stream<Path>s=Files.walk(dir)){s.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(IOException e){throw new RuntimeException(e);}});}}
    }
}
