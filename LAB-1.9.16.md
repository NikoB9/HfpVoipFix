# HfpVoipLab 1.9.16 — projection sans connexion Telecom locale

VersionCode 79, Android 11/API 30, runtime Magisk 1.9.0-persistent conservé.

## Changement de fonctionnement

Le nouveau choix `projection_only` utilise le contournement existant de
`HfpClientDeviceBlock.handleCall` avec la sortie Android Auto du D6. Il évite
la création d'une connexion HFP dans Telecom sur le Redmi. Il ne supprime pas
une connexion déjà créée : le contournement refuse de s'appliquer dans ce cas.
Les états HFP du Samsung restent réels et ne sont pas modifiés.

Cela retire la publication de cet appel local par Telecom vers ses clients.
Ce n'est ni une demande de priorité supplémentaire, ni une falsification des
réponses de focus de l'autoradio. Cela ne garantit pas que le véhicule acceptera
la projection : cette propriété nécessite un essai matériel.

**Compromis : appeler, répondre et raccrocher sur le Samsung.** L'appel de ce
parcours n'est pas fourni aux interfaces d'appel Telecom du Redmi ou d'AA.
Le bouton principal devient « Activer AA isolé · appels sur Samsung ».
Le D6 avec Telecom reste accessible dans les diagnostics facultatifs, dans la
même APK. D5 conserve son parcours et ses règles.

## Conditions et nettoyage

- Un seul appel HFP réellement actif, un SCO maître, ports réellement préparés,
  contournement confirmé, statut frais et version correspondante.
- Le pont isolé exige MODE_NORMAL. Un mode audio différent refuse son démarrage.
- Taux 8000/16000 suivant la négociation réelle, sans renégociation du codec.
- Récupération SCO bornée existante, avec confirmation du contournement au lieu
  d'une connexion Telecom pour ce parcours seulement.
- Preuve native de cinq secondes, vérification système des adresses des ports,
  restauration HAL et retrait des ports possédés restent applicables.
- Le proxy de disponibilité SIM reste limité à l'ancien `telecom_aa` ; aucune
  disponibilité SIM artificielle dans `projection_only`.
- Pas de changement de politique de connexion Bluetooth persistante, de
  désappairage, de second SCO, de faux focus accordé ou de faux état d'appel.
- Les collectes continuent d'être enregistrées dans Téléchargements.

## Validation requise

Les fixtures hôte vérifient le chemin de contournement de production, son refus
avec connexion Telecom préexistante, la préparation/nettoyage des ports aux deux
taux, la récupération maître avec preuve de contournement, les preuves natives
et les exclusions. Ces tests ne remplacent pas un essai Redmi/autoradio.

Après installation et redémarrage des hooks, garder Android Auto en USB,
les profils Bluetooth habituels, sélectionner AA isolé hors appel, attendre le
relais prêt, démarrer une capture puis appeler depuis le Samsung durant 30 s.
Raccrocher sur le Samsung et enregistrer le rapport dans Téléchargements.
Le microphone utilisé reste celui du Redmi ; le microphone voiture n'est pas
implémenté. Ni l'audibilité ni le duplex voiture ne sont déclarés validés.

## Clarification des identifiants AA

Les identifiants du capturer et de l'endpoint transport sont deux espaces
distincts. Dans le binaire AA audité, `irr.b` utilise `trh.bg` : le capturer
TTS 5 correspond à l'endpoint GUIDANCE 1, conservé dans `jdz.e`. Un numéro 1
journalisé depuis `jdz.e` ne désigne donc pas le capturer SYSTEM. Cette
clarification remplace cette interprétation dans les notes antérieures.
