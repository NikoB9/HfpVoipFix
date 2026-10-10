# HfpVoipLab 1.5.0 — synthèse des essais

Cette version regroupe plusieurs expériences de routage audio HFP sur une configuration Android ancienne. Les notes ci-dessous décrivent les catégories de tests et leurs limites ; les identifiants de commit, détails propres à l'appareil et traces brutes ont été retirés.

## Objectif

Évaluer séparément la réception et l'émission audio entre deux appareils reliés par Bluetooth. Les appels sont courts et les résultats sont consignés par direction. Le mode d'observation ne modifie aucun réglage audio.

## Matrice des expériences

| Catégorie | Changement testé |
|---|---|
| Observation | Aucune écriture audio ; collecte d'état uniquement |
| Routage VoIP | Demande du mode de communication avant l'ouverture du flux |
| Large bande | Ajustement conditionnel après négociation compatible, avec restauration de l'état initial |
| Contournement Telecom | Essai du chemin HFP sans créer un appel local concurrent |
| Pont RX / TX | Routage logiciel d'une seule direction à la fois |
| Pont bidirectionnel | Référence expérimentale combinant RX et TX, sans validation présumée |

Chaque intervention est refusée si ses prérequis ne sont pas satisfaits. Un code de retour favorable ou un changement d'état logiciel ne prouve pas qu'une voix est transportée. Les réglages ne sont pas réappliqués en boucle ; si la restauration échoue, les essais suivants sont bloqués.

## Collecte et confidentialité

La capture est démarrée avant les appels. Le rapport peut contenir des journaux système et des états audio ; le masquage est partiel et ne garantit pas l'anonymisation. Relire le ZIP avant tout partage. Garder les rapports bruts, captures Bluetooth, fichiers système, inventaires et empreintes hors du dépôt public.

## Validation et limites

La version a été compilée, signée et contrôlée localement. Des tests hôtes couvrent les conditions d'activation, le nettoyage, la restauration et l'export. Ces tests ne remplacent pas les essais physiques ; cette note ne revendique pas de résultat audio matériel pour les modes expérimentaux.

La clé de signature doit rester privée. Si une intégration continue signe les APK, configurer les secrets dans les paramètres privés du dépôt et ne jamais ajouter la clé ni son mot de passe aux fichiers versionnés.
