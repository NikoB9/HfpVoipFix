package com.hfpvoipfix;
import java.util.Map;
final class RelayPolicy {
 static boolean prepare(String p,boolean enabled,boolean alive,boolean permission){Map<String,String> v=RxGate.parse(p);return enabled&&!alive&&permission&&"1.9.14".equals(v.get("version"))&&"false".equals(v.get("busy"))&&"false".equals(v.get("dirty"))&&"true".equals(v.get("route"))&&"true".equals(v.get("calls"))&&"true".equals(v.get("disconnect"))&&"3".equals(v.get("voip"));}
}
