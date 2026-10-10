package com.hfpvoipfix;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** Static firmware only. Never accepts data, proc, sys, device nodes or arbitrary paths. */
final class FirmwareArchive {
    static final long FILE_LIMIT=32L*1024*1024, TOTAL_LIMIT=128L*1024*1024;
    interface Source {
        String paths() throws Exception;
        byte[] read(String path,long limit) throws Exception;
        String identity() throws Exception;
        default String transport() throws Exception {return "Diagnostic transport non demandé";}
    }
    interface Progress { void update(String message); }
    static boolean allowed(String path) {
        if(path.contains("..")||!path.matches("/[A-Za-z0-9_./@+-]+"))return false;
        return path.matches("/(system|system_ext)/(lib|lib64)/(libbluetooth|libbluetooth_jni|libbt-stack|libbt-hci)[.]so")
            ||path.matches("/apex/com[.]android[.](btservices|bluetooth)(@[0-9]+)?/(lib|lib64)/(libbluetooth|libbluetooth_jni|libbt-stack|libbt-hci)[.]so")
            ||path.matches("/(vendor|odm)/(lib|lib64)/(hw/)?(?:audio[.][A-Za-z0-9_.+-]+|lib[a-zA-Z0-9_.+-]*(?:audio|Audio|audiocompensation|speech|Speech|bt|BT|bluetooth|cvsd)[a-zA-Z0-9_.+-]*|android[.]hardware[.](?:audio|bluetooth)[A-Za-z0-9_.@+-]*)[.]so")
            ||path.matches("/(vendor|odm|system)/etc/(?:audio[A-Za-z0-9_.+-]*|mixer_paths[A-Za-z0-9_.+-]*|bluetooth_audio[A-Za-z0-9_.+-]*)[.]xml");
    }
    static String build(File dir,Source source,Progress progress)throws Exception {
        File temp=new File(dir,"HFP-Firmware.zip.tmp"),target=new File(dir,"HFP-Firmware.zip");
        StringBuilder inventory=new StringBuilder("chemin\toctets\tsha256\n"),errors=new StringBuilder();
        long total=0;int count=0;boolean primary=false;
        String identity=source.identity();
        String transport;try{transport=source.transport();}catch(Exception e){transport="Transport indisponible : "+e.getMessage();errors.append(transport).append('\n');}
        String[] paths=source.paths().split("\\r?\\n");
        Set<String> seen=new HashSet<>();
        try {
            try(FileOutputStream file=new FileOutputStream(temp);ZipOutputStream zip=new ZipOutputStream(file)) {
                for(String path:paths){
                    if(path.isEmpty()||!seen.add(path))continue;
                    if(!allowed(path)){errors.append("Exclu par liste autorisée : ").append(path).append('\n');continue;}
                    if(count>=120||total>=TOTAL_LIMIT){errors.append("LIMITE GLOBALE : fichiers suivants omis\n");break;}
                    progress.update("Copie statique : "+count+" fichiers · "+(total/1048576)+" Mio\n"+path);
                    byte[] data;
                    try{data=source.read(path,Math.min(FILE_LIMIT,TOTAL_LIMIT-total));
                        if(data.length==0)throw new IOException("fichier vide");
                        if(data.length>Math.min(FILE_LIMIT,TOTAL_LIMIT-total))throw new IOException("limite dépassée");
                        if(path.endsWith(".so")&&(data.length<4||data[0]!=127||data[1]!='E'||data[2]!='L'||data[3]!='F'))throw new IOException("signature ELF absente");
                    }catch(Exception e){errors.append(path).append(" : ").append(e.getMessage()).append('\n');continue;}
                    put(zip,"firmware"+path,data);
                    StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(data))hash.append(String.format(Locale.ROOT,"%02x",b&255));
                    inventory.append(path).append('\t').append(data.length).append('\t').append(hash).append('\n');
                    total+=data.length;count++;if(path.contains("/hw/audio.primary."))primary=true;
                }
                if(!primary)throw new IOException("HAL audio.primary absent ou illisible. Aucun nouveau ZIP validé. "+errors);
                put(zip,"inventory.tsv",inventory.toString().getBytes(StandardCharsets.UTF_8));
                put(zip,"errors.txt",errors.toString().getBytes(StandardCharsets.UTF_8));
                put(zip,"system-info.txt",identity.getBytes(StandardCharsets.UTF_8));
                put(zip,"hci-transport.txt",transport.getBytes(StandardCharsets.UTF_8));
                put(zip,"README.txt",("HfpVoipLab 1.7.10 · collecte statique\n"+count+" fichiers, "+total+" octets.\n"+
                    "Bibliothèques ELF et XML copiés sans modification, SHA-256 dans inventory.tsv.\n"+
                    "Aucun enregistrement sonore, contact, numéro, journal HCI brut ou NVRAM exporté. Métadonnées HCI sélectionnées dans hci-transport.txt.\n"+
                    "Aucune lecture /proc/asound, aucun accès PCM, aucun tinymix, aucune modification système.\n"+
                    "Ce paquet permet l'analyse des commandes et dépendances du HAL. Il ne prouve pas le transport SCO.\n"+
                    "Les binaires ne sont pas anonymisés : copie exacte du firmware, à partager pour ce diagnostic, pas à publier sur GitHub.\n"+
                    "Les fichiers exclus ou indisponibles figurent dans errors.txt.\n").getBytes(StandardCharsets.UTF_8));
                zip.finish();zip.flush();file.getFD().sync();
            }
            try(ZipFile verify=new ZipFile(temp)){Enumeration<? extends ZipEntry> entries=verify.entries();byte[] buffer=new byte[32768];
                while(entries.hasMoreElements()){ZipEntry e=entries.nextElement();CRC32 crc=new CRC32();long size=0;
                    try(InputStream in=verify.getInputStream(e)){int n;while((n=in.read(buffer))!=-1){crc.update(buffer,0,n);size+=n;}}
                    if(size!=e.getSize()||crc.getValue()!=e.getCrc())throw new IOException("ZIP endommagé : "+e.getName());
                }
            }
            Files.move(temp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);
            return count+" fichiers · "+(total/1048576)+" Mio · "+(errors.length()==0?"aucune omission":"consulter errors.txt pour les omissions");
        } finally {temp.delete();}
    }
    private static void put(ZipOutputStream zip,String name,byte[] data)throws IOException{zip.putNextEntry(new ZipEntry(name));zip.write(data);zip.closeEntry();}
}
