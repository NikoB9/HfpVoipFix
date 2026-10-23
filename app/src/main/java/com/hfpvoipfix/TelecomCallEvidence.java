package com.hfpvoipfix;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Predicate;

/** Match real HFP connection UUIDs across an older SCO's asynchronous cleanup. */
final class TelecomCallEvidence {
 private static final class Entry {final WeakReference<Object> connection;final long at;boolean matched;Entry(Object c,long now){connection=new WeakReference<>(c);at=now;}}
 private final LinkedHashMap<Object,Entry> entries=new LinkedHashMap<>();
 void add(Object key,Object connection,long now){
  if(key==null||connection==null)return;
  if(!entries.containsKey(key)&&entries.size()>=8)entries.remove(entries.keySet().iterator().next());
  entries.put(key,new Entry(connection,now));
 }
 void remove(Object key){entries.remove(key);}
 void clear(){entries.clear();}
 boolean confirmed(Set<?> calls,long now,Predicate<Object> connected){
  boolean matched=false;
  Iterator<Map.Entry<Object,Entry>> it=entries.entrySet().iterator();
  while(it.hasNext()){
   Map.Entry<Object,Entry> e=it.next();Object connection=e.getValue().connection.get();
   boolean active=calls.contains(e.getKey());
   if(connection==null||now<e.getValue().at||((!active||!e.getValue().matched)&&now-e.getValue().at>=30000)){it.remove();continue;}
   if(!connected.test(connection)){it.remove();continue;}
   if(active){e.getValue().matched=true;matched=true;}
  }
  return matched;
 }
}
