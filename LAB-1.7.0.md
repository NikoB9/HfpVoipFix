# HfpVoipLab 1.7.0

## Bilan vérifié
Le rapport 1.6.1 confirme une ouverture RX réelle pendant l'appel, mais les mesures audio restent nulles. Android indique une négociation large bande alors que le chemin de capture s'ouvre dans un format différent. La perte ultérieure de suivi ne cause pas les zéros précédents. Ce constat ne démontre ni une panne matérielle ni une cause unique liée au codec.

## Changements
- Suivi : abonnement logcat continu dédié, sans fenêtre des N dernières lignes. Un processus root de commande et un processus de lecture, libérés à l'arrêt. Chaque état doit répondre à un nouveau jeton exact. Les états anciens et les copies de statut dans les logs applicatifs sont exclus. Garde de fraîcheur audio de 4 s conservée.
- Commandes depuis l'appareil local : composer un numéro de test, répondre, raccrocher. Les méthodes disponibles sont vérifiées au chargement. Le scope reste limité au service Bluetooth. Aucun appel via la SIM locale ni hook system_server/Telecom supplémentaire. Mode bypass obligatoire, un seul appareil HFP connecté, pas de second appel. Jetons anti-répétition et pas de répétition automatique après délai. Le numéro n'est pas journalisé par nos événements CONTROL ; la propriété est vidée après accusé. Un accusé queued ne prouve pas que le téléphone distant a exécuté la commande.
- Collecteur statique : élargit la sélection des bibliothèques nécessaires au diagnostic local. Les limites et contrôles des chemins restent inchangés. Les fichiers collectés sont destinés à une analyse privée et ne doivent pas être publiés dans le dépôt.

## Codec et limites assumées
Aucun nouveau forçage WBS : la lecture fiable de l'état précédent de la plateforme n'est pas établie. Les modes qui exigent cette valeur restent indisponibles si elle ne peut pas être lue. Ni accès direct au mixeur ou PCM, ni fichier système modifié. Le transport sans interruption n'est pas réparé par cette version. Le microphone du pont reste désactivé.

## Installation et priorité
Même package/certificat. Mise à jour puis un redémarrage pour charger les hooks 1.7.0. Scope LSPosed inchangé.
Priorité : hors appel, utiliser la collecte locale des fichiers système uniquement si l'analyse privée l'exige. Ne pas ajouter l'archive système au dépôt public. Il n'est pas nécessaire de répéter D/D1.
Essai facultatif des nouvelles commandes : sélectionner/appliquer D2 hors appel, démarrer capture, armer et attendre D2 armé, lancer l'appel depuis l'appareil local, puis raccrocher depuis l'appareil distant. Garder l'appareil distant disponible pour sélectionner Bluetooth si nécessaire ou raccrocher. Noter RX, TX non évalué, puis vérifier le rapport avant partage. Aucun autre mode à refaire.

## Validation
54 assertions sur les hooks de production avec doubles Android/Xposed, dont commandes HFP et anti-répétition ; tests protocole numérique ; 100 échanges à jetons distincts au milieu de 150000 lignes parasites sur des processus simulés ; garde RX ; ZIP firmware et rapport. Compilation API30/JDK17/build-tools35, signature stable v3, manifeste, DEX et xposed_init contrôlés. Pas d'appareil ou émulateur Android utilisable : UI et transport physique non validés ici.

Source technique Android 11, utilisée comme référence de signatures puis vérifiée à l'exécution sur MIUI :
https://android.googlesource.com/platform/packages/apps/Bluetooth/+/refs/tags/android-11.0.0_r1/src/com/android/bluetooth/hfpclient/HeadsetClientService.java
