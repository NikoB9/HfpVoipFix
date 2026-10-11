# 1.8.1 — stabilisation de l’application et diagnostic allégé

Le retour utilisateur sur 1.8.0 rapporte un son correct quand le pont fonctionne, mais une interface réinitialisée en paysage, des coupures, une forte consommation et des ralentissements. Il rapporte aussi un essai D5 absent du rapport. Le rapport ne contient que trois appels soft_duplex ; cela ne permet pas de nier la tentative D5 ni d’attribuer une cause précise à la décharge.

## Corrections

- Rotation gérée sans recréer ModeActivity ; les sélecteurs sont également persistés et le numéro restauré en cas de recréation.
- Sélection et parcours effectivement demandé au module affichés séparément. Sélections, demandes d’application, confirmations, erreurs UI et arrêt manuel journalisés pour distinguer D5 sélectionné, appliqué ou refusé.
- Toutes les demandes ProbeChannel sont sérialisées : plusieurs canaux ne peuvent plus écraser simultanément le jeton unique de debug.hfpvoipfix.probe. Vérification de fraîcheur maintenue ; aucune audio autorisée avec un état périmé.
- D4/D5 continus gardent le service et sa sonde entre les appels. Chaque nouveau SCO reçoit de nouveaux objets audio ; les anciennes écritures sont bloquées par l’identité de session. Un SCO terminé ne peut pas être réouvert à partir d’un statut transitoire portant la même identité.
- Capture limitée aux événements HfpVoipFix/HfpLabCapture. Plus de dumpsys automatiques à chaque phase ou changement de route. Instantané manuel et finalisation hors audio conservés.
- Sonde du pont toutes les secondes au lieu d’une boucle de 250 ms, statistiques PCM toutes les deux secondes, notification seulement si son texte change. Boucle non bloquante audio espacée à 10 ms.

## Limites et validation

Le patch natif et les chemins audio restent ceux de 1.7.22. Aucun nouveau module Magisk. La consommation réelle, la rotation MIUI, les appels successifs et D5 restent à mesurer sur le Redmi ; cette version ne les déclare pas validés. Les tests hôtes et la compilation CI sont nécessaires mais ne remplacent pas cette validation.

Installer l’APK, vérifier LSPosed, réactiver le module Magisk 1.7.22 avant de redémarrer. Premier essai : D4, un appel court en tournant l’écran, raccrocher puis un second appel. Exporter ensuite le rapport. D5 reste expérimental : sélectionner puis appliquer hors appel, attendre D5 prêt, utiliser le compte Samsung/HFP dans Téléphone. Pas de validation Android Auto dans cette version.
