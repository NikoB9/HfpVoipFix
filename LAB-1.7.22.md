# HfpVoipLab 1.7.22 : candidat duplex avec XML original

Le rapport sans module confirme D2 avec signal et D4 : phase 1 RX, phase 2 entrée RX détournée sur le micro, phase 3 TX seul, phase 4 RX toujours détournée. Les quatre phases se terminent. D2/D3 restent validés indépendamment : aucun nouveau test de référence demandé.

D4 utilise VOICE_COMMUNICATION sans setPreferredDevice sur le micro. Le firmware expose déjà voip_tx, distinct de primary input. La sélection effective sera vérifiée dans le rapport ; la source demandée ne prouve pas à elle seule un chemin matériel indépendant.

Le candidat Magisk privé 1.7.22 ne remplace que le HAL 32 bits. Il conserve le XML et routingInputDevice d'origine, modifiés dans le candidat retiré 1.7.21. Deux modifications restent : empêcher createCaptureHandler de rouvrir la capture opposée pour la paire SCO/micro interne et soustraire les sorties SCO simples à syncSharedOutDevice. Il conserve les contrôles ROM, empreintes, architecture et conflits de modules. Il se désactive pour le démarrage suivant, sans retirer le montage courant.

Les erreurs mSBC du précédent essai ne suffisent pas à attribuer sa réception nulle à un codec incorrect. La cause exacte de cette régression reste non confirmée. Cette révision élimine deux changements de cette initialisation ; elle n'annonce pas un duplex acquis.

Installer APK et candidat Magisk, puis un seul redémarrage. Seul essai demandé : D4 pendant 65 secondes, correspondant parlant sur les quatre phases et confirmation du micro en phase 3. Raccrocher et exporter. En phase 4 la transmission micro est volontairement arrêtée, la capture micro reste ouverte. Un autre redémarrage restaure le HAL original.

Validation : tests hôtes Java et ARM/ELF, contrôles installateur et signature APK. Aucun de ces contrôles ne valide le duplex matériel. Aucun binaire propriétaire, empreinte ROM ou rapport brut publié ici.
