package com.hfpvoipfix;
import android.content.Context;
final class LabProbe {
    static synchronized String read(Context c)throws Exception{
        try(ProbeChannel channel=new ProbeChannel()){
            String line=channel.read();c.getSharedPreferences("capture",0).edit().putString("root","accordé").putString("probe",line).putLong("probe_at",System.currentTimeMillis()).apply();return line;
        }catch(Exception e){c.getSharedPreferences("capture",0).edit().putString("error","Sonde : "+e.getMessage()).apply();throw e;}
    }
}
