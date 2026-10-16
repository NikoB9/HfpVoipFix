#!/usr/bin/env python3
"""Run D6's real focus lifecycle against the Android focus API contract."""
import pathlib,subprocess,tempfile,os
root=pathlib.Path(__file__).resolve().parents[1]
files={
'android/media/AudioAttributes.java': 'package android.media;public class AudioAttributes {}',
'android/os/Looper.java': 'package android.os;public class Looper {public static Looper getMainLooper(){return new Looper();}}',
'android/os/Handler.java': 'package android.os;public class Handler {public Handler(Looper l){}}',
'android/media/AudioFocusRequest.java': '''package android.media;import android.os.Handler;public class AudioFocusRequest {
 public AudioManager.OnAudioFocusChangeListener listener;public AudioAttributes attributes;public int gain;public boolean delayed,pauses;
 public static class Builder {AudioFocusRequest r=new AudioFocusRequest();public Builder(int gain){r.gain=gain;}
 public Builder setAudioAttributes(AudioAttributes a){r.attributes=a;return this;}
 public Builder setAcceptsDelayedFocusGain(boolean b){r.delayed=b;return this;}
 public Builder setWillPauseWhenDucked(boolean b){r.pauses=b;return this;}
 public Builder setOnAudioFocusChangeListener(AudioManager.OnAudioFocusChangeListener l,Handler h){r.listener=l;return this;}
 public AudioFocusRequest build(){return r;}}
}''',
'android/media/AudioManager.java': '''package android.media;public class AudioManager {
 public static final int AUDIOFOCUS_GAIN=1,AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK=3,AUDIOFOCUS_REQUEST_GRANTED=1;
 public interface OnAudioFocusChangeListener{void onAudioFocusChange(int change);}
 public int result=1,requests,abandons;public boolean loseDuringRequest;public AudioFocusRequest active;
 public int requestAudioFocus(AudioFocusRequest r){requests++;active=r;if(loseDuringRequest)r.listener.onAudioFocusChange(-1);return result;}
 public int abandonAudioFocusRequest(AudioFocusRequest r){if(r!=active)throw new AssertionError();abandons++;return 1;}
}''',
'com/hfpvoipfix/ProjectionFocusTest.java': '''package com.hfpvoipfix;import android.media.*;import java.io.*;public class ProjectionFocusTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
 AudioManager m=new AudioManager();AudioAttributes a=new AudioAttributes();java.util.List<String> events=new java.util.ArrayList<>();
 ProjectionAudioFocus f=new ProjectionAudioFocus(m,a,events::add);check(!f.granted());f.acquire();check(f.granted());
 check(m.active.attributes==a&&m.active.gain==3&&!m.active.delayed&&m.active.pauses);
 for(int change:new int[]{-1,-2,-3,0}){m.active.listener.onAudioFocusChange(change);check(!f.granted());m.active.listener.onAudioFocusChange(1);check(f.granted());}
 f.close();f.close();check(m.abandons==1&&!f.granted());m.active.listener.onAudioFocusChange(1);check(!f.granted());
 for(int result:new int[]{0,2}){m=new AudioManager();m.result=result;f=new ProjectionAudioFocus(m,a,events::add);
 try{f.acquire();throw new AssertionError("ungranted focus accepted");}catch(IOException expected){}finally{f.close();}
 check(m.requests==1&&m.abandons==1&&!f.granted());}
 m=new AudioManager();m.loseDuringRequest=true;f=new ProjectionAudioFocus(m,a,events::add);f.acquire();check(!f.granted());f.close();
 try{f.acquire();throw new AssertionError("closed focus reacquired");}catch(IOException expected){}check(m.requests==1);
 check(events.stream().anyMatch(e->e.contains("forwarding=false")));
 System.out.println("PASS production D6 focus: matching attributes, denial/delay refusal, all losses, regain, cleanup and late callbacks");
 }}'''
}
with tempfile.TemporaryDirectory(dir=root/'tests') as tmp:
 d=pathlib.Path(tmp);out=d/'classes';out.mkdir()
 for name,content in files.items():
  p=d/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content)
 java=pathlib.Path(os.environ['JAVA_HOME'])/'bin'
 subprocess.run([str(java/'javac'),'-encoding','UTF-8','-d',str(out),*[str(p) for p in d.rglob('*.java')],str(root/'app/src/main/java/com/hfpvoipfix/ProjectionAudioFocus.java')],check=True)
 subprocess.run([str(java/'java'),'-cp',str(out),'com.hfpvoipfix.ProjectionFocusTest'],check=True)
