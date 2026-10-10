# HfpVoipLab 1.7.3 — MediaTek WBS après la route native

## Constat du rapport 1.7.2

Le rapport d'essai confirme la négociation large bande et l'application de la commande expérimentale, mais le chemin RX reste dans le mode précédent : les interruptions ne progressent pas et les échantillons reçus sont nuls. Les données précises du rapport privé ne sont pas reprises dans cette note publique.

Le code 1.7.2 envoyait le réglage avant le retour de la route HFP native. Le rapport ne montre pas directement ce qui remet ou maintient le mode précédent ; le moment de l’écriture est la variable testée ici.

## Changement en 1.7.3

Avec WBS négocié, le hook marque le réglage comme en attente, expose uniquement l'entrée SCO, laisse la route HFP native se terminer, puis applique le réglage de plateforme dans le hook de retour. Si la demande échoue ou lève une exception, D2 ferme l'intervention et retire son port. À la fin du SCO, le hook restaure le réglage.

Les événements de diagnostic rendent l'ordre vérifiable. Un code de retour sans erreur ne démontre toujours pas à lui seul le mode réellement actif : vérifier l'état RX, les interruptions et les statistiques audio.

## Vérification locale

- 60 assertions du hook, dont l'absence d'écriture du réglage avant le retour de la route native, la fermeture en cas d'échec, la restauration et les garde-fous D2.
- Tests protocole d’appel, métadonnées HCI, canal de statut, archive de rapport et garde RX passés.
- APK versionCode 38 / `1.7.3-lab`, APK Signature Scheme v3, alignement vérifié, même certificat que 1.7.2.
- Pas d’appareil connecté à cet environnement : l’ordre réel entre le hook de retour et l’ouverture de capture reste à confirmer sur le téléphone.

## Essai ciblé

Installer 1.7.3, redémarrer une fois pour charger le hook LSPosed, sélectionner D2 et armer le pont comme précédemment. Faire un seul appel et partager le rapport. Aucune trace HCI ni collecte firmware supplémentaire n’est nécessaire pour cette vérification.
