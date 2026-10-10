package com.hfpvoipfix;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;

/** Pure Java export path, also exercised by host tests. Never publishes a half-written ZIP. */
final class ReportArchive {
    static final String[] REQUIRED={"summary.txt","sessions.json","audio-policy.txt","audio-flinger.txt","mediatek-audio.log","system-info.txt","errors.txt","audio-config.txt","audio-manager.txt"};
    static void build(File dir)throws IOException{
        File[] parts=dir.listFiles((d,n)->n.startsWith("events-")&&n.endsWith(".log"));
        if(parts==null)throw new IOException("Dossier de capture inaccessible");Arrays.sort(parts);
        File temporary=new File(dir,"report.tmp");
        try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(temporary));
            BufferedWriter mtk=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(dir,"mediatek-audio.log")),StandardCharsets.UTF_8))){
            zip.putNextEntry(new ZipEntry("hfp-events.log"));
            for(File f:parts)try(BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(f),StandardCharsets.UTF_8))){String line;
                zip.write(("\n=== "+f.getName()+" ===\n").getBytes(StandardCharsets.UTF_8));
                while((line=reader.readLine())!=null){zip.write((line+"\n").getBytes(StandardCharsets.UTF_8));
                    String low=line.toLowerCase(Locale.ROOT);
                    if(low.contains("audioalsa")||low.contains("audiobtcvsd")||low.contains("bt_sco_")||low.contains("capturehandler")){mtk.write(line);mtk.newLine();}
                }
            }zip.closeEntry();mtk.flush();
            for(String n:REQUIRED){File f=new File(dir,n);
                if(!f.isFile())throw new IOException("Fichier requis manquant : "+n);
                zip.putNextEntry(new ZipEntry(n));Files.copy(f.toPath(),zip);zip.closeEntry();
            }
        }
        // Full CRC/stream validation before exposing the content URI.
        try(ZipFile check=new ZipFile(temporary)){Enumeration<? extends ZipEntry> entries=check.entries();byte[] buffer=new byte[8192];
            while(entries.hasMoreElements()){ZipEntry entry=entries.nextElement();CRC32 crc=new CRC32();
                try(InputStream in=check.getInputStream(entry)){int n;while((n=in.read(buffer))!=-1)crc.update(buffer,0,n);}
                if(crc.getValue()!=entry.getCrc())throw new IOException("CRC invalide : "+entry.getName());
            }
        }
        Files.move(temporary.toPath(),new File(dir,"HFP-Rapport.zip").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
