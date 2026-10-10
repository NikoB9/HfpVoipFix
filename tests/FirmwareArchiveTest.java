package com.hfpvoipfix;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
public final class FirmwareArchiveTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        for(String p:new String[]{"/system/lib64/libbluetooth.so","/apex/com.android.btservices@123/lib64/libbluetooth_jni.so","/vendor/lib/libbluetooth_mtk.so","/vendor/lib/hw/audio.primary.mt6785.so","/vendor/lib64/libaudio.primary.default.so","/vendor/lib/libaudiocompensationfilter.so","/vendor/etc/audio_device.xml","/vendor/lib/hw/android.hardware.audio@6.0-impl.so"})check(FirmwareArchive.allowed(p),p);
        for(String p:new String[]{"/system/lib64/libarbitrary.so","/data/misc/bluetooth/logs/btsnoop_hci.log","/proc/asound/card0/pcm0p/sub0/status","/data/misc/bluedroid/bt_config.conf","/vendor/etc/../../data/secret","/vendor/lib/libaudio.so;id","/dev/snd/pcm0","/vendor/nvdata/audio.xml"})check(!FirmwareArchive.allowed(p),p);
        File dir=Files.createTempDirectory("firmware-test").toFile();
        FirmwareArchive.Source good=new FirmwareArchive.Source(){
            public String paths(){return "/vendor/lib/hw/audio.primary.mt6785.so\n/vendor/etc/audio_device.xml\n/vendor/lib/libaudio_bad.so\n/data/secret\n";}
            public byte[] read(String p,long limit){return p.endsWith("xml")?"<audio/>".getBytes():p.contains("bad")?new byte[]{1,2,3}:new byte[]{127,'E','L','F',0,1,2};}
            public String identity(){return "test identity";}
        };
        FirmwareArchive.build(dir,good,s->{});File archive=new File(dir,"HFP-Firmware.zip");byte[] before=Files.readAllBytes(archive.toPath());
        try(ZipFile zip=new ZipFile(archive)){
            check(zip.getEntry("firmware/vendor/lib/hw/audio.primary.mt6785.so")!=null,"primary copied");
            check(zip.getEntry("hci-transport.txt")!=null,"transport metadata present");
            check(zip.getEntry("firmware/data/secret")==null,"private excluded");
            check(zip.getEntry("firmware/vendor/lib/libaudio_bad.so")==null,"bad ELF excluded");
            String errors=new String(zip.getInputStream(zip.getEntry("errors.txt")).readAllBytes());check(errors.contains("ELF")&&errors.contains("/data/secret"),"omissions explicit");
            String inv=new String(zip.getInputStream(zip.getEntry("inventory.tsv")).readAllBytes());check(inv.matches("(?s).*\\t[0-9a-f]{64}\\n.*"),"hashes present");
        }
        try{FirmwareArchive.build(dir,new FirmwareArchive.Source(){public String paths(){return "/vendor/etc/audio_device.xml";}public byte[] read(String p,long l){return new byte[]{1};}public String identity(){return "x";}},s->{});throw new AssertionError("missing primary accepted");}catch(IOException expected){}
        check(java.util.Arrays.equals(before,Files.readAllBytes(archive.toPath())),"old archive preserved on failure");
        check(!new File(dir,"HFP-Firmware.zip.tmp").exists(),"temp removed");
        System.out.println("Firmware archive: allowlist, privacy boundaries, ELF, hashes, ZIP, failure preservation OK");
    }
}
