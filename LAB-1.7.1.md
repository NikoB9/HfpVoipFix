# HfpVoipLab 1.7.1 — métadonnées de transport HCI

1.7.1 garde les hooks Bluetooth de 1.7.0. Cette révision ajoute une préparation explicite et restaurable de `persist.bluetooth.btsnooplogmode`, la collecte statique de quelques bibliothèques Bluetooth du système, et l'extraction locale de métadonnées SCO/eSCO. Aucun nouveau hook LSPosed, scope, AudioPatch ou réglage audio n'est ajouté.

## Résumé public des observations

L'analyse locale de fichiers système et les rapports de test ont été utilisés pour orienter les versions suivantes. Les noms de fichiers, empreintes, adresses mémoire, symboles vendor et détails horodatés ont été retirés de cette note publique ; les archives brutes restent hors du dépôt.

Les essais initiaux ont établi que la route d'entrée pouvait s'ouvrir pendant un appel sans fournir d'échantillons non nuls. Une trace Bluetooth ultérieure a confirmé l'établissement de liaisons synchrones, mais ne contenait pas de paquets audio HCI exploitables. Cela ne prouve ni l'absence de trafic audio sur un chemin hors HCI, ni une panne matérielle.

Les journaux d'appel ont montré un décalage entre le mode large bande négocié et le format observé à l'ouverture du chemin RX. Cette piste a conduit à tester une configuration MediaTek existante, puis à séparer les changements de mode des changements d'activation du flux. La première correction de bande n'a pas suffi à rétablir les échantillons ; la réception claire a été confirmée après un changement ultérieur documenté dans les notes de version.

Ces résultats justifient de conserver un pont RX contrôlé et une vérification des échantillons réels. Ils ne justifient pas de modifier des fichiers système ni de publier des rapports bruts.

### Évaluation d'AA Passenger

AA Passenger n'est pas un pont d'appel HFP. Son dépôt décrit un modèle à deux appareils : un appareil « driver » exécute Android Auto et est relié à la voiture ; un appareil « passenger » lui envoie des commandes de navigation, des photos et de l'audio média par Wi-Fi Direct ou point d'accès. Le dépôt présente le streaming comme audio média et renvoie vers un service de capture média privilégié. Cela ne fournit ni enregistrement Telecom des appels HFP reçus par l'appareil local, ni transport de voix HFP, ni retour micro d'appel. L'API Android standard de capture de lecture exige un usage média/jeu/inconnu ; elle ne rend pas la voix d'appel HFP capturable comme audio média. AA Passenger peut donc servir plus tard à contrôler Android Auto ou à envoyer de la musique, mais pas à contourner le chemin SCO muet constaté ici.

Une implémentation noyau publique comparable réveille l'interruption lorsque l'un des sens audio quitte l'état inactif. Cela ne permet pas d'affirmer que la ROM testée utilise exactement le même code. La capture micro ne doit donc pas être utilisée comme moyen de « réveiller » RX ; TX doit rester un essai séparé.

Sources de référence Android 11 :

- https://android.googlesource.com/platform/system/bt/+/refs/tags/android-11.0.0_r1/hci/include/bt_vendor_lib.h — rôle de `BT_VND_OP_SCO_CFG` et `BT_VND_OP_SET_AUDIO_STATE`.
- https://android.googlesource.com/platform/system/bt/+/refs/tags/android-11.0.0_r1/hci/src/btsnoop.cc — propriétés du journal, format btsnoop H4, chemins et rotation.
- https://android.googlesource.com/platform/system/bt/+/refs/tags/android-11.0.0_r1/bta/hf_client/bta_hf_client_sco.cc — paramètres eSCO HF Client.
- https://github.com/MiCode/Xiaomi_Kernel_OpenSource — source noyau publique ; son implémentation ne confirme pas celle de la ROM testée.

Ces éléments affinent la piste : le wrapper standard de commandes vendor ne montre pas une configuration d'état SCO prise en charge, et le chemin audio observé précédemment n'a pas reçu d'IRQ. Ils ne prouvent pas que le wrapper standard est le seul chemin propriétaire, ni que l'absence d'IRQ est la cause initiale.

## Trace transport

L'utilisateur active cette trace seulement hors appel. La valeur préexistante autorisée (`disabled`, `filtered`, `full` ou chaîne vide) est conservée avant passage à `full`; les valeurs inconnues et réglages modifiés à l'extérieur refusent d'être écrasés. Après la collecte, le bouton de restauration remet la valeur précédente. La désactivation/réactivation Bluetooth est manuelle et se fait hors appel; l'application ne désappaire aucun appareil.

Les journaux Bluetooth bruts peuvent contenir des données sensibles. L'outil de diagnostic n'exporte que des métadonnées filtrées ; vérifier les archives avant partage et ne pas ajouter les rapports ou fichiers système bruts au dépôt public.

Le mode « complet » peut être ignoré par MIUI. Le statut `full` prouve uniquement la valeur de propriété, pas que la pile l'a activée. Il faut voir les paquets analysés pour confirmer la capture; la trace peut ne pas contenir de trafic SCO HCI lorsque le flux utilise PCM. Une connexion eSCO réussie ne prouve pas la présence d'audio.

Le décodeur limite chaque entrée à 128 Mio et le rapport à 10 000 événements pertinents; les paquets tronqués, fichier vide/incompatible, absence du log et saturations sont affichés explicitement. Le lecteur n'ouvre que les quatre chemins AOSP fixés et ne lit ni `/proc/asound`, ni NVRAM, ni flux PCM.

## Vérification

- Tests hôte: hooks (54 assertions), protocole contrôle, handshake du flux de statut, garde RX, ZIP de rapports/firmware et décodeur HCI (layouts de commandes legacy/enhanced, événements, confidentialité, troncature et limites).
- Compilation JDK 17 / Android 11, APK versionCode 36, package `com.hfpvoipfix`, assets LSPosed `xposed_init`, DEX, alignement et certificat v3 vérifiés.
- La signature reste identique à 1.7.0.
- Aucun émulateur fonctionnel ni appareil connecté ici. Le contrôle visuel de l'interface et l'efficacité de la capture Bluetooth sur MIUI restent à vérifier sur le téléphone.

## Essai ciblé

Mettre à jour HfpVoipLab. Préparer la trace hors appel, basculer Bluetooth, attendre la reconnexion, sélectionner D2, démarrer la capture et armer la réception avant l'appel. À la fin, vérifier les métadonnées filtrées, noter séparément RX et TX, puis restaurer le réglage Bluetooth. Ne pas partager ni versionner d'archive système brute.
