package com.hfpvoipfix;
import java.io.*;
import java.nio.charset.StandardCharsets;
/** Bounded metadata only, independent of logcat; never accepts PCM samples. */
final class BridgeJournal {
    static synchronized void write(File dir,String event){
        File file=new File(dir,"bridge-events.log");
        if(file.length()>2000000)return;
        try(FileOutputStream out=new FileOutputStream(file,true)){
            out.write((System.currentTimeMillis()+" "+Privacy.clean(event)+"\n").getBytes(StandardCharsets.UTF_8));
        }catch(IOException ignored){}
    }
}
