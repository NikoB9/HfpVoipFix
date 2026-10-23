#!/usr/bin/env python3
"""Exercise the production Downloads transaction and failure cleanup with a fake resolver."""
import os,pathlib,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parents[1]
files={
'android/net/Uri.java':'package android.net;public class Uri {public final String text;public Uri(String s){text=s;}}',
'android/content/ContentValues.java':'package android.content;public class ContentValues extends java.util.HashMap<String,Object> {}',
'android/os/Environment.java':'package android.os;public class Environment {public static final String DIRECTORY_DOWNLOADS="Download";}',
'android/provider/MediaStore.java':'package android.provider;import android.net.Uri;public class MediaStore {public static class MediaColumns {public static final String DISPLAY_NAME="name",MIME_TYPE="mime",RELATIVE_PATH="relative",IS_PENDING="pending";}public static class Downloads {public static final Uri EXTERNAL_CONTENT_URI=new Uri("downloads");}}',
'android/database/Cursor.java':'package android.database;public interface Cursor extends java.io.Closeable {boolean moveToFirst();String getString(int n);void close();}',
'android/content/ContentResolver.java':'''package android.content;
import java.io.*;import android.net.Uri;import android.database.Cursor;
public class ContentResolver {
public String fail="";public ContentValues insert;public int inserts,deletes,updates;public boolean closed,published;public ByteArrayOutputStream data=new ByteArrayOutputStream();
public Uri insert(Uri collection,ContentValues values){inserts++;if(!"downloads".equals(collection.text))throw new AssertionError();insert=new ContentValues();insert.putAll(values);if(fail.equals("insert"))return null;return new Uri("created");}
public OutputStream openOutputStream(Uri u,String mode){if(!mode.equals("w"))throw new AssertionError();if(fail.equals("null"))return null;return new OutputStream(){public void write(int x)throws IOException{if(fail.equals("write"))throw new IOException("full");data.write(x);}public void close()throws IOException{closed=true;if(fail.equals("close"))throw new IOException("close failed");}};}
public int update(Uri uri,ContentValues v,String s,String[] a){updates++;if(!closed||!Integer.valueOf(0).equals(v.get("pending")))throw new AssertionError("publish before close");if(fail.equals("publish"))return 0;published=true;return 1;}
public Cursor query(Uri u,String[] cols,String s,String[] a,String order){if(fail.equals("query"))throw new SecurityException();return new Cursor(){public boolean moveToFirst(){return true;}public String getString(int n){return "HFP-Rapport-renamed.zip";}public void close(){}};}
public int delete(Uri uri,String s,String[] a){deletes++;if(fail.equals("delete"))throw new SecurityException();return 1;}
}''',
'com/hfpvoipfix/DownloadExportTest.java':'''package com.hfpvoipfix;
import java.io.*;import java.nio.file.*;import java.util.*;import android.content.*;
public class DownloadExportTest {
static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
public static void main(String[] args)throws Exception {
Path p=Files.createTempFile("ready-report", ".zip");byte[] content=new byte[100000];new Random(42).nextBytes(content);Files.write(p,content);
try {
ContentResolver r=new ContentResolver();String n=DownloadExport.save(r,p.toFile(),"HFP-Rapport");check(n.equals("HFP-Rapport-renamed.zip")&&Arrays.equals(content,r.data.toByteArray())&&r.published&&r.deletes==0,"complete exact copy, authoritative renamed file");check(r.insert.get("relative").equals("Download/")&&r.insert.get("mime").equals("application/zip")&&r.insert.get("pending").equals(1),"Downloads ZIP initially hidden");check(r.insert.get("name").toString().matches("HFP-Rapport-[0-9]{8}-[0-9]{6}-[0-9]{3}\\\\.zip"),"dated safe name");
for(String failure:new String[]{"insert","null","write","close","publish"}){r=new ContentResolver();r.fail=failure;try{DownloadExport.save(r,p.toFile(),"HFP-Rapport");throw new AssertionError("false success: "+failure);}catch(IOException expected){}check(r.deletes==(failure.equals("insert")?0:1),"partial row removed: "+failure);check(Arrays.equals(content,Files.readAllBytes(p)),"private ZIP retained on failure");}
r=new ContentResolver();r.fail="query";n=DownloadExport.save(r,p.toFile(),"HFP-Firmware");check(n.startsWith("HFP-Firmware-")&&r.published&&r.deletes==0,"optional metadata failure cannot undo successful save");
for(String prefix:new String[]{"../HFP","unknown"}){r=new ContentResolver();try{DownloadExport.save(r,p.toFile(),prefix);throw new AssertionError();}catch(IOException expected){}check(r.inserts==0,"prefix excluded");}
Files.write(p,new byte[0]);r=new ContentResolver();try{DownloadExport.save(r,p.toFile(),"HFP-Rapport");throw new AssertionError();}catch(IOException expected){}check(r.inserts==0,"empty report not published");
System.out.println("PASS production Downloads export: exact bytes, pending/close/publish order, provider rename, null/write/close/publish failures remove partial rows, original report retained, no storage permission or sharing intent.");
}finally{Files.delete(p);}
}}
'''
}
with tempfile.TemporaryDirectory(dir=root/'tests') as tmp:
 d=pathlib.Path(tmp);out=d/'classes';out.mkdir()
 for n,s in files.items():
  p=d/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s)
 java=pathlib.Path(os.environ['JAVA_HOME'])/'bin'
 src=[str(p) for p in d.rglob('*.java')]+[str(root/'app/src/main/java/com/hfpvoipfix/DownloadExport.java')]
 subprocess.run([str(java/'javac'),'-encoding','UTF-8','-d',str(out),*src],check=True)
 subprocess.run([str(java/'java'),'-cp',str(out),'com.hfpvoipfix.DownloadExportTest'],check=True)
