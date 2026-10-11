# HfpVoipLab 1.7.22 : premier essai duplex concluant

## Résultat observé le 11 octobre 2026

Le testeur rapporte : son en phases 1 et 2, son et micro en phase 3, son en phase 4. Le rapport de cette exécution confirme :

- APK 1.7.22 et marqueur du candidat natif 1.7.22 présents ; empreinte du HAL correspondant au candidat, XML d'origine.
- Route réception SCO vers haut-parleur maintenue aux quatre phases ; microphone interne confirmé à partir de la phase 2 et sortie SCO de transmission confirmée en phase 3.
- Signal de réception mesuré aux quatre phases. En phase 3, 241 153 échantillons RX et 239 211 échantillons TX écrits pendant environ 15 secondes, avec signal non nul dans les deux directions.
- Les quatre phases de 15 secondes se terminent et le pont se ferme normalement ; le journal d'erreurs de l'application est vide.
- La capture SCO reste ouverte lorsque le micro est ajouté ; l'ouverture de TX SCO est exclue du routage de sortie partagé. La fermeture de la capture SCO intervient au nettoyage de fin de séquence.

Cela valide un premier duplex court sur l'appareil du testeur, en recoupant son écoute et les mesures du rapport. Les journaux locaux seuls ne prouvent pas ce qu'entend le téléphone distant. Aucune validation d'appel long, de reconnexions répétées, d'écho ou de qualité vocale n'est encore établie. Le candidat reste limité à un démarrage : redémarrer restaure le HAL d'origine.

Conserver cette révision comme référence fonctionnelle. La prochaine vérification utile porte sur un duplex prolongé et sa stabilité ; D2/D3 ne nécessitent pas une nouvelle validation.

## Configuration de cet essai

Le rapport sans module confirme D2 avec signal et D4 : phase 1 RX, phase 2 entrée RX détournée sur le micro, phase 3 TX seul, phase 4 RX toujours détournée. Les quatre phases se terminent. D2/D3 restent validés indépendamment : aucun nouveau test de référence demandé.

D4 utilise VOICE_COMMUNICATION sans setPreferredDevice sur le micro. Le firmware expose déjà voip_tx, distinct de primary input. Le rapport du 11 octobre confirme une capture micro avec source 7 et flag VOIP_TX, distincte de la capture SCO source 6 sans flag. La source demandée seule ne suffit pas à établir ce résultat.

Le candidat Magisk privé 1.7.22 ne remplace que le HAL 32 bits. Il conserve le XML et routingInputDevice d'origine, modifiés dans le candidat retiré 1.7.21. Deux modifications restent : empêcher createCaptureHandler de rouvrir la capture opposée pour la paire SCO/micro interne et soustraire les sorties SCO simples à syncSharedOutDevice. Il conserve les contrôles ROM, empreintes, architecture et conflits de modules. Il se désactive pour le démarrage suivant, sans retirer le montage courant.

Les erreurs mSBC du précédent essai ne suffisent pas à attribuer sa réception nulle à un codec incorrect. La cause exacte de cette régression reste non confirmée. Cette révision élimine deux changements de cette initialisation ; elle ne permettait pas, avant l'essai, d'annoncer un duplex acquis.

Installer APK et candidat Magisk, puis un seul redémarrage. Seul essai demandé : D4 pendant 65 secondes, correspondant parlant sur les quatre phases et confirmation du micro en phase 3. Raccrocher et exporter. En phase 4 la transmission micro est volontairement arrêtée, la capture micro reste ouverte. Un autre redémarrage restaure le HAL original.

Validation : tests hôtes Java et ARM/ELF, contrôles installateur et signature APK. Aucun de ces contrôles ne valide le duplex matériel. Aucun binaire propriétaire, empreinte ROM ou rapport brut publié ici.
