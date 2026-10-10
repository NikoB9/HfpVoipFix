package com.hfpvoipfix;
import java.util.regex.*;

/** Redact payloads, never the logcat timestamp/PID/TID prefix or unlabelled audio numbers. */
final class Privacy {
    private static final Pattern HEADER=Pattern.compile("^(\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d+\\s+\\d+\\s+\\d+\\s+[VDIWEFAS]\\s+)(.*)$");
    static String clean(String text){
        String[] lines=text.split("\\n",-1);StringBuilder out=new StringBuilder();
        for(int i=0;i<lines.length;i++){
            Matcher header=HEADER.matcher(lines[i]);
            if(header.matches())out.append(header.group(1)).append(payload(header.group(2)));
            else out.append(payload(lines[i]));
            if(i+1<lines.length)out.append('\n');
        }
        return out.toString();
    }
    private static String payload(String s){
        return s.replaceAll("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}","[MAC]")
            .replaceAll("(?i)(tel:|sip:)[^\\s,;}]+","$1[masqué]")
            .replaceAll("(?i)(\\b(?:mNumber|number|phoneNumber|displayName|callerName|deviceName|subscriberId|imei|imsi|iccid)\\s*[=:]\\s*)[^,;\\r\\n}]+","$1[masqué]")
            .replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}","[courriel]")
            .replaceAll("(?<![\\w])\\+[1-9][0-9]{7,14}(?![0-9])","[numéro]");
    }
}
