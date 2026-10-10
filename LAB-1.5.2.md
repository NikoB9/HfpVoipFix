# HfpVoipLab 1.5.2 — collecte statique du firmware audio

Cette version ne prétend pas corriger le silence D/D1. Elle fournit les bibliothèques exactes de la ROM nécessaires à l'analyse des commandes HFP et de leurs dépendances.

Installation : mise à jour de 1.5.1 avec le même certificat. Aucune modification des hooks, LabModes ou LabProbe : le module chargé annonce toujours 1.5.1 et reste compatible. Aucun redémarrage nécessaire pour cette collecte.

Hors appel et après arrêt de la capture : utiliser « Collecter les fichiers audio système », attendre « Firmware prêt » et vérifier l'inventaire. Aucun appel n'est nécessaire. Conserver l'archive dans un espace privé pour l'analyse ; ne pas publier l'archive ou l'inventaire dans le dépôt. Le ZIP d'appels précédent est conservé séparément.

Collecte : bibliothèques audio/Bluetooth et XML audio/mixer via une liste fixe, sans parcours récursif. Les liens sont résolus et revérifiés contre la liste autorisée. Aucun /data, NVRAM, /proc, /sys, /dev, PCM, tinymix, écriture vendor ou paramètre audio. Les ELF/XML sont copiés à l'identique ; ce paquet est destiné au diagnostic privé et ne doit pas être publié dans le dépôt.

Limites : 32 Mio par fichier, 128 Mio au total, 120 fichiers, délai 20 s par copie. Inventaire SHA-256, omissions explicites, validation CRC du ZIP. Absence d'un HAL audio.primary lisible = échec explicite ; ancien ZIP préservé. Service de premier plan ; collecte interrompue signalée au retour dans l'interface.

Les bibliothèques non sélectionnées pourront être identifiées après inspection des dépendances ELF. Ce paquet n'inclut pas une capture du transport Bluetooth et ne démontre pas le fonctionnement du SCO.

Validation : compilation API 30, signature v3, alignement, manifeste, DEX et assets. Tests sur hôte des frontières de collecte, du ZIP, des erreurs et des hooks existants. Aucun téléphone ni émulateur Android utilisable dans l'environnement de build ; aucune validation physique sur l'appareil cible.

Référence architecturale (ne démontre pas l'implémentation MIUI) : https://source.android.com/docs/core/audio/hidl-implement — wrapper du HAL historique et bibliothèques vendor. Android 11 impose de travailler avec ses interfaces existantes ; aucune migration AIDL Android 14 n'est introduite.
