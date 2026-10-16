package com.hfpvoipfix;

import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import java.io.IOException;

/** One focus owner per D6 bridge. No retries or permanent focus takeover. */
final class ProjectionAudioFocus implements AutoCloseable {
    interface Events { void write(String message); }
    private final AudioManager manager;
    private final Events events;
    private final AudioFocusRequest request;
    private volatile boolean granted,closed;
    private final Object lock=new Object();
    private long callbacks;

    ProjectionAudioFocus(AudioManager manager,AudioAttributes attributes,Events events){
        this.manager=manager;this.events=events;
        request=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes).setAcceptsDelayedFocusGain(false)
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(change->{
                synchronized(lock){if(closed)return;callbacks++;granted=change==AudioManager.AUDIOFOCUS_GAIN;}
                events.write("D6_FOCUS change="+change+" forwarding="+granted);
            },new Handler(Looper.getMainLooper())).build();
    }
    void acquire()throws IOException{
        long before;synchronized(lock){if(closed)throw new IOException("Priorité audio déjà libérée.");before=callbacks;}
        int result=manager.requestAudioFocus(request);
        synchronized(lock){
            if(result!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED)granted=false;
            else if(callbacks==before&&!closed)granted=true;
        }
        events.write("D6_FOCUS gain=MAY_DUCK request="+result+" forwarding="+granted());
        if(result!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED)throw new IOException("D6 : priorité audio de projection refusée. Exporter le rapport.");
    }
    boolean granted(){return granted&&!closed;}
    @Override public void close(){
        synchronized(lock){if(closed)return;closed=true;granted=false;}
        int result=manager.abandonAudioFocusRequest(request);
        events.write("D6_FOCUS abandon="+result);
    }
}
