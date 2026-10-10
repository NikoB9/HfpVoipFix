# HfpVoipLab 1.7.2 — alignement MediaTek WBS dans D2

## Diagnostic retenu

Les journaux d'essai montrent qu'une liaison synchrone peut être établie alors que le chemin de réception reste silencieux. Le format demandé à l'application et le format observé à l'ouverture RX ne concordaient pas ; les compteurs d'interruption restaient inactifs. Les traces brutes et identifiants propres au téléphone ne sont pas publiés.

Une analyse locale a indiqué qu'un réglage propre à la plateforme pouvait modifier la bande du chemin audio. Les détails internes de l'implémentation et les empreintes des fichiers système sont omis de cette version publique. L'essai de cette piste était limité à RX WBS ; la capture disponible ne permettait pas de localiser la cause du silence.

## Changement en 1.7.2

Lorsque D2 reçoit l'indication WBS, le hook demande le réglage de plateforme avant de poursuivre la route SCO et de créer le pont RX. Si Android négocie la bande étroite, D2 ne modifie pas le mode. Si l'état est illisible ou si la commande échoue, le hook n'expose pas le port d'entrée au pont : D2 échoue fermé.

Après la désactivation de la route SCO, le hook restaure le réglage. Le rapport journalise les opérations et leurs codes retour. Si la restauration échoue, l'état reste marqué comme sale et les interventions suivantes sont refusées jusqu'au nettoyage.

Le pont reste RX uniquement ; aucun micro n'est envoyé, aucun AudioPatch n'est créé et aucune renégociation du codec Bluetooth n'est demandée. Le changement n'établit pas encore que la commande fera arriver des interruptions BTCVSD sur le téléphone.

## Vérification

- 59 assertions des hooks, y compris WB uniquement si WBS est négocié, restauration à `off`, absence d'écriture en bande étroite et refus d'ouvrir D2 si la commande WB échoue ou lève une exception.
- Tests de protocole d'appel, décodeur HCI, canal de statut, archive de rapport et garde RX passés.
- APK `com.hfpvoipfix`, versionCode 37 / `1.7.2-lab`, vérifiée alignée et signée v3 avec le même certificat que la version précédente.
- Pas d'appareil connecté ici : le retour réel du chemin audio et du son reste à vérifier sur le téléphone.

## Essai ciblé

1. Installer `HfpVoipLab-1.7.2.apk` puis redémarrer une fois pour charger le nouveau hook Bluetooth.
2. Dans l'application, confirmer que le module chargé affiche 1.7.2. Hors appel, sélectionner D2, démarrer la capture et armer le pont.
3. Lancer l'appel de test depuis l'appareil local via le HFP distant, puis raccrocher.
4. Écouter la réception et terminer la capture. Partager `HFP-Rapport.zip`.

Pas besoin de réactiver HCI snoop, de collecter d'autres fichiers système, ni de refaire les anciens modes. Dans le rapport, vérifier que le réglage est actif, que les interruptions progressent et que les échantillons D2 contiennent des valeurs non nulles. Un code retour sans erreur, à lui seul, ne prouve pas que le chemin audio a été activé.
