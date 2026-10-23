package com.hfpvoipfix;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** API 30: publish a completed copy in Downloads without broad storage access. */
final class DownloadExport {
 static String save(ContentResolver resolver,File source,String prefix)throws IOException{
  if(!"HFP-Rapport".equals(prefix)&&!"HFP-Firmware".equals(prefix))throw new IOException("Type de fichier non pris en charge.");
  if(!source.isFile()||source.length()==0)throw new IOException("Le ZIP n’est pas encore prêt.");
  String name=prefix+"-"+new SimpleDateFormat("yyyyMMdd-HHmmss-SSS",Locale.ROOT).format(new Date())+".zip";
  ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.DISPLAY_NAME,name);
  values.put(MediaStore.MediaColumns.MIME_TYPE,"application/zip");
  values.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/");
  values.put(MediaStore.MediaColumns.IS_PENDING,1);
  Uri uri=null;boolean published=false;
  try{
   uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
   if(uri==null)throw new IOException("Création dans Téléchargements refusée.");
   try(InputStream in=new FileInputStream(source);OutputStream out=resolver.openOutputStream(uri,"w")){
    if(out==null)throw new IOException("Écriture dans Téléchargements indisponible.");
    byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
   }
   values.clear();values.put(MediaStore.MediaColumns.IS_PENDING,0);
   if(resolver.update(uri,values,null,null)!=1)throw new IOException("Publication du fichier non confirmée.");
   published=true;
   // The provider can rename a colliding filename: show its authoritative name.
   try(Cursor c=resolver.query(uri,new String[]{MediaStore.MediaColumns.DISPLAY_NAME},null,null,null)){
    if(c!=null&&c.moveToFirst()){String actual=c.getString(0);if(actual!=null&&!actual.isEmpty())name=actual;}
   }catch(RuntimeException ignored){}
   return name;
  }finally{
   if(uri!=null&&!published)try{resolver.delete(uri,null,null);}catch(RuntimeException ignored){}
  }
 }
}
