# HfpVoipLab 1.7.8 — sélection du micro Android par défaut

## Diagnostic précédent

Le rapport du téléphone montre que les deux appels D3 ont atteint le mode SCO, mais `RxBridgeService` a fermé le pont avant l'ouverture de `AudioRecord` : Android exposait plusieurs entrées de type micro intégré. Aucun échantillon micro n'a donc été transmis. D2 reste le mode de réception et ne teste pas le micro.

## Changement

D3 laisse maintenant la politique audio Android choisir le micro pour la source `MIC`. Le pont vérifie ensuite que l'entrée réellement routée est un micro intégré et que la route micro → SCO reste stable avant d'envoyer des échantillons. Le rapport indique les candidats et le micro sélectionné (ID, adresse et nom produit), sans enregistrer le PCM.

## Essai D3

Après installation et redémarrage LSPosed, appliquer D3 hors appel, démarrer la collecte, armer le pont et attendre « D3 micro armé ». Lancer un appel sortant et parler normalement. Vérifier avec le correspondant si la voix est claire ; raccrocher après quelques secondes si aucun son ne passe. Exporter le rapport pour voir les événements `MIC_CANDIDATE`, `MIC_SELECTED`, `ROUTE_CONFIRMED` et `STATS direction=tx`.

Cette modification corrige le refus dû aux micros multiples. Le résultat sonore reste à confirmer sur le téléphone ; la route SCO du correspondant et les contrôles de confidentialité Android peuvent encore empêcher la capture.
