# 1.9.3 — D5 local et D6 Android Auto séparés

## Choix utilisateur

La page simple propose Activer le relais (D5), Activer le relais AA (D6), Désactiver et Options avancées et rapport. Le choix persiste après redémarrage. Un changement pendant un appel ou un nettoyage est refusé ; hors appel le pont se ferme puis le superviseur prépare le nouveau choix. Les options avancées exposent seulement D5/D6, sans les anciens essais D2/D3/D4 ni les boutons HFP directs.

## D5

Le fichier RxBridgeService a été reconstruit depuis le commit 6b4bd98913d4faef044737983828214eaeaac2b0 (1.9.0), puis étendu par des branches D6 explicites. D5 conserve la sélection unique des ports SCO, la sortie haut-parleur, le microphone VOICE_COMMUNICATION par défaut, les gains, la progression initiale et les contrôles de route de 1.9.0. La sélection automatique du compte sortant et les demandes connectAudio de 1.9.1/1.9.2 ne s’exécutent plus en D5. Le code commun de diagnostic/version/UI a changé ; cela ne constitue pas une garantie matérielle d’absence de régression. Un compte HFP déjà choisi reste une configuration système existante.

## D6

Les comptes et demandes audio expérimentales sont réservés à telecom_aa. Le probe publie les IDs des ports SCO maître trouvés par type interne et adresse dans le processus Bluetooth. L’application choisit ces IDs et ne confond plus le maître avec un deuxième appareil Bluetooth. Elle identifie ensuite une unique paire véhicule avec une adresse commune distincte du maître. Elle refuse les ambiguïtés, identités communes et paires incomplètes plutôt que choisir arbitrairement.

Si une paire véhicule distincte est exposée, D6 essaye SCO maître -> SCO véhicule et SCO véhicule -> SCO maître. Cette tentative peut être refusée ou reroutée par le HAL MediaTek ; les routes effectives restent contrôlées avant tout transfert.

À défaut de paire véhicule, un mix de projection dynamique identifie la sortie projetée. Le critère ne dépend plus du PID principal Android Auto, qui était absent malgré la projection sur les rapports précédents. Le format d’adresse ap/mixp prouve un mix de policy dynamique, pas à lui seul le propriétaire Android Auto : D6 est explicitement sélectionné par l’utilisateur. Ce repli utilise le micro local du Redmi et journalise mic_local=true ; aucun micro voiture n’est promis par la présence d’un mix de lecture.

USB et sans fil empruntent la même sélection d’endpoints. Le runtime Magisk 1.9.0, le binaire et le XML restent identiques. La possibilité de deux canaux SCO simultanés et l’erreur réseau affichée par l’autoradio restent à valider/diagnostiquer sur le matériel. Cette version ne simule pas un réseau mobile disponible.

## Vérification

Tests hôtes sur le code de sélection : IDs maître, paire voiture, identités/ambiguïtés/périphériques manquants, projection dynamique et séparation D5/D6. Gardes Telecom étendues à D6 sans modifier les exclusions SIM/appels multiples/urgence. CI compile et signe l’APK Android 11. Les rapports natifs/Telecom sont privés ; aucun binaire vendor ajouté au dépôt.
