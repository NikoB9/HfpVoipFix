# 1.9.5 — D6 via projection vocale, sans second SCO

Le rapport 1.9.4 montre les ports maître 72/76 et véhicule 28/27. En phase TX, la route effective est 27 au lieu de 76 ; le pont bloque l’envoi et arrête au bout de trois secondes. La simple présence de deux ports SCO n’établit pas une indépendance des routes. La capture confirmée permet d’observer ce défaut.

D6 ne demande plus connectAudio vers le véhicule et ne choisit plus la paire SCO du véhicule comme endpoints audio. Un hook Telecom Android 11 intercepte CallAudioRouteStateMachine.setBluetoothOn(String) seulement pour un unique appel HFP non urgent en premier plan (CONNECTING à ACTIVE), sur le modèle/ROM/runtime validés et le mode telecom_aa. Il demande à la machine de route de revenir à l’écouteur via son message USER_SWITCH_EARPIECE et laisse le transport du son au pont. D5, les comptes SIM, urgences, autres appareils/versions et appels multiples sont exclus. Les connexions Bluetooth de profil/commandes ne sont pas désactivées. Il n’y a ni forceSco, modification globale de permission ni réseau simulé.

La réception D6 utilise USAGE_ASSISTANCE_NAVIGATION_GUIDANCE. Les mixes Android Auto observés acceptent cet usage sur leur deuxième canal de projection à 16 kHz. La route effective reste obligatoirement REMOTE_SUBMIX avant envoi ; le micro reste celui du Redmi. Ce scénario explore la restitution via le canal projeté d’annonces pendant un appel ; il ne constitue pas encore une intégration d’appel Android Auto complète. Un autoradio peut ne pas restituer ce canal dans cet état : test matériel requis.

Le chemin D5, la capture corrigée 1.9.4 et le runtime Magisk 1.9.0 sont conservés. Pas de dépendance USB/Wi-Fi. Les lectures réseau restent en lecture seule. L’erreur de numérotation autoradio reste à diagnostiquer.

Source ciblée ROM Android 11 : https://android.googlesource.com/platform/packages/services/Telecomm/+/refs/heads/android11-release/src/com/android/server/telecom/CallAudioRouteStateMachine.java

Vérification : fixture exécutant le hook de production, exclusions D5/SIM/urgence/multiple/premier plan et états ; tests du pont, nettoyage/export et CI/signature. Aucun test matériel disponible.
