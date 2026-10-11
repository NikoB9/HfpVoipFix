# 1.8.2 — D5 : Telecom et mode audio normal

## Constat et hypothèse

Le dernier essai 1.8.1 confirme D4 pendant environ 80 s : routes stables, flux RX/TX distincts, arrêt après raccrochage. D5 crée bien sa connexion Telecom mais utilise MODE_IN_COMMUNICATION ; 17 des 22 mesures RX/TX ont les mêmes RMS/pics, cohérents avec le retour micro rapporté, puis la route change. Ces statistiques ne prouvent pas l’identité PCM ni la cause exacte dans le HAL.

La source AOSP Android 11 `CallAudioModeStateMachine.VoipCallFocusState.enter` demande le mode communication. L’expérimentation traduit cette seule demande en MODE_NORMAL pour tenter de conserver les chemins D4 tout en gardant la connexion native Telecom et ses commandes.

Source : https://android.googlesource.com/platform/packages/services/Telecomm/+/refs/heads/android11-release/src/com/android/server/telecom/CallAudioModeStateMachine.java

## Intervention limitée

Le module reçoit une portée LSPosed supplémentaire `com.android.server.telecom`. Le hook utilise les entrées VoipCallFocusState et OtherFocusState, avec un contexte local au thread. Il traduit seulement setMode(3) pendant ces callbacks, uniquement si : parcours D5, Android 11/begonia, marqueur natif 1.7.22 actif, un seul appel suivi, appel au premier plan en DIALING/ACTIVE, hors urgence et compte com.android.bluetooth/HfpClientConnectionService. Les autres appels, D4, modes sonnerie/appel SIM et demandes en dehors de ces callbacks gardent leur comportement.

Si MIUI n’expose pas les classes/méthodes requises, le hook ne traduit pas la demande. D5 attend la confirmation du mode normal pendant au maximum cinq secondes avant d’ouvrir l’audio, et arrête le pont si un autre mode reprend le contrôle. Aucune boucle de forçage du mode et aucun changement natif supplémentaire.

Tests hôtes : garde de sélection et callbacks de production face à des fixtures Telecom/Xposed ; précédents tests Bluetooth et sondes conservés. Compilation avec avertissements de dépréciation. Ces vérifications ne valident pas le HAL ou l’interface Téléphone MIUI.

## Installation et essai

Installer 1.8.2 et vérifier LSPosed : Bluetooth **et Telecom / Gestion des appels** (`com.android.server.telecom`). Réactiver le module Magisk 1.7.22 puis redémarrer une fois. Choisir D5, appliquer hors appel, démarrer la session et attendre prêt. Utiliser Téléphone avec le compte Samsung/HFP ; un appel sortant court puis un entrant avec réponse et raccrochage depuis Téléphone. Vérifier les deux voix et l’absence de retour micro. Exporter le rapport.

Si D5 refuse le mode audio, le rapport doit montrer HOOK_READY/OVERRIDE ou UNAVAILABLE dans LAB TELECOM_AUDIO ; son absence permet de repérer une portée LSPosed manquante ou un processus différent. D4 reste disponible. Android Auto n’est pas testé.
