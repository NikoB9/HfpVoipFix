package com.hfpvoipfix;
import java.nio.file.*;
public class BridgeJournalTest {
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("bridge-journal-test"),log=dir.resolve("bridge-events.log");
        try{
            BridgeJournal.write(dir.toFile(),"PHASE_BEGIN phase=2 number=+33123456789");
            String text=Files.readString(log);
            if(!text.contains("PHASE_BEGIN phase=2")||text.contains("+33123456789"))throw new AssertionError("journal/redaction");
            Files.write(log,new byte[2000001]);long length=Files.size(log);
            BridgeJournal.write(dir.toFile(),"PHASE_STATS");if(Files.size(log)!=length)throw new AssertionError("unbounded journal");
            System.out.println("PASS metadata journal: phase persistence, redaction and size cap");
        }finally{Files.deleteIfExists(log);Files.delete(dir);}
    }
}
