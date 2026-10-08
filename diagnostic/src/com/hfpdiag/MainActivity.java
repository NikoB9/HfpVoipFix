package com.hfpdiag;
import android.app.*;import android.content.*;import android.os.*;import android.widget.*;import java.io.*;
public final class MainActivity extends Activity {
 private TextView status;
 private void action(String op){Intent i=new Intent(this,DiagnosticService.class).setAction(op);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);status.setText("Requested: "+op);}
 @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(28,24,28,24);ScrollView s=new ScrollView(this);s.addView(l);
 TextView title=new TextView(this);title.setText("HFP Diagnostic — Redmi MIUI");title.setTextSize(23);l.addView(title);
 status=new TextView(this);status.setText("Ready. Captures Bluetooth HFP, Telecom, AudioFlinger, MediaTek and audio policy.");l.addView(status);
 Button start=new Button(this);start.setText("START CAPTURE before call");start.setOnClickListener(v->action("start"));l.addView(start);
 Button during=new Button(this);during.setText("SNAPSHOT DURING call");during.setOnClickListener(v->action("during"));l.addView(during);
 Button stop=new Button(this);stop.setText("STOP and build report");stop.setOnClickListener(v->action("stop"));l.addView(stop);
 Button share=new Button(this);share.setText("SHARE report ZIP");share.setOnClickListener(v->{File f=new File(getFilesDir(),"HFP-Diagnostic.zip");if(!f.isFile()){status.setText("No report yet");return;}Intent send=new Intent(Intent.ACTION_SEND);send.setType("application/zip");send.putExtra(Intent.EXTRA_STREAM,android.net.Uri.fromFile(f));send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivity(Intent.createChooser(send,"Share report"));}catch(Exception e){status.setText("Sharing failed: "+e.getMessage());}});l.addView(share);
 TextView note=new TextView(this);note.setText("Root Magisk required. No modifications to Bluetooth, LSPosed, audio HAL or pairing. Do not read /proc/asound/.../status during calls. Lower volume for noisy tests. Reports can include private information.");l.addView(note);
 setContentView(s);}
}
