# 1.9.1 — première intégration Android Auto

Le rapport filaire du 12 octobre confirme que D5 reste stable pendant une projection Android Auto : le pont transporte les deux flux pendant 21 secondes sans erreur. Il confirme aussi que la réception était forcée vers le haut-parleur du Redmi et que la capture micro restait sur son micro intégré. Android Auto avait pourtant créé deux mixes de projection `REMOTE_SUBMIX`, dont un mix 48 kHz qui accepte `USAGE_MEDIA`.

## Routage projeté

Pendant D5, l’application détecte une projection active uniquement si le processus Android Auto et au moins une sortie `REMOTE_SUBMIX` sont présents. Elle conserve `USAGE_MEDIA`, mais cesse alors d’imposer le haut-parleur du Redmi. AudioPolicy peut donc envoyer la réception au mix média de projection. Le pont exige ensuite que la route effective soit `REMOTE_SUBMIX` avant de transmettre les échantillons. Hors projection, il conserve strictement la route validée vers le haut-parleur.

La détection ne teste ni USB ni Wi-Fi. Les deux modes Android Auto passent par la même couche de projection côté téléphone et restent des cibles de validation distinctes.

## Appels lancés depuis l’autoradio

Lorsque D5 est activé, le hook Bluetooth consulte les comptes d’appel actifs. S’il trouve exactement un compte appartenant au `HfpClientConnectionService` natif, il le choisit comme compte sortant par défaut via Telecom. Il refuse zéro, plusieurs comptes HFP, tout autre package et toute autre `ConnectionService`. Cette sélection vise l’échec « réseau non disponible » observé lorsque l’autoradio tentait d’utiliser la téléphonie locale sans SIM du Redmi.

## Diagnostic suivant

Le rapport ajoute l’état Telecom, la liste des périphériques audio de projection et les événements de création d’appel utiles. Cela doit permettre d’identifier le chemin du micro de l’autoradio. La 1.9.1 ne force pas un `REMOTE_SUBMIX` en entrée : les mixes vus dans le rapport sont les captures de lecture utilisées par Android Auto, pas une preuve d’un microphone de voiture accessible à l’application.

Le module natif Magisk reste en 1.9.0 : son binaire et son XML ne changent pas. L’APK/LSPosed passe à 1.9.1 et nécessite un redémarrage pour charger les nouveaux hooks.
