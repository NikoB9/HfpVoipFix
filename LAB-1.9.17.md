# HfpVoipLab 1.9.17 — commandes et publication véhicule séparées

VersionCode 80, Android 11/API 30. Runtime natif Magisk 1.9.0-persistent inchangé.

## D6 contrôlable, parcours expérimental

Le choix `telecom_aa_control` conserve les vraies connexions HFP de Telecom,
leurs états et leurs commandes. Les interfaces Téléphone du Redmi et Android
Auto continuent de recevoir ces appels. Le pont audio garde la sortie navigation
et le micro Redmi ; il ne crée pas de second SCO et ne modifie pas les décisions
de focus reçues du véhicule.

Ce parcours ajoute une politique sélective de publication :

- Dans Telecom, `BluetoothPhoneServiceImpl.updateHeadsetWithCallState` ne publie
  pas l'agrégat lorsqu'il contient uniquement notre appel HFP, non urgent. La
  méthode `sendClccForCall` exclut la même entrée ; la réponse de fin CLCC reste
  gérée par le code natif. L'appel n'est pas retiré de CallsManager.
- Dans le binaire AA exact déjà audité, le sérialiseur `xjd.aZ` reçoit une vue
  séparée sans cet appel, via `ojc.k`, uniquement pour construire la publication
  `CarPhoneStatus` destinée au véhicule. Les autres appels à `ojc.k`, notamment
  ceux de l'interface AA, reçoivent la liste réelle inchangée.
- Les listes source, les objets CarCall, les connexions Telecom et les états
  HFP du Samsung ne sont pas modifiés. Cette exclusion de publication est une
  politique du module, pas une API Google documentée de relais HFP.

Ces filtres sont inactifs pour D5, AA isolé et l'ancien D6. Ils exigent le
mode explicite, begonia, Android 11, runtime 1.9.0-persistent, un seul appel et
le compte exact du service HFP Client. L'agrégat Bluetooth laisse passer les
urgences, les appels SIM, plusieurs appels ou une identité indisponible. Le filtre
AA laisse passer les identités inconnues et plusieurs appels. Il n'ajoute pas
de détecteur d'urgence AA : les états et commandes d'urgence ne sont pas altérés.

Le filtre AA exige le SHA-256 exact du base.apk audité. Le sélecteur refuse ce
parcours si le binaire installé diffère ou ne peut pas être vérifié. Le manifeste
ne déclare qu'une requête de visibilité pour le paquet Android Auto.
Les installations partielles des nouveaux filtres restent sans effet. Après
publication d'un appel étranger, urgent côté AG ou d'appels multiples, le filtre
concerné reste suspendu jusqu'à une liste réellement vide : il ne masque pas
brusquement un appel déjà exposé lorsque l'autre appel se termine.

## AA isolé conservé et commandable sur le Redmi

Le choix `projection_only` et son chemin audio de 1.9.16 restent disponibles.
Le nouvel écran principal ajoute un numéro, Appeler, Répondre et Raccrocher/refuser.
Ces commandes utilisent les méthodes HFP Client réelles du Samsung, sans créer
une connexion Telecom. Elles sont indépendantes de la capture de diagnostic.

La numérotation exige le relais isolé prêt et un état frais, sans appel ni
nettoyage en cours. Les commandes sont sérialisées ; leur accusé est corrélé au
token réellement traité. Un résultat `queued` signifie commande transmise et
ne prétend pas que le correspondant a répondu ou que l'appel s'est terminé.
Le nettoyage ne retire que la commande encore possédée par son émetteur.
Pour ce parcours, le module exige un seul appel entrant pour répondre et un
seul appel pour raccrocher. Les commandes directes refusent D5 et D6 Telecom.

## Vérification et limites

Les fixtures hôte exercent les hooks de production : exclusions de comptes et
modes, urgence AG, appels multiples, copie de liste et interface AA inchangée,
imbrication/nettoyage de la portée du sérialiseur, installation partielle,
commandes concurrentes, refus, préparation du relais et accusés réels.
Les tests existants du D5, des ports, du HAL et du focus restent applicables.
Ce ne sont pas des essais sur Redmi ou dans un véhicule.

Le D6 contrôlable réintroduit volontairement Telecom avec cette nouvelle
politique de publication. La coexistence audio et commandes doit être vérifiée
sur l'autoradio ; elle n'est pas annoncée validée avant cet essai. Le microphone
voiture reste non implémenté et le duplex voiture n'est pas déclaré validé.

## Essai court

Installer, activer les cinq cibles LSPosed et redémarrer une fois ; conserver le
runtime natif existant et AA en USB avec les réglages Bluetooth habituels.
Choisir « Activer D6 contrôlable », attendre le relais prêt, démarrer une capture,
passer un appel depuis Android Auto, vérifier le son pendant 30 secondes puis
raccrocher depuis Android Auto. Si possible vérifier aussi un appel entrant.
Enregistrer le rapport dans Téléchargements. En cas de retour de la coupure,
revenir hors appel à AA isolé dans la même APK, sans réinstallation.
