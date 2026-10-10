package com.hfpvoipfix;
/** Root-only command property: numeric dialing only; contents never logged. */
final class CallCommand {
    static String[] parse(String raw){String[] p=raw.split(":",-1);if(p.length!=3||!p[0].matches("[a-f0-9]{1,20}"))return null;
        if(p[1].equals("dial"))return p[2].matches("[+]?[0-9]{1,20}")?p:null;
        if((p[1].equals("answer")||p[1].equals("hangup"))&&p[2].equals("-"))return p;
        return null;
    }
}
