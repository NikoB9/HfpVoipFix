# HfpVoipLab 1.8.0 : duplex continu et premier D5 Telecom

La 1.7.22 reste la référence matérielle : duplex confirmé pendant une phase de 15 secondes. La 1.8.0 conserve ses sources, gains et périphériques audio, mais maintient le duplex jusqu’au raccrochage après une courte ouverture ordonnée RX, micro puis TX. Cette durée prolongée doit être validée sur le Redmi.

## Interface

Les parcours principaux sont D4 continu et D5 expérimental. D2, D3 et le diagnostic D4 en quatre phases restent dans les outils avancés. La préparation audio reste automatique pendant la session ; notification d’arrêt et aucune sauvegarde PCM. Les numéros ne sont plus préremplis. Les commandes D5 passent par Téléphone ; les boutons HFP directs refusent D5 pour éviter une seconde voie de commande.

## D5

D5 réutilise le ConnectionService HFP natif de Bluetooth au lieu de le bloquer. Les connexions entrantes/sortantes reçoivent le flag VoIP ; les ports logiciels et paramètres MediaTek sont préparés comme pour le duplex. Le pont attend une connexion native retournée non déconnectée, les hooks requis, le codec négocié et les ports confirmés. Il refuse MODE_IN_CALL, une route incorrecte persistante, un état Bluetooth périmé et les captures rendues silencieuses. Il ne crée pas de seconde connexion Telecom ni de nouveau compte.

Le compte HFP/Samsung doit être présent, activé dans les comptes d’appel de Téléphone et sélectionné pour appeler. Si MIUI ne l’expose pas, exporter le rapport ; ne pas choisir une SIM locale comme substitut. D5 n’a pas encore été validé sur ce téléphone. La connexion native et le flag VoIP ne prouvent pas une bonne route audio.

## Installation et essais

Installer l’APK 1.8.0, réactiver le module Magisk 1.7.22 existant avant le redémarrage et vérifier le module dans LSPosed. Un seul redémarrage pour les deux. Le binaire natif est inchangé : aucune nouvelle installation ZIP requise s’il est déjà installé. Son marqueur reste 1.7.22-one-boot ; il sera de nouveau désactivé pour le démarrage suivant.

Commencer par D4 pendant deux à trois minutes, son et micro confirmés par le correspondant. Raccrocher, appliquer D5 hors appel dans la même session, attendre prêt, puis essayer un appel sortant via Téléphone avec le compte Samsung et un appel entrant vers le Samsung. Exporter le rapport. Pas de nouvelle validation D2/D3 requise.

## Android Auto

Objectif : téléphone maître (SIM/appel), HFP vers Redmi esclave root, puis commandes et audio vers la voiture. Liaison Android Auto USB ou sans fil encore indécise. L’audio reste actuellement explicitement dirigé vers le haut-parleur et le micro internes du Redmi ; aucune compatibilité voiture n’est annoncée.

La documentation officielle Android Auto du 18 juin 2026 demande Jetpack Telecom et les callbacks de commandes distantes pour une application d’appels personnalisée. D5 réutilise ici l’intégration native HFP de l’OS ; ce n’est pas une implémentation de cette future application Android Auto. Il faudra vérifier les commandes, le routage audio de la voiture et la coexistence Bluetooth séparément, puis choisir l’intégration adaptée.

Sources : https://developer.android.com/training/cars/communication/calling et https://developer.android.com/reference/android/telecom/ConnectionService .

## Validation

Tests hôtes des hooks : D5 conserve la création et l’enregistrement Telecom, expose les deux ports et applique le flag VoIP. Tests du gate : D5 n’utilise pas une confirmation de bypass, exige sa connexion Telecom et ses hooks. Tests du calendrier : D4 reste en duplex au-delà de 60 secondes et d’une heure ; le diagnostic conserve ses quatre phases. Compilation, signature et alignement APK vérifiés en CI. Aucune de ces vérifications ne remplace les essais matériels D4 long et D5.
