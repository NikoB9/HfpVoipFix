# D4 — résultats du test 1.7.20 et prochaine investigation

Le test utilisateur confirme le son en phase 1, le microphone transmis en phase 3, puis aucun des deux en phase 4. Le son en phase 2 reste incertain. Ces observations auditives sont distinctes des métadonnées de routage.

## Deux conflits observés

| Phase | Métadonnées et événements du HAL | Observation utilisateur |
| --- | --- | --- |
| 1 : RX seul | Capture SCO, sortie haut-parleur, échantillons RX transmis | Son OK |
| 2 : ajout de la capture MIC | Le flux RX bascule vers le microphone intégré ; le HAL ferme la capture Bluetooth. Le garde de routage rejette les échantillons RX | Son incertain |
| 3 : ajout de la sortie TX SCO | MIC vers SCO actif ; le HAL fait suivre la sortie primaire vers SCO et ferme le chemin haut-parleur. RX reste sur MIC | Micro OK, plus de son |
| 4 : retrait de la sortie TX, MIC conservé | Le chemin haut-parleur rouvre, mais RX reste sur MIC. Le garde continue de rejeter RX ; TX est volontairement arrêté | Aucun des deux |

Le journal direct préserve les quatre phases. L'absence de son en phase 4 ne démontre donc pas un échec de réouverture du haut-parleur.

La configuration fournie déclare une entrée primaire limitée à un flux ouvert et actif. Dans cet essai, les deux captures partagent effectivement cette entrée. Elle déclare également des profils voip_tx et fast input ; leur existence ne démontre ni leur sélection par les API Java, ni leur indépendance physique dans le HAL. Les préférences de périphérique et les succès AudioRecord/AudioTrack ne prouvent pas deux chemins matériels indépendants.

## Prochaine étape nécessaire

Examiner le HAL audio primaire exact et ses dépendances : sélection des profils de capture, conditions de partage de sortie et possibilités de chemins simultanés SCO / microphone / haut-parleur. Identifier une condition ou un point de modification vérifiable avant une nouvelle expérimentation. Une réorganisation des mêmes ouvertures ne constitue pas une correction des deux conflits constatés.

Le rapport de test disponible contient les journaux et XML, mais aucun binaire ELF du HAL. Les bibliothèques d'une ancienne conversation ne sont pas disponibles dans cet espace. La version 1.7.20 contient déjà la collecte statique nécessaire :

1. Hors appel, terminer la capture.
2. Ouvrir « Outils avancés · HCI, firmware et autres modes ».
3. Choisir « Collecter les fichiers audio système » puis « Collecter ».
4. Attendre « Firmware prêt », puis « Partager le firmware dans ChatGPT ».
5. Joindre HFP-Firmware.zip dans la conversation de diagnostic.

Cette collecte lit les bibliothèques et configurations sans modifier le système. L'archive contient les empreintes SHA-256 et les omissions ; elle reste hors du dépôt public. Aucun nouvel APK correctif n'est annoncé tant qu'aucun changement ciblé n'est identifié. Les configurations D2 et D3 validées séparément sont conservées.
