package com.hfpvoipfix;
import android.media.*;
import java.io.IOException;

/** D6 only: separates the master's registered SCO ports from the vehicle's ports. */
final class CarEndpoints {
    static final int REMOTE_SUBMIX=25;
    static AudioDeviceInfo master(AudioManager am,int flags,int id)throws IOException{
        if(id<0)throw new IOException("D6 : identifiant du port maître indisponible.");
        AudioDeviceInfo result=null;
        for(AudioDeviceInfo d:am.getDevices(flags))if(d.getId()==id&&d.getType()==AudioDeviceInfo.TYPE_BLUETOOTH_SCO){
            if(result!=null)throw new IOException("D6 : port maître ambigu.");result=d;
        }
        if(result==null)throw new IOException("D6 : port maître absent, id="+id);return result;
    }
    static AudioDeviceInfo[] vehicle(AudioManager am,AudioDeviceInfo masterIn,AudioDeviceInfo masterOut)throws IOException{
        AudioDeviceInfo in=null,out=null;
        for(AudioDeviceInfo d:am.getDevices(AudioManager.GET_DEVICES_OUTPUTS))if(d.getType()==AudioDeviceInfo.TYPE_BLUETOOTH_SCO&&d.getId()!=masterOut.getId()){
            if(out!=null)throw new IOException("D6 : plusieurs sorties véhicule candidates.");out=d;
        }
        if(out==null)return null;
        String a=out.getAddress();
        if(a==null||a.isEmpty()||a.equalsIgnoreCase(masterOut.getAddress()))throw new IOException("D6 : identité véhicule non distincte du maître.");
        for(AudioDeviceInfo d:am.getDevices(AudioManager.GET_DEVICES_INPUTS))if(d.getType()==AudioDeviceInfo.TYPE_BLUETOOTH_SCO&&d.getId()!=masterIn.getId()&&a.equalsIgnoreCase(d.getAddress())){
            if(in!=null)throw new IOException("D6 : plusieurs micros véhicule candidats.");in=d;
        }
        if(in==null)throw new IOException("D6 : paire audio véhicule incomplète.");return new AudioDeviceInfo[]{in,out};
    }
    static boolean projection(AudioManager am){
        for(AudioDeviceInfo d:am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)){
            String a=d.getAddress();
            if(d.getType()==REMOTE_SUBMIX&&a!=null&&a.matches("[0-9]+:ap:[0-9]+mixp:[0-9]+"))return true;
        }
        return false;
    }
}
