# Audit d'architecture HFP → projection Android Auto — 20 octobre 2026

## Conclusion étayée, limites explicites

D6 est actuellement un détournement de la restitution navigation/média, pas un
chemin d'appel projeté démontré. Le code conserve l'appel Telecom HFP, supprime
la demande SCO véhicule et demande la route écouteur. La voix est ensuite lue
avec l'usage navigation pour rejoindre un mix de capture AA. Le rapport fourni
montre des échantillons non nuls dans le lecteur AA après ajout du micro/TX ;
il ne prouve ni l'envoi continu sur le transport de projection ni la restitution
par l'autoradio. La prochaine cible d'investigation est cette frontière, sans
attribuer d'avance la coupure à un composant précis.

Le correctif de coexistence de focus porte sur MediaFocusControl du Redmi. Ce
n'est pas une preuve de priorité audio sur l'autoradio. Il serait injustifié de
continuer à élargir ce hook, de remplacer des retours de succès ou de fabriquer
une autorisation de capture. La 1.9.13 est un diagnostic causal, pas une réparation
acoustique validée.

## Éléments réellement lus

- Dernier rapport privé : mêmes trois appels et mêmes preuves que LAB-1.9.13.md.
  Les données brutes ne sont pas publiées.
- Code du module : CarProjectionHook, TelecomAudioHook, RxBridgeService,
  ProjectionFocusHook, CallDiagnostics, FirmwareService/Archive et scripts Magisk.
- Sources AOSP Android 11 récupérées localement par git, et non documentation
  d'Android Automotive utilisée comme spécification Android Auto :
  - frameworks/av : `e7e59490c070f00bb2f5e0273e4de482dd05c209` ;
  - hardware/libhardware : `ee904afe2232645d7f969a3c5de2aa3eb893ddfe` ;
  - packages/apps/Bluetooth : `45a2fb5c6f454b6ee54e9869c85034b8d07cb48e` ;
  - packages/services/Telecomm : `83b9d243e9a4ae7a3f2f6d416c1e29ec997666a7`.
- Noyau officiel Xiaomi, branche begonia-r-oss :
  `777ef5dfc3c93ae5e8f004bec4285230295760b5` ; modules Xiaomi :
  `cc4a152193b03cde6ecfcffcc317151aaa2b0cdd`.
  Une branche de sources publiée n'est pas une preuve que le noyau installé est
  construit exactement depuis ce commit/configuration.

## Nouveau recoupement : SCO côté autoradio avant le pont

Le premier bilan 1.9.13 ne recoupait pas cet intervalle avec la seconde de son
rapportée. Les appels 1 et 3 comportent un transport audio Headset (rôle AG du
Redmi vers le périphérique mains libres), distinct du transport HeadsetClient
vers le Samsung :

| Appel | Headset AudioOn | Demande disconnectAudio venant de Telecom | Différence | Début RX logiciel |
| --- | --- | --- | --- | --- |
| 1 | 22:07:06.852 | 22:07:07.603 / DISCONNECT_AUDIO à .604 | environ 0,75 s | 22:07:12.388 |
| 3 | 22:08:00.387 | 22:08:01.127 / DISCONNECT_AUDIO à .128 | environ 0,74 s | 22:08:03.800 |

Juste avant ces déconnexions, le module journalise
`vehicle_sco_request=suppressed route_request=earpiece`. AOSP permet de suivre
le changement de route jusqu'à setBluetoothOff/disconnectBluetoothAudio. Le
rapport montre ensuite l'appel à HeadsetService.disconnectAudio par Telecom,
puis la transition effective AudioOn → AudioDisconnecting → Connected.

Il s'agit d'une ouverture native brève **avant** le début RX/micro/TX du pont.
Elle constitue une autre origine possible du son bref entendu. L'identité
acoustique et l'instant exact décrit par l'utilisateur ne sont pas établis :
il serait faux d'affirmer que cette correspondance prouve la cause du symptôme.
Mais il est également injustifié de supposer que cette seconde provient déjà
de la piste navigation. Ce recoupement rend prioritaire la séparation de ces
deux chemins. Il ne justifie pas de supprimer aveuglément le hook, car cela
réintroduirait le conflit des deux transports constaté en 1.9.4.

## Ce que dit le code

### Le sous-mix est un conduit local

AudioPolicyManager.getOutputForAttrInt choisit la route explicite si présente,
sinon un mix de politique correspondant, sinon le moteur de politique.
AudioPolicyMixCollection sélectionne les mixes selon leurs critères. Le rapport
montre uniquement MEDIA/GAME et ASSISTANT/NAVIGATION_GUIDANCE pour les deux mixes
AA enregistrés. Aucun de ces critères n'est VOICE_COMMUNICATION. Changer
l'étiquette de la piste ne crée pas un chemin d'appel AA.

Dans le HAL remote_submix AOSP, out_write peut renvoyer le nombre d'octets demandé
alors que le pipe est arrêté et que le buffer est perdu. in_read peut aussi
retourner des octets nuls lorsque le pipe manque ou manque de données. Ces
possibilités expliquent les limites des anciens compteurs, mais ne prouvent pas
qu'elles se produisent sur ce téléphone. Les mesures AA non nulles du dernier
rapport sont une preuve plus avancée dans la chaîne que le seul compteur write.

### Deux SCO ne sont pas deux endpoints arbitrairement indépendants

HeadsetClientStateMachine Android 11 explique que le routage HF passe par les
paramètres HAL, avec un support incomplet dans les routes/volumes SCO habituels.
HeadsetClientService.isScoRouted vérifie les autres machines du même rôle HF ;
ce contrôle ne prouve pas à lui seul une interdiction matérielle de HF+AG.

Le pilote Xiaomi mtk-btcvsd.c expose, par instance, un état TX et un état RX,
un pointeur de sous-flux par direction et une bande NB/WB. mtk_pcm_btcvsd_open
réinitialise et affecte cet état partagé. Cela ne ressemble pas à une table
universelle de PCM adressés par périphérique Bluetooth. Ce constat et l'échec
1.9.4 rendent injustifiée une relance de la même architecture à deux SCO. Ils
ne suffisent pas à déclarer la puce physiquement incapable de deux liens ou à
inventer une nouvelle configuration de mixeur.

### Confidentialité

AudioPolicyService Android 11 classe REMOTE_SUBMIX comme source virtuelle et
l'autorise dans ce traitement de concurrence. La confidentialité du micro ne
peut donc pas être annoncée comme explication universelle. Les différences MIUI
et les traitements DSP restent à comparer aux fichiers réellement chargés.

### Angle mort des diagnostics

CallDiagnostics ne recueille qu'une liste limitée de tags d'appel/module, avec
un maximum de deux processus. Le rapport ne reconstitue pas la chaîne lecteur
AA → sélection de flux → décision de focus de projection → émission/arrêt/retours
transport. Il faudra identifier ces classes/points de journalisation dans
l'APK installée, mesurer des compteurs/métadonnées sans conserver la voix, puis
corréler leurs transitions avec celles déjà connues. Des tags supposés ou une
capture logcat vide ne doivent pas devenir des preuves d'absence d'émission.

## ROM et binaires

ROM exacte identifiée :
`miui_BEGONIAIDGlobal_V12.5.6.0.RGGIDXM_e5599068e6_11.0.zip`.
L'index ZIP distant et les métadonnées OTA ont été lus depuis le CDN Xiaomi.
Les métadonnées annoncent l'empreinte exacte fournie par le téléphone, SDK 30,
patch de sécurité 2022-04-01. vendor.transfer.list et audio_dsp.img ont été
extraits et leurs CRC ZIP vérifiés. La reconstruction de vendor est partielle : environ 100 Mio compressés récupérés
à ce stade, et aucun HAL exact extrait. Le transfert CDN est trop lent pour
que cette voie remplace la copie statique du téléphone. Les tables ext4 lisibles
confirment des répertoires, mais les extents nécessaires au HAL ne sont pas
encore disponibles. Je ne présente pas les fichiers absents comme analysés. Le MD5 de l'archive entière
n'a pas été vérifié ; les métadonnées seules ne constituent pas une validation
cryptographique de toute la ROM.

La ROM stock ne contient pas le patch Magisk actuellement chargé. La copie
chargée doit être identifiée par son hash et comparée à la référence d'origine.
Les scripts du dépôt attendent :

- HAL original : `6ca4f18a2652f5ecd0faa7e151f1637e4943da7bfe4256de72116b5c51b1b809` ;
- HAL validé modifié : `3387c3b7e9bc593728cd13c20e911007befb8fa25ea1dc8338645a487ec09ddc`.

Trois HAL publics voisins ont été téléchargés en reconnaissance. Leurs hashes
ne correspondent pas à cette référence ; ils sont exclus des conclusions sur
la ROM utilisateur et ne doivent jamais servir de payload de remplacement.

## Android Auto : prudence indispensable

Il n'y a pas de copie de l'APK AA installée dans la pièce jointe. Les sources
AOSP, Android Automotive, SDK de voitures et implémentations communautaires
ne sont pas le code de cette application précise.

Les dépôts aasdk et open-android-auto ont été consultés comme pistes. La page
audio du second marque plusieurs enums comme non vérifiées et des messages
comme retirés ; elle décrit un canal d'appel projeté tandis que sa page Bluetooth
décrit les appels par HFP. Cette contradiction interdit de transformer leur
table de canaux en contrat Google. Il faut récupérer l'APK exacte, sa version,
ses splits et son hash avant de sélectionner une cible de hook/désassemblage.

## Suite ciblée

1. Obtenir une copie statique de l'AA installée, du HAL chargé et du code système
   MIUI pertinent. Cela ne demande ni nouvel appel, ni nouveau reboot, ni flash.
2. Terminer la comparaison stock/patch natif et lire les décisions de routage,
   sans modifier les voies D5 confirmées.
3. Décompiler AA, suivre ses AudioRecord REMOTE_SUBMIX jusqu'aux producteurs de
   flux et à la gestion du focus/transport. Les noms obfusqués seront vérifiés
   par signatures et graphe d'appels, pas copiés depuis une autre version.
4. Instrumenter la première frontière manquante à partir de ce graphe. Une
   lecture non nulle suivie d'un arrêt d'émission départagerait AA et l'autoradio ;
   une émission/confirmation continue demanderait ensuite des éléments côté
   récepteur. Une confirmation transport ne prouverait toujours pas l'acoustique.
5. Choisir un correctif seulement après cette localisation. Si l'architecture
   de la voiture exige réellement un chemin d'appel différent, l'annoncer plutôt
   que multiplier les variantes navigation/focus.

## Sources vérifiables

- [AudioPolicyManager Android 11](https://android.googlesource.com/platform/frameworks/av/+/e7e59490c070f00bb2f5e0273e4de482dd05c209/services/audiopolicy/managerdefault/AudioPolicyManager.cpp)
- [AudioPolicyMix Android 11](https://android.googlesource.com/platform/frameworks/av/+/e7e59490c070f00bb2f5e0273e4de482dd05c209/services/audiopolicy/common/managerdefinitions/src/AudioPolicyMix.cpp)
- [remote_submix Android 11](https://android.googlesource.com/platform/hardware/libhardware/+/ee904afe2232645d7f969a3c5de2aa3eb893ddfe/modules/audio_remote_submix/audio_hw.cpp)
- [HeadsetClientStateMachine Android 11](https://android.googlesource.com/platform/packages/apps/Bluetooth/+/45a2fb5c6f454b6ee54e9869c85034b8d07cb48e/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java)
- [Routage Telecom Android 11](https://android.googlesource.com/platform/packages/services/Telecomm/+/83b9d243e9a4ae7a3f2f6d416c1e29ec997666a7/src/com/android/server/telecom/CallAudioRouteStateMachine.java)
- [Pilote Xiaomi begonia](https://github.com/MiCode/Xiaomi_Kernel_OpenSource/blob/777ef5dfc3c93ae5e8f004bec4285230295760b5/sound/soc/mediatek/common/mtk-btcvsd.c)
- [ROM CDN Xiaomi](https://bigota.d.miui.com/V12.5.6.0.RGGIDXM/miui_BEGONIAIDGlobal_V12.5.6.0.RGGIDXM_e5599068e6_11.0.zip)
- [Index de version ROM, source secondaire](https://xmfirmwareupdater.com/miui/begonia/stable/V12.5.6.0.RGGIDXM/)
- [Documentation communautaire audio, non spécification](https://github.com/mrmees/open-android-auto/blob/main/docs/channels/audio.md)
- [Documentation communautaire Bluetooth, non spécification](https://github.com/mrmees/open-android-auto/blob/main/docs/channels/bluetooth.md)
