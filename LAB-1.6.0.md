# HfpVoipLab 1.6.0 — essai RX logiciel D2

## Pourquoi
Les essais précédents n'ont pas établi que les paramètres audio génériques ouvraient le pont HFP nécessaire. D/D1 n'ont donc pas validé le transport. Cela ne prouve pas l'impossibilité matérielle ; une synthèse publique des constats est conservée dans ANALYSE-HAL-2026-10-09.md.

D2 contourne le pont HFP vers Telecom comme D, expose uniquement le port d'entrée SCO et fournit l'état de la session au service de l'application. Il n'ajoute aucun AudioPatch, aucune sortie SCO, ne demande aucun changement de mode audio et n'écrit aucun paramètre codec. Les commandes natives HFP restent celles d'Android.

Le service de premier plan ouvre AudioRecord (VOICE_RECOGNITION, PCM16 mono à la fréquence mAudioWbs rapportée) et AudioTrack (SPEECH/MEDIA, haut-parleur). API publiques Android 11. La préférence de périphérique n'est jamais considérée comme preuve : vérification getRoutedDevice avant ET après lecture, pas de restitution avant correspondance SCO en entrée / haut-parleur en sortie. Les données de démarrage sont jetées. Aucun son n'est enregistré dans un fichier. Aucun pont vers le microphone ou l'appareil source.

Mesures SOFT_RX : nombre d'échantillons lus/écrits/jetés, non-zéros, RMS, crête, underruns, route, erreurs de lecture/écriture et capture rendue silencieuse par Android. Les niveaux ne prouvent pas la présence de voix intelligible. La fréquence demandée ne prouve pas la fréquence matérielle ni le codec sur le fil.

## Essai
1. Mettre à jour avec l'APK 1.6.0 signée du même certificat. Conserver le scope Bluetooth recommandé. Redémarrer une fois pour charger les hooks 1.6.0.
2. Ouvrir Lab, vérifier root/module. Hors appel, autoriser les stratégies expérimentales, choisir D2 et appliquer.
3. Démarrer la capture et attendre sa confirmation.
4. Armer D2, accorder la permission audio (Android l'appelle microphone) si demandée puis appuyer de nouveau. Attendre « D2 armé ».
5. Appeler un numéro de test depuis l'appareil source, destination Bluetooth. Garder le volume multimédia modéré ; gain du pont fixé à 0,35. Écouter la réception puis raccrocher. Microphone : Non évalué.
6. Attendre l'arrêt D2, noter la réception, terminer la capture puis partager HFP-Rapport.zip.

Arrêt manuel dans l'écran ou notification. Un seul appel par armement ; armement 120 s maximum, flux 180 s maximum. Arrêt sur fin/changement de SCO, déconnexion, perte de routage, état Bluetooth périmé (4 s), erreur, absence de données pendant 8 s ou mode téléphonique local. Le suivi SCO est interrogé : l'arrêt sur raccrochage dépend de la prochaine réponse, et n'est pas un callback temps réel. Pas de redémarrage entre essais ultérieurs. Si D2 échoue, fournir le rapport plutôt que répéter les autres modes.

## Limites et validation
Pas de téléphone/émulateur utilisable dans l'environnement de build. Aucun succès du transport revendiqué à ce stade. L'ouverture d'AudioRecord sollicite le HAL par les API normales Android et reste expérimentale. Pas d'accès PCM direct, tinymix, /proc/asound ni modification vendor.
Tests sur hôte : 48 assertions hooks dont D2 (bypass, entrée seule, pas de patch/codec/mode, latched, nettoyage), garde de session RX, export ZIP. Signature, manifeste, DEX, assets LSPosed et alignement vérifiés. Les tests sur hôte ne remplacent pas un test Android.

Références API vérifiées :
https://developer.android.com/reference/android/media/AudioRecord
https://developer.android.com/reference/android/media/AudioRouting
https://developer.android.com/about/versions/11/privacy/foreground-services
