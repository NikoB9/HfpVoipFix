# 1.8.4 — états d’appel et reprise silencieuse du suivi

Les rapports 1.8.3 montrent deux défauts distincts : le pont s’ouvrait pendant un appel entrant encore en sonnerie, et une interruption de sonde de 3 s pouvait condamner une session audio active. Le dernier essai montre aussi RX RMS/pic nuls avant l’arrêt, malgré MIC non nul : l’arrêt du suivi ne prouve pas la cause de ce silence.

## Correction de l’état Telecom

La garde utilisait 4 à la place de 5 pour ACTIVE. AOSP Android 11 CallState définit DIALING=3, RINGING=4 et ACTIVE=5, tandis que les états HFP emploient ACTIVE=0. Les deux espaces ne doivent pas être confondus. La garde du hook accepte maintenant DIALING/ACTIVE et refuse RINGING. Les fixtures précédentes avaient reproduit cette erreur ; elles sont corrigées avec un test de rejet explicite de RINGING.

Source : https://android.googlesource.com/platform/packages/services/Telecomm/+/refs/heads/android11-release/src/com/android/server/telecom/CallState.java

Le suivi Bluetooth publie call_active, calculé à partir des appels HFP actifs, nettoyé au raccrochage ou à la déconnexion. D5 n’ouvre les objets audio qu’avec call_active=true et un mode audio normal confirmé. D4 reste indépendant de cette nouvelle condition.

## Sonde intermittente

La fraîcheur autorisant une écriture audio reste limitée à 4 s. Un D4/D5 continu conserve ses objets audio pendant une récupération bornée à 12 s depuis la dernière réponse fraîche : sorties mises en pause et vidées, données entrantes drainées et jetées, aucune voix transférée. Une réponse fraîche du même epoch/SCO permet de reprendre ; un autre appel, autorisation retirée ou absence prolongée arrête le pont. Les erreurs de mode/route restent bloquantes.

Les journaux ajoutent AUDIO_PAUSE/AUDIO_RESUME et call_active. Le diagnostic de chargement reste inclus lorsqu’il est disponible. Aucun nouveau HAL/Magisk ou changement de source/gain audio. Les zéros RX ne sont pas utilisés comme une panne automatique : un interlocuteur silencieux peut légitimement produire du silence. Leur cause et la qualité de reprise restent à valider sur le Redmi.

Installer APK 1.8.4, vérifier LSPosed Bluetooth/Gestion des appels/Cadre du sous-système, réactiver Magisk 1.7.22 puis redémarrer une fois. D5, appliquer hors appel, démarrer la session. Appel sortant avec conversation puis entrant décroché depuis Téléphone, mêmes comptes Samsung/HFP. Vérifier les deux voix et exporter le rapport. Tests hôtes : ACTIVE accepté, RINGING refusé, D5 bloqué avant HFP ACTIVE, interruption et récupération sans élargir la fenêtre de fraîcheur, changement de SCO/refus/expiration bloqués. Pas de validation Android Auto.
