package com.hfpvoipfix;

import java.io.*;
import java.util.*;

/** Streaming allowlist decoder. Never exports ACL, SCO payloads, addresses or vendor parameters. */
final class HciMetadata {
    static final long MAX_BYTES=128L*1024*1024;
    static final long EPOCH_DELTA=0x00dcddb30f2f8000L;
    static final int MAX_EVENTS=10000;
    private final StringBuilder events=new StringBuilder();
    private long packets,acl,scoRx,scoTx,bytes,first=-1,last=-1,dropped;
    private int selected,omitted,truncated,sync;
    private String warning="";
    static String decode(InputStream source)throws IOException {
        HciMetadata m=new HciMetadata();m.read(new DataInputStream(new BufferedInputStream(source)));return m.report();
    }
    private void read(DataInputStream in)throws IOException {
        byte[] magic=new byte[8];in.readFully(magic);
        if(!Arrays.equals(magic,new byte[]{'b','t','s','n','o','o','p',0})||in.readInt()!=1||in.readInt()!=1002)
            throw new IOException("Format btsnoop H4 version 1 requis ; aucun contenu brut exporté.");
        bytes=16;
        while(true){
            int lead=in.read();if(lead<0)break;
            if(bytes+24>MAX_BYTES){warning="LIMITE 128 Mio atteinte : fin non analysée";break;}
            try {
                long original=((long)lead<<24)|((long)in.readUnsignedByte()<<16)|((long)in.readUnsignedByte()<<8)|in.readUnsignedByte();
                long included=Integer.toUnsignedLong(in.readInt());int flags=in.readInt();
                dropped=Math.max(dropped,Integer.toUnsignedLong(in.readInt()));long ts=in.readLong();bytes+=24;
                if(included<1||included>65540||included>original){warning="Longueur de paquet invalide : analyse arrêtée";break;}
                if(bytes+included>MAX_BYTES){warning="LIMITE 128 Mio atteinte : fin non analysée";break;}
                byte[] packet=new byte[(int)included];in.readFully(packet);bytes+=included;packets++;
                long epoch=(ts-EPOCH_DELTA)/1000;if(first<0)first=epoch;last=epoch;
                if(included!=original){truncated++;continue;}
                inspect(packet,flags,epoch);
            }catch(EOFException e){warning="Dernier paquet incomplet (journal en écriture ou tronqué)";break;}
        }
    }
    private static int u(byte[] p,int i){return p[i]&255;}
    private static int le16(byte[] p,int i){return u(p,i)|(u(p,i+1)<<8);}
    private static long le32(byte[] p,int i){return Integer.toUnsignedLong(le16(p,i)|(le16(p,i+2)<<16));}
    private static String hex(int i){return String.format(Locale.ROOT,"0x%04x",i);}
    private void event(long at,String s){selected++;if(selected<=MAX_EVENTS)events.append("epoch=").append(at).append(' ').append(s).append('\n');else omitted++;}
    private static boolean relevant(int op){return op==0x0428||op==0x0429||op==0x042a||op==0x043d||op==0x043e||op==0x0c26||(op&0xfc00)==0xfc00;}
    private void inspect(byte[] p,int flags,long at){
        int type=u(p,0);
        if(type==2){acl++;return;}
        if(type==3){if(p.length<4||u(p,3)!=p.length-4){truncated++;return;}if((flags&1)!=0)scoRx++;else scoTx++;return;}
        if(type==1){
            if(p.length<4||u(p,3)!=p.length-4){truncated++;return;}
            int op=le16(p,1),n=p.length-4;if(!relevant(op))return;
            String s="CMD opcode="+hex(op)+" paramètres_octets="+n;
            if((op==0x0428&&n==17)||(op==0x0429&&n==21)){
                int q=op==0x0428?6:10;
                s+=" tx_bw="+le32(p,q)+" rx_bw="+le32(p,q+4)+" voice="+hex(le16(p,q+10))+" packet_types="+hex(le16(p,q+13));
            }else if((op==0x043d&&n==59)||(op==0x043e&&n==63)){
                int q=op==0x043d?6:10;
                s+=" tx_bw="+le32(p,q)+" rx_bw="+le32(p,q+4)+" tx_codec="+u(p,q+8)+" rx_codec="+u(p,q+13)
                    +" input_bw="+le32(p,q+22)+" output_bw="+le32(p,q+26)
                    +" input_codec="+u(p,q+30)+" output_codec="+u(p,q+35)
                    +" input_path="+u(p,q+48)+" output_path="+u(p,q+49);
            }else if(op==0x0c26&&n==2)s+=" voice="+hex(le16(p,4));
            event(at,s);return;
        }
        if(type!=4)return;
        if(p.length<3||u(p,2)!=p.length-3){truncated++;return;}
        int code=u(p,1),n=p.length-3;
        if(code==0x2c&&n==17){sync++;event(at,"SYNCHRONOUS_COMPLETE status="+u(p,3)+" handle="+hex(le16(p,4))+" link_type="+u(p,12)+" interval="+u(p,13)+" retransmission="+u(p,14)+" rx_length="+le16(p,15)+" tx_length="+le16(p,17)+" air_mode="+u(p,19));}
        else if(code==0x2d&&n==9)event(at,"SYNCHRONOUS_CHANGED status="+u(p,3)+" handle="+hex(le16(p,4))+" interval="+u(p,6)+" rx_length="+le16(p,8)+" tx_length="+le16(p,10));
        else if(code==0x05&&n==4)event(at,"DISCONNECTION status="+u(p,3)+" handle="+hex(le16(p,4))+" reason="+u(p,6));
        else if(code==0x0f&&n==4&&relevant(le16(p,5)))event(at,"COMMAND_STATUS opcode="+hex(le16(p,5))+" status="+u(p,3));
        else if(code==0x0e&&n>=4&&relevant(le16(p,4)))event(at,"COMMAND_COMPLETE opcode="+hex(le16(p,4))+" status_octet="+u(p,6));
    }
    private String report(){return "Métadonnées HCI uniquement. Adresses, ACL/RFCOMM, numéros, clés, son et paramètres vendor exclus.\n"
        +"paquets="+packets+" octets_lus="+bytes+" premier_epoch="+first+" dernier_epoch="+last+" drops_déclarés="+dropped+" tronqués="+truncated+"\n"
        +"ACL_ignorés="+acl+" SCO_HCI_RX="+scoRx+" SCO_HCI_TX="+scoTx+" événements_connexion_synchrone="+sync+" événements_omis="+omitted+"\n"
        +"AVERTISSEMENT="+(warning.isEmpty()?"aucun":warning)+"\n"
        +"L'absence de paquets SCO HCI ne prouve pas l'absence d'audio : le chemin PCM peut contourner HCI.\n"
        +"Un succès de connexion ne prouve pas le transport sonore. Le codec air transparent (3) ne prouve pas seul mSBC.\n"
        +"input_path/output_path : 0=HCI, 1=PCM (AOSP). À recouper avec le journal d'appel et la pile MIUI.\n"
        +events;
    }
}
