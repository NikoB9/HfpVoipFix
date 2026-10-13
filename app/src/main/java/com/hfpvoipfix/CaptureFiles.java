package com.hfpvoipfix;
import java.io.*;
import java.util.*;
/** Clear only session-owned diagnostic files; preserve firmware and the archived previous ZIP. */
final class CaptureFiles {
    static void clearSession(File directory)throws IOException{
        File[] files=directory.listFiles();if(files==null)throw new IOException("Dossier de capture indisponible.");
        Set<String> names=new HashSet<>(Arrays.asList(ReportArchive.REQUIRED));
        names.addAll(Arrays.asList(ReportArchive.OPTIONAL));names.add("HFP-Rapport.zip");names.add("sessions.json");
        for(File f:files)if(f.isFile()&&(names.contains(f.getName())||f.getName().startsWith("events-")))
            if(!f.delete())throw new IOException("Ancienne donnée non supprimée : "+f.getName());
    }
}
