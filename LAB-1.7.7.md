# HfpVoipLab 1.7.7 — essai séparé du microphone

## Résultat précédent

Le rapport fourni après la 1.7.6 confirme `btscoOn 1`, un flux RX non nul et une réception claire à l’écoute. La restitution D2 est considérée validée par l’utilisateur. Aucun événement ni compteur du rapport ne prouve que le microphone TX a été testé.

## Changement

D3 ajoute un chemin émission seule : microphone intégré de l'appareil local → AudioTrack `VOICE_COMMUNICATION` → sortie SCO de l'appareil distant. L’application vérifie les périphériques réellement routés avant de transmettre les échantillons. Elle ne lit pas de PCM vers un fichier et n’enregistre pas l’appel. Le niveau local n’est pas rejoué sur le haut-parleur.

D2 demeure la réception validée. D2 et D3 restent des modes séparés ; une seule direction est ouverte par essai. Le service au premier plan démarre après action explicite, affiche une notification persistante avec arrêt immédiat, et s’arrête à la fin du SCO, après trois minutes, ou en cas de perte de route, de données, de permission ou d’état Bluetooth.

## Essai D3 sur le téléphone

1. Installer la version 1.7.7 par-dessus la version précédente, ouvrir LSPosed et redémarrer pour charger le hook.
2. Dans le lab, autoriser les stratégies expérimentales, sélectionner « D3 · Essai micro logiciel » et appliquer hors appel.
3. Démarrer la capture, puis armer le pont et attendre « D3 micro armé ».
4. Appeler un service de test depuis l'appareil local et parler. Demander à une personne distante si la voix est claire. Si l’essai est silencieux ou déformé, raccrocher et exporter le rapport ; ne pas changer plusieurs modes.
5. En cas de fonctionnement, raccrocher. Vérifier l’arrêt automatique, puis noter le résultat séparément pour la réception et le microphone.

Le micro Android doit être autorisé pour l’application. Le root ne supprime pas les contrôles d’accès audio du système. Le démarrage est visible et l’arrêt reste disponible dans la notification.

## Application Téléphone

Le mode « 0 · Appels natifs » laisse Telecom gérer l’interface du téléphone. Les essais D2/D3 contournent volontairement l’enregistrement HFP dans Telecom, donc leurs commandes restent dans le lab. Compatibilité de l’interface native et audio D2/D3 piloté par le composeur Android : non vérifiée. C’est le chantier suivant après validation TX ; il ne faut pas annoncer le parcours Téléphone autonome avant l’essai sur l'appareil cible.

## Validation locale

- La garde de session accepte seulement D2/D3 quand le mode demandé correspond au mode appliqué, au SCO actif, à la route ajoutée et aux hooks présents.
- Les tests de hook vérifient que D3 n’ajoute que la sortie SCO, n’ajoute pas d’AudioPatch et restaure les états MediaTek au raccrochage.
- Les tests hôte ne remplacent pas l’essai réel d'un correspondant ni une validation sur la ROM cible.
