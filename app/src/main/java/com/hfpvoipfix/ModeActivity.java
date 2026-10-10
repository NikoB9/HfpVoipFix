package com.hfpvoipfix;
import android.app.*;
import android.os.*;
import android.content.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.*;

public final class ModeActivity extends Activity {
    private final ExecutorService work=Executors.newSingleThreadExecutor();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private TextView status,details,notice;
    private Button apply,start,stop,snap,share,rate,inspect,firmware,firmwareShare,firmwareInspect;
    private TextView firmwareStatus,rxStatus,transportStatus;
    private Button rxArm,rxStop,probeButton;
    private Spinner modes,advancedModes;
    private EditText phoneNumber;
    private CheckBox risk;
    private boolean visible,working;
    private String requested="non vérifié";
    private static final String[] PRIMARY_IDS={"observe","soft_rx","soft_tx"};
    private static final String[] PRIMARY_NAMES={"0 · Téléphone Android","D2 · Réception validée","D3 · Essai micro"};
    private static final String[] ADVANCED_IDS={"","voip","wbs","endpoints","bypass","bypass_comm","codec","rx","tx","bridge"};
    private static final String[] RESULTS={"Non évalué","Silence","Bip continu","Retour microphone","Voix reçue mais déformée","Voix claire","Son uniquement au début ou à la fin","Autre"};
    private android.content.SharedPreferences prefs(){return getSharedPreferences("capture",0);}
    private void action(String s){startForegroundService(new Intent(this,CaptureService.class).setAction(s));}
    private void text(LinearLayout l,String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setPadding(0,12,0,12);l.addView(t);}
    private Button button(LinearLayout l,String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->r.run());l.addView(b);return b;}
    private void toast(String s){notice.setText(s);}
    private String selectedMode(){if(advancedModes!=null&&advancedModes.getSelectedItemPosition()>0)return ADVANCED_IDS[advancedModes.getSelectedItemPosition()];return PRIMARY_IDS[modes.getSelectedItemPosition()];}
    private void showMode(){if(details==null||modes==null)return;String id=selectedMode();details.setText(LabModes.DETAILS[LabModes.index(id)]+"\nMIUI 12.5 : résultat d’exécution à vérifier dans le rapport.");refresh();}
    private void task(CheckedTask r){if(working)return;working=true;refresh();work.execute(()->{
        try{r.run();}catch(Exception e){ui.post(()->toast("Erreur : "+e.getMessage()));}
        finally{ui.post(()->{working=false;refresh();});}
    });}
    private interface CheckedTask{void run()throws Exception;}
    @Override public void onCreate(Bundle b){super.onCreate(b);
        ScrollView scroll=new ScrollView(this);LinearLayout l=new LinearLayout(this);l.setOrientation(1);int pad=(int)(18*getResources().getDisplayMetrics().density);l.setPadding(pad,pad,pad,pad);scroll.addView(l);LinearLayout root=l;
        text(l,"HfpVoipLab 1.7.10",27);text(l,"D2 réception validée · D3 essai micro · un rapport",16);
        status=new TextView(this);status.setTextSize(15);l.addView(status);
        notice=new TextView(this);notice.setTextSize(15);l.addView(notice);
        probeButton=button(l,"Vérifier root et module Bluetooth",()->task(()->{String p=LabProbe.read(this);requested=Root.command("getprop "+LabModes.PROP,1000,4).trim();ui.post(()->toast("Réponse récente reçue du processus Bluetooth."));}));
        text(l,"Appels du Samsung, pilotés depuis le Redmi",21);
        phoneNumber=new EditText(this);phoneNumber.setHint("Numéro à appeler sur le Samsung");phoneNumber.setInputType(android.text.InputType.TYPE_CLASS_PHONE);phoneNumber.setText("666");l.addView(phoneNumber);
        button(l,"Appeler depuis le Redmi",()->controlCall("dial"));
        button(l,"Répondre à l’appel du Samsung",()->controlCall("answer"));
        button(l,"Raccrocher l’appel du Samsung",()->controlCall("hangup"));
        text(l,"Les boutons pilotent le Samsung via HFP. Le mode 0 laisse l’application Téléphone Android gérer l’appel ; D2/D3 utilisent le pont de test.",14);
        text(l,"Choisir un parcours",21);
        modes=new Spinner(this);modes.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,PRIMARY_NAMES));modes.setSelection(1);l.addView(modes);
        details=new TextView(this);details.setTextSize(16);l.addView(details);
        modes.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int i,long id){if(advancedModes!=null&&advancedModes.getSelectedItemPosition()!=0)advancedModes.setSelection(0);showMode();}});
        risk=new CheckBox(this);risk.setText("Autoriser les stratégies expérimentales pour cette ouverture");risk.setChecked(false);risk.setOnCheckedChangeListener((b1,x)->refresh());l.addView(risk);
        apply=button(l,"Appliquer au prochain appel",this::applyMode);
        text(l,"Parcours : choisir D2 (écouter sur le Redmi) ou D3 (envoyer le micro du Redmi au correspondant), appliquer hors appel, démarrer la capture, puis armer avant d’appeler. D3 est un essai TX seul : il n’enregistre rien et la notification permet l’arrêt immédiat. Le correspondant doit confirmer le micro. Le routage natif Android reste inchangé hors appel.",15);
        text(l,"Pont audio · une direction par appel",21);
        rxStatus=new TextView(this);l.addView(rxStatus);
        rxArm=button(l,"Armer le mode choisi pour le prochain appel",()->{
            if(!LabModes.softwareBridge(selectedMode())){toast("L’armement audio sert seulement aux modes D2 et D3.");return;}
            if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},160);toast("Autoriser la permission microphone puis appuyer de nouveau sur Armer.");return;}
            if(!risk.isChecked()){toast("Cocher l’autorisation des stratégies expérimentales.");return;}
            startForegroundService(new Intent(this,RxBridgeService.class).putExtra("mode",selectedMode()));
        });
        rxStop=button(l,"Arrêter immédiatement le pont",()->startService(new Intent(this,RxBridgeService.class).setAction("stop")));
        LinearLayout advanced=new LinearLayout(this);advanced.setOrientation(1);advanced.setVisibility(android.view.View.GONE);
        button(root,"Outils avancés · HCI, firmware et autres modes",()->advanced.setVisibility(advanced.getVisibility()==android.view.View.VISIBLE?android.view.View.GONE:android.view.View.VISIBLE));
        root.addView(advanced);
        l=advanced;
        text(l,"Mode expérimental supplémentaire",21);
        java.util.ArrayList<String> advancedNames=new java.util.ArrayList<>();advancedNames.add("Aucun · garder le parcours principal");for(int i=1;i<ADVANCED_IDS.length;i++)advancedNames.add(LabModes.NAMES[LabModes.index(ADVANCED_IDS[i])]);
        advancedModes=new Spinner(this);advancedModes.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,advancedNames));advancedModes.setSelection(0);l.addView(advancedModes);
        advancedModes.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int i,long id){showMode();}});
        text(l,"Transport SCO · diagnostic natif",21);
        transportStatus=new TextView(this);l.addView(transportStatus);
        text(l,"Module Bluetooth attendu : 1.7.10. D2/D3 ne s’ouvrent qu’après confirmation du mode, du SCO et du routage réel. Pour D3, parler normalement pendant l’appel et demander au correspondant si la voix est claire. Arrêter le pont ou raccrocher pour couper l’accès micro. Une nouvelle trace HCI n’est pas nécessaire.",15);
        button(l,"1 · Préparer la trace HCI",()->new AlertDialog.Builder(this).setTitle("Activer temporairement la trace Bluetooth ?")
            .setMessage("Android pourra stocker un journal Bluetooth brut sensible sur le téléphone pendant cet essai. Le lab ne partage que des métadonnées techniques : pas de contenu ACL, numéro, adresse, clé ou son. Le réglage précédent sera sauvegardé. Désactiver/réactiver Bluetooth hors appel ensuite ; le lab ne supprime aucun appareil appairé. MIUI peut ignorer ce réglage AOSP.")
            .setNegativeButton("Annuler",null).setPositiveButton("Préparer",(d,w)->task(()->{TransportDiagnostic.setLogging(this,true);ui.post(()->toast("Trace demandée. Désactiver puis réactiver Bluetooth dans ses réglages, attendre la reconnexion du Samsung, puis effectuer l’essai."));})).show());
        button(l,"2 · Ouvrir les réglages Bluetooth",()->{try{startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));}catch(ActivityNotFoundException e){toast("Ouvrir les réglages Bluetooth manuellement.");}});
        button(l,"Après collecte · restaurer la journalisation",()->new AlertDialog.Builder(this).setTitle("Rapports déjà collectés ?")
            .setMessage("Collecter d’abord le firmware et les métadonnées HCI. Le redémarrage Bluetooth après restauration peut supprimer les traces système. Aucun changement des appareils appairés.")
            .setNegativeButton("Annuler",null).setPositiveButton("Restaurer",(d,w)->task(()->{TransportDiagnostic.setLogging(this,false);ui.post(()->toast("Réglage précédent restauré. Désactiver/réactiver Bluetooth hors appel pour l’appliquer."));})).show());
        text(l,"Analyser le firmware audio",21);
        firmwareStatus=new TextView(this);l.addView(firmwareStatus);
        firmware=button(l,"Collecter les fichiers audio système",()->new AlertDialog.Builder(this).setTitle("Collecte statique, hors appel")
            .setMessage("Copie en lecture seule des bibliothèques audio/Bluetooth et XML système (128 Mio maximum). Les traces HCI existantes sont lues pour extraire uniquement les métadonnées de transport ; les paquets bruts et le son sont exclus du ZIP. Les binaires sont conservés à l’identique. Le rapport d’appels précédent reste disponible. Garder les appels arrêtés pendant la collecte.")
            .setNegativeButton("Annuler",null).setPositiveButton("Collecter",(d,w)->startForegroundService(new Intent(this,FirmwareService.class))).show());
        firmwareInspect=button(l,"Vérifier le contenu du ZIP firmware",()->inspectZip("HFP-Firmware.zip",new String[]{"README.txt","inventory.tsv","hci-transport.txt","errors.txt","system-info.txt"}));
        firmwareShare=button(l,"Partager le firmware dans ChatGPT",()->{
            Intent i=new Intent(Intent.ACTION_SEND).setType("application/zip").putExtra(Intent.EXTRA_STREAM,ReportProvider.FIRMWARE);
            i.setClipData(ClipData.newUri(getContentResolver(),"Firmware audio",ReportProvider.FIRMWARE));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try{startActivity(Intent.createChooser(i,"Partager le firmware dans ChatGPT"));}catch(ActivityNotFoundException e){toast("Aucune application de partage disponible.");}
        });
        l=root;
        text(l,"2 · Session de diagnostic",21);
        start=button(l,"Démarrer une session de capture",()->{
            Runnable run=()->action(CaptureService.START);
            if(new File(getFilesDir(),"sessions.json").exists())new AlertDialog.Builder(this).setTitle("Nouvelle session ?").setMessage("Exporter d’abord la session précédente. La nouvelle session remplace les journaux de travail ; le dernier ZIP est conservé comme précédent.").setNegativeButton("Annuler",null).setPositiveButton("Démarrer",(d,w)->run.run()).show();else run.run();
        });
        snap=button(l,"Instantané AudioPolicy / AudioFlinger",()->action(CaptureService.SNAP));
        rate=button(l,"Après raccrochage : noter réception et microphone",this::rating);
        stop=button(l,"Terminer et créer le ZIP",()->action(CaptureService.STOP));
        text(l,"Attendre « Capture confirmée », appeler, raccrocher, noter le résultat puis changer de mode. La capture reste ouverte entre les essais. Le compteur concerne les événements filtrés.",15);
        text(l,"3 · Vérifier et partager",21);
        inspect=button(l,"Vérifier les fichiers du rapport",this::inspectReport);
        share=button(l,"Partager le rapport dans ChatGPT",()->{
            new AlertDialog.Builder(this).setTitle("Partager le rapport ?").setMessage("Le masquage est partiel. Vérifier les fichiers avant partage. Choisir ChatGPT dans la feuille de partage.").setNegativeButton("Annuler",null).setPositiveButton("Partager",(d,w)->{
                Intent i=new Intent(Intent.ACTION_SEND).setType("application/zip").putExtra(Intent.EXTRA_STREAM,ReportProvider.URI);
                i.setClipData(ClipData.newUri(getContentResolver(),"Rapport HFP",ReportProvider.URI));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try{startActivity(Intent.createChooser(i,"Partager le rapport dans ChatGPT"));}catch(ActivityNotFoundException e){toast("Aucune application de partage disponible.");}
            }).show();
        });
        text(l,"Sécurité : aucune lecture PCM /proc/asound, aucun tinymix, aucune modification vendor. Une réussite d’AudioPatch n’atteste pas du transport Bluetooth. En cas de route résiduelle signalée, arrêter les essais et exporter.",14);
        setContentView(scroll);refresh();
    }
    private void controlCall(String action){final String number=action.equals("dial")?phoneNumber.getText().toString().replace(" ",""):"-";
        if(action.equals("dial")&&!number.matches("[+]?[0-9]{1,20}")){toast("Saisir un numéro composé de chiffres, avec + facultatif.");return;}
        task(()->{
            String p=RxBridgeService.alive?prefs().getString("probe",""):LabProbe.read(this);
            if(RxBridgeService.alive&&System.currentTimeMillis()-prefs().getLong("probe_at",0)>4000)throw new IOException("État Bluetooth périmé : attendre une réponse récente.");
            if(!p.contains("version=1.7.10 ")||!p.contains(" control=true "))throw new IOException("Commandes HFP indisponibles : vérifier le module 1.7.10 chargé après redémarrage.");
            if(action.equals("dial")&&p.contains(" busy=true "))throw new IOException("Un appel est déjà actif.");
            if(action.equals("dial")&&p.contains(" requested=soft_rx ")&&(!RxBridgeService.alive||!getSharedPreferences("rx",0).getBoolean("armed",false)))throw new IOException("Armer D2 avant de composer.");
            if(action.equals("dial")&&p.contains(" requested=soft_tx ")&&(!RxBridgeService.alive||!getSharedPreferences("rx",0).getBoolean("armed",false)))throw new IOException("Armer D3 avant de composer.");
            String token=Long.toHexString(System.nanoTime());
            Root.command("setprop debug.hfpvoipfix.ctrl '"+token+":"+action+":"+number+"'",1000,5);
            long until=System.currentTimeMillis()+6000;
            while(System.currentTimeMillis()<until){
                Thread.sleep(350);String reply=RxBridgeService.alive?prefs().getString("probe",""):LabProbe.read(this);
                java.util.Map<String,String> fields=RxGate.parse(reply);
                if(token.equals(fields.get("ctrl_token"))){String result=fields.get("ctrl_result");Root.command("setprop debug.hfpvoipfix.ctrl ''",1000,4);ui.post(()->toast("queued".equals(result)?"Commande transmise au HFP Client. Vérifier l’état de l’appel.":"Commande non exécutée : "+result+" · "+fields.get("ctrl_error")));return;}
            }
            throw new IOException("Commande sans accusé : vérifier le Samsung avant toute nouvelle tentative.");
        });
    }
    private void applyMode(){final String choice=selectedMode();
        if(LabModes.risky(choice)&&!risk.isChecked()){toast("Autoriser explicitement les stratégies expérimentales.");return;}
        task(()->{
            String probe=LabProbe.read(this);
            if(!probe.contains("version=1.7.10 "))throw new IOException("Ancien module encore chargé : redémarrer une fois pour charger 1.7.10.");
            if(!probe.contains(" busy=false "))throw new IOException("Appel ou SCO actif : attendre le raccrochage complet.");
            if(!choice.equals("observe")&&probe.contains(" dirty=true "))throw new IOException("Nettoyage audio incomplet : exporter et arrêter les essais.");
            if(!choice.equals("observe")&&(!probe.contains(" route=true ")||!probe.contains(" calls=true ")))throw new IOException("Hooks de suivi indisponibles : seule Observation est autorisée.");
            if(LabModes.bypass(choice)&&!probe.contains(" bypass=true "))throw new IOException("Mode D indisponible sur ce processus.");
            if((choice.equals("bypass_comm")||LabModes.softwareBridge(choice))&&!probe.contains(" disconnect=true "))throw new IOException("D1 indisponible : hook de déconnexion manquant.");
            if(LabModes.voip(choice)&&!probe.contains(" voip=3 "))throw new IOException("Hooks VoIP incomplets.");
            String answer=Root.command("setprop "+LabModes.PROP+" "+choice+" && getprop "+LabModes.PROP,1000,5).trim();
            if(!choice.equals(answer))throw new IOException("Mode non confirmé par getprop");
            requested=choice;
            String phase=prefs().getString("phase","idle");if(phase.equals("running"))SessionStore.note(this,"Mode demandé : "+choice);
            LabProbe.read(this);
            ui.post(()->toast("Mode demandé : "+choice+". Les logs du prochain appel confirmeront les opérations appliquées."));
        });
    }
    private void rating(){try{
        String[] calls=SessionStore.ended(this);if(calls.length==0){toast("Aucun appel terminé détecté dans cette capture. Ne pas attribuer de résultat à un appel non identifié.");return;}
        LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(25,10,25,10);
        text(l,"Appel terminé",16);Spinner call=new Spinner(this);call.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,calls));call.setSelection(calls.length-1);l.addView(call);
        text(l,"Réception sur le Redmi",16);Spinner rx=new Spinner(this);rx.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,RESULTS));l.addView(rx);
        text(l,"Microphone : ce qu’entend le correspondant",16);Spinner tx=new Spinner(this);tx.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,RESULTS));l.addView(tx);
        EditText other=new EditText(this);other.setHint("Commentaire facultatif (sans numéro ni nom)");l.addView(other);
        new AlertDialog.Builder(this).setTitle("Observation auditive").setView(l).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w)->{
            try{String key=calls[call.getSelectedItemPosition()].split(" · ")[0];SessionStore.result(this,key,RESULTS[rx.getSelectedItemPosition()],RESULTS[tx.getSelectedItemPosition()],other.getText().toString());toast("Résultat enregistré pour "+key);}catch(Exception e){toast("Erreur : "+e.getMessage());}
        }).show();
    }catch(Exception e){toast("Erreur : "+e.getMessage());}}
    private void inspectReport(){String[] names={"summary.txt","sessions.json","errors.txt","hfp-events.log","audio-policy.txt","audio-flinger.txt","mediatek-audio.log","system-info.txt","audio-config.txt","audio-manager.txt"};
        inspectZip("HFP-Rapport.zip",names);
    }
    private void inspectZip(String archive,String[] names){
        new AlertDialog.Builder(this).setTitle("Vérifier le ZIP").setItems(names,(d,w)->task(()->{
            StringBuilder content=new StringBuilder();boolean cut=false;
            try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(new File(getFilesDir(),archive))){
                java.util.zip.ZipEntry entry=zip.getEntry(names[w]);if(entry==null)throw new IOException("Fichier absent");
                try(BufferedReader in=new BufferedReader(new InputStreamReader(zip.getInputStream(entry),StandardCharsets.UTF_8))){String line;
                    while((line=in.readLine())!=null){if(content.length()>120000){cut=true;break;}content.append(line).append('\n');}
                }
            }
            final String shown=content.toString()+(cut?"\nAPERÇU LIMITÉ : ouvrir le ZIP pour consulter la totalité.":"");
            ui.post(()->{ScrollView sc=new ScrollView(this);TextView t=new TextView(this);t.setText(shown);t.setTextSize(12);t.setTextIsSelectable(true);sc.addView(t);
                new AlertDialog.Builder(this).setTitle(names[w]).setView(sc).setPositiveButton("Fermer",null).show();});
        })).setNegativeButton("Fermer",null).show();
    }
    private void refresh(){if(status==null)return;
        String phase=prefs().getString("phase","idle"),p=prefs().getString("probe","");long at=prefs().getLong("probe_at",0);
        if((phase.equals("running")||phase.equals("starting")||phase.equals("processing"))&&!CaptureService.alive){
            phase="error";prefs().edit().putString("phase","error").putString("status","Service interrompu. Terminer pour récupérer les fichiers conservés.").apply();
        }
        boolean fresh=System.currentTimeMillis()-at<15000;
        String module=p.isEmpty()?"non confirmé":(fresh?"réponse récente":"dernière réponse il y a "+((System.currentTimeMillis()-at)/1000)+" s");
        String applied="non confirmé";java.util.regex.Matcher m=java.util.regex.Pattern.compile("applied=(\\w+)").matcher(p);if(m.find())applied=m.group(1).equals("idle")?"aucun appel (dernière vérification)":m.group(1)+" (dernière vérification)";
        status.setText("Root Magisk : "+prefs().getString("root","non vérifié")+"\nModule LSPosed / Bluetooth : "+module+
            "\nMode demandé : "+requested+"\nStratégie retenue : "+applied+"\nDernier appel capturé : "+prefs().getString("last_mode","aucun")+" · état HFP "+prefs().getString("last_call_state","—")+"\nCapture : "+prefs().getString("status","inactive")+
            "\nApplication technique : "+prefs().getString("strategy","non vérifiée")+"\nÉvénements écrits : "+prefs().getLong("count",0)+"\nDernière anomalie : "+prefs().getString("error","aucune"));
        android.content.SharedPreferences fp=getSharedPreferences("firmware",0);
        String fs=fp.getString("phase","idle");
        if(fs.equals("running")&&!FirmwareService.alive){fs="error";fp.edit().putString("phase",fs).putString("status","Collecte interrompue : relancer hors appel.").apply();}
        boolean collecting=FirmwareService.alive;
        if(transportStatus!=null)transportStatus.setText(getSharedPreferences("transport",0).contains("previous")?"Trace demandée par le lab : réglage précédent sauvegardé, à restaurer après collecte. Activation réelle à confirmer dans hci-transport.txt.":"Aucun réglage de trace détenu par le lab. Le réglage système peut être différent.");
        boolean bridging=RxBridgeService.alive;
        if(rxStatus!=null)rxStatus.setText(getSharedPreferences("rx",0).getString("status","Pont non armé."));
        if(rxArm!=null){rxArm.setEnabled(!bridging&&!collecting&&!working&&phase.equals("running")&&risk.isChecked()&&LabModes.softwareBridge(selectedMode()));rxStop.setEnabled(bridging);}
        if(probeButton!=null)probeButton.setEnabled(!bridging&&!working);
        if(firmwareStatus!=null)firmwareStatus.setText(fp.getString("status","Aucune collecte statique effectuée."));
        if(firmware!=null){firmware.setEnabled(!bridging&&!CaptureService.alive&&!collecting&&!working);boolean fr=fs.equals("ready")&&!collecting&&new File(getFilesDir(),"HFP-Firmware.zip").isFile();firmwareShare.setEnabled(fr);firmwareInspect.setEnabled(fr&&!working);}
        boolean running=phase.equals("running"), starting=phase.equals("starting"), processing=phase.equals("processing");
        if(start!=null){start.setEnabled(!collecting&&!running&&!starting&&!processing&&!working);stop.setEnabled(!bridging&&(running||starting||phase.equals("error"))&&!processing);snap.setEnabled(running);rate.setEnabled(running);}
        if(apply!=null)apply.setEnabled(!bridging&&!collecting&&!working&&!starting&&!processing&&(!LabModes.risky(selectedMode())||risk.isChecked()));
        boolean ready=phase.equals("ready")&&new File(getFilesDir(),"HFP-Rapport.zip").isFile();if(share!=null){share.setEnabled(ready);inspect.setEnabled(ready&&!working);}
    }
    private final Runnable poll=new Runnable(){public void run(){refresh();if(visible)ui.postDelayed(this,1000);}};
    @Override protected void onResume(){super.onResume();visible=true;poll.run();}
    @Override protected void onPause(){visible=false;ui.removeCallbacks(poll);super.onPause();}
    @Override protected void onDestroy(){work.shutdown();super.onDestroy();}
}
