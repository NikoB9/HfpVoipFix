package com.hfpvoipfix;
import java.util.*;
/** A fresh Bluetooth handshake is mandatory; no cached status may open audio. */
final class RxGate {
    static Map<String,String> parse(String line){Map<String,String> out=new HashMap<>();for(String part:line.split("\\s+")){int i=part.indexOf('=');if(i>0)out.put(part.substring(0,i),part.substring(i+1));}return out;}
    static String armError(String line,String mode,String property){
        Map<String,String> p=parse(line);
        if(!"1.7.12".equals(p.get("version")))return "Le module Bluetooth 1.7.12 n’est pas chargé. Installe cette version puis redémarre le téléphone.";
        if(!LabModes.softwareBridge(mode))return "Choisir D2, D3 ou D4 avant l’armement.";
        if(!"false".equals(p.get("busy")))return "Un appel ou un SCO est déjà actif. Raccrocher et attendre la fin complète.";
        if(!mode.equals(property)||!mode.equals(p.get("requested")))return "Le mode "+mode+" n’est pas appliqué au module Bluetooth. Appuyer sur « Appliquer au prochain appel », puis réessayer.";
        if(!"true".equals(p.get("route"))||!"true".equals(p.get("calls"))||!"true".equals(p.get("disconnect"))||!"true".equals(p.get("bypass")))return "Les hooks Bluetooth requis pour le pont ne sont pas tous chargés. Vérifier LSPosed et redémarrer.";
        return null;
    }
    static boolean ready(String line){Map<String,String> p=parse(line);String requested=p.get("requested");return "1.7.12".equals(p.get("version"))&&LabModes.softwareBridge(requested)&&requested.equals(p.get("applied"))&&"1".equals(p.get("sco"))&&"true".equals(p.get("bypass_ok"))&&"true".equals(p.get("soft_ports"))&&"true".equals(p.get("disconnect"))&&"true".equals(p.get("route"))&&"true".equals(p.get("calls"))&&"false".equals(p.get("dirty"))&&("8000".equals(p.get("rate"))||"16000".equals(p.get("rate")));}
    static String identity(String line){Map<String,String> p=parse(line);return p.get("epoch")+":"+p.get("sco_seq");}
    static boolean fresh(long now,long received){return received>0&&now>=received&&now-received<4000;}
}
