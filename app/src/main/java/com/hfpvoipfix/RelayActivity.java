package com.hfpvoipfix;
import android.app.*;import android.os.*;import android.content.*;import android.widget.*;
public final class RelayActivity extends Activity {
 private TextView status,commandStatus;private EditText number;private final java.util.concurrent.atomic.AtomicBoolean controlling=new java.util.concurrent.atomic.AtomicBoolean();private final Handler ui=new Handler(Looper.getMainLooper());private boolean visible;private String pendingMode="telecom_duplex";private boolean pendingCompare,pendingDiagnostic;
 private Button button(LinearLayout l,String s,Runnable action){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->action.run());l.addView(b);return b;}
 @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout l=new LinearLayout(this);l.setOrientation(1);int pad=(int)(20*getResources().getDisplayMetrics().density);l.setPadding(pad,pad,pad,pad);TextView title=new TextView(this);title.setText("Relais d’appels · 1.9.17");title.setTextSize(25);l.addView(title);status=new TextView(this);status.setTextSize(17);l.addView(status);
  TextView help=new TextView(this);help.setText("Relais local : son et micro du Redmi. Relais AA : essai avec l’autoradio, micro du Redmi. D6 contrôlable : commandes dans Téléphone et Android Auto, à vérifier avec le son. AA isolé reste le repli audio et possède des commandes HFP directes ci-dessous. Garder Android Auto branché en USB. L’appel ne sera pas présenté dans l’interface Téléphone du Redmi ou d’AA. Changer de relais hors appel. Les diagnostics facultatifs ci-dessous ne constituent pas des correctifs.");l.addView(help);
  button(l,"Activer le relais",()->enable("telecom_duplex"));button(l,"Activer D6 contrôlable · essai Android Auto",()->enable("telecom_aa_control"));button(l,"Activer AA isolé · repli audio",()->enable("projection_only"));button(l,"Désactiver le relais",()->{getSharedPreferences("relay",0).edit().putBoolean("enabled",false).apply();startService(new Intent(this,RelayService.class).setAction("stop"));refresh();});
  TextView controls=new TextView(this);controls.setText("Commandes directes · AA isolé uniquement. Pour D6 contrôlable, utiliser Téléphone sur Redmi ou Android Auto.");l.addView(controls);
  commandStatus=new TextView(this);l.addView(commandStatus);
  number=new EditText(this);number.setHint("Numéro à appeler depuis le Samsung");number.setInputType(android.text.InputType.TYPE_CLASS_PHONE);l.addView(number);
  button(l,"Appeler · AA isolé",()->control("dial"));button(l,"Répondre · AA isolé",()->control("answer"));button(l,"Raccrocher / refuser · AA isolé",()->control("hangup"));
  LinearLayout diagnostic=new LinearLayout(this);diagnostic.setOrientation(1);diagnostic.setVisibility(android.view.View.GONE);
  button(l,"Diagnostics AA · facultatif",()->diagnostic.setVisibility(diagnostic.getVisibility()==android.view.View.VISIBLE?android.view.View.GONE:android.view.View.VISIBLE));l.addView(diagnostic);
  button(diagnostic,"Ancien relais AA · avec Telecom",()->enable("telecom_aa"));
  TextView purpose=new TextView(this);purpose.setText("Ces essais servent à comparer des causes précises. Ils changent temporairement les étapes ou le profil audio. Choisir « Activer le relais AA » pour revenir au fonctionnement habituel.");diagnostic.addView(purpose);
  button(diagnostic,"Isoler micro et transmission · 1 appel",()->new AlertDialog.Builder(this).setTitle("Isoler le moment de la coupure")
   .setMessage("Pendant un appel d’au moins 70 secondes : réception seule, ajout du micro Redmi, ajout de l’envoi vers Samsung, retrait de l’envoi puis du micro, puis essai média avec micro et envoi. Chaque étape dure 8 secondes. Cela vérifie si la coupure suit un changement précis ; ce n’est pas un mode d’usage normal. Le passage au média relance aussi la piste et sa priorité audio.")
   .setNegativeButton("Annuler",null).setPositiveButton("Préparer cet essai",(d,w)->enable("telecom_aa",false,true)).show());
  button(diagnostic,"Comparer sorties et micros · 4 appels",()->new AlertDialog.Builder(this).setTitle("Comparer quatre profils AA")
   .setMessage("Quatre appels de 30 secondes depuis le Samsung : navigation + source micro communication vocale ; média + même source ; média + source MIC ; navigation + source MIC. La source demandée reste soumise au routage Android. Un profil est gardé pour chaque appel, même si le SCO se reconnecte. Cet essai compare les sorties et les sources micro ; il ne répare pas automatiquement le relais.")
   .setNegativeButton("Annuler",null).setPositiveButton("Préparer les quatre appels",(d,w)->enable("telecom_aa",true)).show());
  button(l,"Ouvrir Téléphone",()->{try{startActivity(new Intent(Intent.ACTION_DIAL));}catch(ActivityNotFoundException e){status.setText("Application Téléphone indisponible.");}});
  button(l,"Configuration initiale des comptes d’appel",()->{try{startActivity(new Intent(android.telecom.TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS));}catch(ActivityNotFoundException e){status.setText("Dans Téléphone, activer le compte Samsung/HFP et le choisir pour les appels.");}});
  button(l,"Capture et rapport · Téléchargements",()->startActivity(new Intent(this,ModeActivity.class)));ScrollView scroll=new ScrollView(this);scroll.addView(l);setContentView(scroll);refresh();
 }
 private void control(String action){
  if(!controlling.compareAndSet(false,true))return;
  final String target="dial".equals(action)?number.getText().toString().replace(" ",""):"-";
  new Thread(()->{try{String message=RemoteCallController.send(this,action,target);ui.post(()->commandStatus.setText(message));}
   catch(Exception e){ui.post(()->commandStatus.setText(String.valueOf(e.getMessage())));}finally{controlling.set(false);}},"hfp-call-command").start();
 }
 private void enable(String mode){enable(mode,false);}
 private void enable(String mode,boolean compare){enable(mode,compare,false);}
 private void enable(String mode,boolean compare,boolean diagnostic){pendingMode=mode;pendingCompare=compare;pendingDiagnostic=diagnostic;if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},190);return;}
  new Thread(()->{try{
   if(LabModes.controlled(mode)&&!AaCarFocusHook.exactApk(getPackageManager().getApplicationInfo("com.google.android.projection.gearhead",0).sourceDir))throw new java.io.IOException("D6 contrôlable requiert la version Android Auto auditée. Utiliser AA isolé pour conserver le chemin audio.");
   java.util.Map<String,String> current=RxGate.parse(LabProbe.read(this));
   if(!"false".equals(current.get("busy"))||!"false".equals(current.get("dirty")))throw new java.io.IOException("Attendre la fin de l’appel et du nettoyage avant de préparer les essais.");
   if(RxBridgeService.alive&&!RelayService.enabled(this))throw new java.io.IOException("Terminer le pont de diagnostic avant d’activer le relais.");
   if(RelayService.enabled(this)&&!mode.equals(RelayService.mode(this))){
    java.util.Map<String,String> v=RxGate.parse(LabProbe.read(this));
    if(!"false".equals(v.get("busy"))||!"false".equals(v.get("dirty")))throw new java.io.IOException("Attendre la fin de l’appel et du nettoyage avant de changer de relais.");
    getSharedPreferences("relay",0).edit().putString("mode",mode).apply();
    if(RxBridgeService.alive)startService(new Intent(this,RxBridgeService.class).setAction("reconfigure"));
   }
   getSharedPreferences("aa_diagnostic",0).edit().putBoolean("enabled",diagnostic).putString("call","").apply();
   getSharedPreferences("aa_trials",0).edit().putBoolean("enabled",compare).putInt("index",0).putString("call","").putInt("assigned",-1).apply();
   getSharedPreferences("relay",0).edit().putString("mode",mode).putBoolean("enabled",true).apply();
   startForegroundService(new Intent(this,RelayService.class));ui.post(this::refresh);
  }catch(Exception e){ui.post(()->status.setText(String.valueOf(e.getMessage())));}},"relay-select").start();
 }
 private void refresh(){if(status==null)return;status.setText(RelayService.enabled(this)?((LabModes.car(RelayService.mode(this))?(LabModes.isolated(RelayService.mode(this))?"D6 · AA isolé · appels sur Samsung\n":"D6 · AA avec Telecom\n"):"D5 · Relais local\n")+(RxBridgeService.alive?getSharedPreferences("rx",0).getString("status","Préparation…"):getSharedPreferences("relay",0).getString("status","Préparation du relais…"))):"Relais désactivé");}
 private final Runnable poll=new Runnable(){public void run(){refresh();if(visible)ui.postDelayed(this,2000);}};
 @Override protected void onResume(){super.onResume();visible=true;if(RelayService.enabled(this))startForegroundService(new Intent(this,RelayService.class));poll.run();}
 @Override protected void onPause(){visible=false;ui.removeCallbacks(poll);super.onPause();}
 @Override public void onRequestPermissionsResult(int n,String[] p,int[] r){super.onRequestPermissionsResult(n,p,r);if(n==190&&r.length>0&&r[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)enable(pendingMode,pendingCompare,pendingDiagnostic);}
}
