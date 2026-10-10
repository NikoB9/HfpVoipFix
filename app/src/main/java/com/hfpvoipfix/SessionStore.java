package com.hfpvoipfix;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

final class SessionStore {
    private static JSONObject data;
    static synchronized void begin(Context c)throws Exception{
        data=new JSONObject().put("schema",1).put("started_at_ms",System.currentTimeMillis())
            .put("calls",new JSONArray()).put("notes",new JSONArray());save(c);
    }
    static synchronized JSONObject load(Context c)throws Exception{
        if(data==null){File f=new File(c.getFilesDir(),"sessions.json");
            data=f.isFile()?new JSONObject(new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8)):
                new JSONObject().put("calls",new JSONArray()).put("notes",new JSONArray());}
        return data;
    }
    static synchronized void call(Context c,String key,int state,String mode,long time)throws Exception{
        JSONArray a=load(c).getJSONArray("calls");JSONObject item=null;
        for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getString("key").equals(key)){item=a.getJSONObject(i);break;}
        if(item==null){item=new JSONObject().put("key",key).put("mode",mode).put("start_ms",time).put("states",new JSONArray());a.put(item);}
        item.getJSONArray("states").put(new JSONObject().put("state",state).put("at_ms",time));
        if(state==7)item.put("end_ms",time);
        save(c);
    }
    static synchronized void note(Context c,String text)throws Exception{
        load(c).getJSONArray("notes").put(new JSONObject().put("at_ms",System.currentTimeMillis()).put("text",text));save(c);
    }
    static synchronized String[] ended(Context c)throws Exception{
        JSONArray a=load(c).getJSONArray("calls");java.util.ArrayList<String> keys=new java.util.ArrayList<>();
        for(int i=0;i<a.length();i++){JSONObject item=a.getJSONObject(i);if(item.has("end_ms"))keys.add(item.getString("key")+" · "+item.getString("mode"));}
        return keys.toArray(new String[0]);
    }
    static synchronized void result(Context c,String key,String rx,String tx,String other)throws Exception{
        JSONArray a=load(c).getJSONArray("calls");
        for(int i=0;i<a.length();i++){JSONObject item=a.getJSONObject(i);if(item.getString("key").equals(key)){
            item.put("reception",rx).put("microphone",tx).put("commentaire",Privacy.clean(other)).put("rated_at_ms",System.currentTimeMillis());save(c);return;
        }}throw new IOException("Appel introuvable");
    }
    static synchronized void finish(Context c)throws Exception{load(c).put("ended_at_ms",System.currentTimeMillis());save(c);}
    private static void save(Context c)throws Exception{
        File tmp=new File(c.getFilesDir(),"sessions.tmp");
        try(FileOutputStream out=new FileOutputStream(tmp)){out.write(data.toString(2).getBytes(StandardCharsets.UTF_8));out.getFD().sync();}
        Files.move(tmp.toPath(),new File(c.getFilesDir(),"sessions.json").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
