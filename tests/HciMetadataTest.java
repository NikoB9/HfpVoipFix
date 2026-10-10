package com.hfpvoipfix;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class HciMetadataTest {
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    static ByteArrayOutputStream header()throws Exception{
        ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);
        d.write(new byte[]{'b','t','s','n','o','o','p',0});d.writeInt(1);d.writeInt(1002);return b;
    }
    static void packet(ByteArrayOutputStream b,byte[] p,int flags)throws Exception{
        DataOutputStream d=new DataOutputStream(b);d.writeInt(p.length);d.writeInt(p.length);d.writeInt(flags);d.writeInt(0);d.writeLong(HciMetadata.EPOCH_DELTA+1700000000000000L);d.write(p);
    }
    static String parse(ByteArrayOutputStream b)throws Exception{return HciMetadata.decode(new ByteArrayInputStream(b.toByteArray()));}
    static void le16(byte[] p,int i,int n){p[i]=(byte)n;p[i+1]=(byte)(n>>8);}
    static void le32(byte[] p,int i,int n){le16(p,i,n);le16(p,i+2,n>>16);}
    static byte[] cmd(int op,int size){byte[] p=new byte[size+4];p[0]=1;le16(p,1,op);p[3]=(byte)size;return p;}
    public static void main(String[] args)throws Exception{
        ByteArrayOutputStream b=header();
        for(int op:new int[]{0x043d,0x043e}){
            byte[] p=cmd(op,op==0x043d?59:63);int q=op==0x043d?6:10;
            le32(p,q,8000);le32(p,q+4,8000);p[q+8]=5;p[q+13]=5;
            le32(p,q+22,32000);le32(p,q+26,32000);p[q+30]=4;p[q+35]=4;p[q+48]=1;p[q+49]=1;packet(b,p,2);
        }
        for(int op:new int[]{0x0428,0x0429}){
            byte[] p=cmd(op,op==0x0428?17:21);int q=op==0x0428?6:10;
            le32(p,q,8000);le32(p,q+4,8000);le16(p,q+10,0x63);le16(p,q+13,0x380);packet(b,p,2);
        }
        byte[] sync=new byte[20];sync[0]=4;sync[1]=0x2c;sync[2]=17;le16(sync,4,0x123);Arrays.fill(sync,6,12,(byte)0xee);sync[12]=2;sync[13]=6;le16(sync,15,60);le16(sync,17,60);sync[19]=3;packet(b,sync,3);
        packet(b,new byte[]{4,0x0f,4,0,1,0x3e,4},3);
        packet(b,new byte[]{4,0x0e,4,1,0x72,(byte)0xfc,0},3);
        byte[] privateBytes="PRIVATE_NUMBER_CONTACT_AUDIO_SECRET".getBytes(StandardCharsets.UTF_8);
        byte[] acl=new byte[privateBytes.length+5];acl[0]=2;System.arraycopy(privateBytes,0,acl,5,privateBytes.length);packet(b,acl,1);
        byte[] sco=new byte[privateBytes.length+4];sco[0]=3;sco[3]=(byte)privateBytes.length;System.arraycopy(privateBytes,0,sco,4,privateBytes.length);packet(b,sco,1);packet(b,sco,0);
        byte[] vendor=cmd(0xfc72,privateBytes.length);System.arraycopy(privateBytes,0,vendor,4,privateBytes.length);packet(b,vendor,2);
        String s=parse(b);
        check(s.contains("tx_codec=5 rx_codec=5 input_bw=32000 output_bw=32000 input_codec=4 output_codec=4 input_path=1 output_path=1"),"enhanced layout");
        check(s.contains("voice=0x0063 packet_types=0x0380"),"legacy layout");
        check(s.contains("SYNCHRONOUS_COMPLETE status=0 handle=0x0123 link_type=2 interval=6 retransmission=0 rx_length=60 tx_length=60 air_mode=3"),"event layout");
        check(s.contains("COMMAND_STATUS opcode=0x043e status=0")&&s.contains("COMMAND_COMPLETE opcode=0xfc72 status_octet=0"),"command responses");
        check(s.contains("epoch=1700000000000")&&s.contains("SCO_HCI_RX=1 SCO_HCI_TX=1"),"timestamps and counters");
        check(!s.contains("PRIVATE")&&!s.contains("eeee"),"no private contents");
        byte[] shortLog=Arrays.copyOf(b.toByteArray(),b.size()-2);
        check(HciMetadata.decode(new ByteArrayInputStream(shortLog)).contains("Dernier paquet incomplet"),"partial tail explicit");
        ByteArrayOutputStream malformed=header();DataOutputStream d=new DataOutputStream(malformed);d.writeInt(1000000);d.writeInt(1000000);d.writeInt(0);d.writeInt(0);d.writeLong(0);
        check(parse(malformed).contains("Longueur de paquet invalide"),"allocation bound");
        ByteArrayOutputStream many=header();for(int i=0;i<10002;i++)packet(many,sync,3);
        check(parse(many).contains("événements_omis=2"),"output bound explicit");
        byte[] bad=header().toByteArray();bad[15]=0;
        try{HciMetadata.decode(new ByteArrayInputStream(bad));throw new AssertionError("invalid format accepted");}catch(IOException expected){}
        System.out.println("HCI metadata: enhanced/legacy layouts, events, privacy, counters, bounds, malformed/truncated input OK");
    }
}
