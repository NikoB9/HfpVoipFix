# 1.8.3 — chargement Telecom dans le processus partagé

La capture LSPosed montre que Bluetooth et com.android.server.telecom sont correctement cochés. Les journaux montrent le module HFP dans Bluetooth, mais aucun chargement de ce module au PID 1461 associé aux composants système dont Telecom. Cela motive un chemin supplémentaire de chargement ; les traces ne prouvent pas seules la cause complète.

Le module accepte les callbacks android/system et attend le classloader de l’APK com.android.server.telecom via LoadedApk.getClassLoader avant d’installer les hooks Telecom existants. Les autres APK ne déclenchent aucune installation. Les interventions audio restent limitées par la garde D5/Android 11/begonia/patch natif/appel HFP unique au premier plan/hors urgence, comme en 1.8.2.

Les événements de chargement et décisions Telecom sont également écrits dans les journaux persistants LSPosed. Le rapport ajoute telecom-hook.txt, filtré au démarrage de la session : aucune recherche manuelle de ligne nécessaire.

Installation : APK 1.8.3, module LSPosed actif avec Bluetooth, Gestion des appels et Cadre du sous-système / Framework système. Réactiver Magisk 1.7.22 avant un redémarrage unique. Puis D5, appliquer hors appel, démarrer la session, un appel court via Téléphone avec le compte Samsung/HFP et exporter le rapport.

Validation logicielle : fixture du chargement différé Telecom via le processus framework, rejet des APK sans rapport, tests de portée audio existants, tests Bluetooth et compilation. Chargement et audio MIUI à confirmer sur téléphone. Pas de nouveau patch natif ni validation Android Auto.
