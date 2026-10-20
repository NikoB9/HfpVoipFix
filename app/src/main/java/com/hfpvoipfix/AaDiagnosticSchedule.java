package com.hfpvoipfix;
/** Optional one-call experiment. Adjacent phases change one audio endpoint only. */
final class AaDiagnosticSchedule {
    static final int COUNT=8;
    static final long DURATION_MS=8000;
    static int next(int phase,long elapsed){
        if(phase==0)return 1;
        if(phase>=COUNT+1)return COUNT+1;
        return elapsed>=DURATION_MS?phase+1:phase;
    }
    static boolean microphone(int phase){return phase==2||phase==3||phase==4||phase==7||phase==8;}
    static boolean transmit(int phase){return phase==3||phase==8;}
    static int usage(int phase){return phase>=6?1:12;}
    static String label(int phase){
        switch(phase){
            case 1:return "Navigation · réception seule";
            case 2:return "Navigation · ajout micro";
            case 3:return "Navigation · ajout TX";
            case 4:return "Navigation · retrait TX";
            case 5:return "Navigation · retrait micro";
            case 6:return "Média · réception seule";
            case 7:return "Média · ajout micro";
            case 8:return "Média · ajout TX";
            default:return "Terminé · raccrocher et exporter";
        }
    }
    static boolean canStart(boolean enabled,boolean comparing,String previous,String call){
        return enabled&&!comparing&&call!=null&&call.matches("[0-9]+:[1-9][0-9]*")&&!call.equals(previous);
    }
}
