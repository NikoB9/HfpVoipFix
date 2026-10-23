# Audit des binaires réellement installés — 23 octobre 2026

## Périmètre et niveau de preuve

L'archive privée `HFP-Statique-20261023-202329.tar` a été extraite après contrôle
des chemins ; tous les fichiers de son manifeste SHA256SUMS ont été vérifiés.
Elle contient l'APK AA et ses splits, MtkBluetooth, MtkTelecom, framework.jar,
services.jar, le HAL audio 32 bits et deux XML vendor. Aucune donnée d'application
ni voix n'est nécessaire à cette analyse. Les binaires propriétaires, sources
décompilées et rapports bruts ne sont pas ajoutés au dépôt public.

L'empreinte système fournie est begonia / Android 11 / V12.5.6.0.RGGIDXM,
plateforme mt6785. Le marqueur natif est `1.9.0-persistent`. L'APK AA **installée**
est `17.8.663874-release`, versionCode `178663874`, mise à jour le 17 octobre.
Le même dumpsys présente aussi le stub système 1.1 : ce n'est pas l'application
active analysée. Sa mise à jour antérieure au rapport du 20 est compatible avec
la même version à cette date, mais cette archive statique ne prouve pas quel
chemin interne a effectivement fonctionné pendant les appels précédents.

| Fichier | SHA-256 |
| --- | --- |
| AA base.apk | `c56e4ebba055e9facdd96c4ea54e80c743a8d184c0d7891f25df98f71119d6ac` |
| MtkTelecom.apk | `e583e0c1e93b9f99f053b37f84c43b326320c809d082baf31165edfadb729e37` |
| MtkBluetooth.apk | `480aa1e2de6014bfcf8ed635b3841ce0592262035bd582cea3258a6092d72c74` |
| services.jar | `640323deb58d1eb39202dd6d485da2cce88e55958ba7b527c3efff198b700a5d` |
| framework.jar | `7d0c9b0deaf75268432c8b80a706e9ecdfe55d964722d433c4eae468bd539ef6` |
| HAL audio copié | `3387c3b7e9bc593728cd13c20e911007befb8fa25ea1dc8338645a487ec09ddc` |

## Méthode

JADX 1.5.6 a servi à la navigation. La décompilation AA et Bluetooth comporte des
erreurs ; celle de Telecom réussit. Les conditions importantes ont été relues
dans les instructions DEX avec Androguard 4.1.4 : une méthode Java reconstruite
incorrectement ne constitue pas une spécification. Capstone 5 et pyelftools ont
servi au désassemblage des fonctions ARM exportées du HAL. Les adresses ci-dessous
sont propres aux fichiers identifiés, pas des interfaces Android publiques.

## 1. AA : lecture PCM et émission sont deux étapes distinctes

La branche `irk` capture le REMOTE_SUBMIX, puis fournit les blocs à un composant
aval `irq` uniquement lorsque son état interne le permet. Le DEX confirme que la
lecture `iuw.b` n'est pas conditionnée par le même booléen que l'appel `irq.p`.
Dans `irk.e`, l'appel `irq.p` à l'offset **octet** `0x0eb4` est précédé du test de
`irk.j` à `0x0e98/0x0e9c`. Sans composant aval disponible, des données peuvent
être mises en attente ou abandonnées. Ainsi, AA_SUBMIX_PCM non nul n'établit pas
l'émission vers le véhicule. Cela correspond à l'angle mort du rapport du 20,
sans prouver à lui seul la cause acoustique de sa coupure.

Le composant d'émission de cette branche aboutit à `jdz`, sous-classe de `jfd`.
`jfd.C` soumet les trames au canal lorsqu'il est démarré ; il peut aussi les
rejeter s'il ne l'est pas. `jfd.a` traite notamment les messages d'acquittement
32772 et vérifie session et compteurs. La fonction de correspondance `wxw.z`
convertit cet identifiant brut en constante interne 32773 : ces deux nombres
ne doivent pas être confondus dans un hook à l’entrée de la méthode. La 1.9.14 observe ces frontières sans lire
les buffers. Un appel de soumission, même terminé sans exception, reste distinct
d'une réception distante ; un message ACK observé à l'entrée peut être refusé
par les vérifications normales. Aucun de ces compteurs ne prouve l'audibilité.

## 2. AA : un verrou d'appel précède la vraie demande à l'autoradio

Deux implémentations sont présentes : `iso`/`irk` et `qvk`/`quc`. Leur présence
ne dit pas laquelle est choisie par cette connexion. Le code construit ces
branches dans des chemins de connexion différents. Les deux comportent une
règle d'interdiction pendant un appel.

Dans `iso.m(int)`, le gain Android 3 est converti en demande transitoire
MAY_DUCK. Le code conserve les attentes de réponse en cours, vérifie si le focus
actuel suffit, puis empêche la demande lorsque le moniteur téléphonique voit
un appel **et** que le véhicule est en LOSS_TRANSIENT. Le gain permanent est
également bloqué pendant un appel. La vérification DEX de `iso.m` confirme :

- `0x0156–0x0162` : lecture du moniteur d'appel `iur.d` ;
- `0x0166–0x0176` : test gain permanent ou état `xko.f` ;
- `0x017a–0x01d0` : journalisation du refus et retour sans envoi ;
- `0x0228` : appel normal à `iso.l`, absent dans la branche de refus.

Les noms des états viennent de `trh.E` de cette APK : `xko.f` signifie
LOSS_TRANSIENT. Ce n'est pas une numérotation déduite d'un protocole communautaire.
`iur` initialise son booléen depuis TelephonyManager.getCallState et le suit par
PhoneStateListener. La présence de ce code ne prouve pas encore la valeur du
booléen pendant chacun des appels du rapport.

`iso.l` construit et envoie une vraie demande de focus sur son transport lié ;
il met en place l'attente et le traitement normal de la réponse/du timeout. Il
n'accorde pas lui-même le focus de l'autoradio. La règle similaire de `qvk.i`
a également été vérifiée en DEX. Cette branche n'expose pas à cet endroit une
attribution de package aussi sûre que `iso.i` : aucun contournement n'y est ajouté.

Le hook existant ProjectionFocusHook agit sur le focus Android du Redmi, pas
sur ce verrou dans AA. Il n'est donc pas suffisant pour établir un focus accordé
par le véhicule. Le classement particulier assistant/navigation de `iso.n` ne
constitue pas, à lui seul, une interdiction universelle des applications tierces :
il ne justifie pas de faire passer notre package pour une application Google.

**Conclusion : verrou réel identifié dans le code, déclenchement dans les appels
précédents non établi.** La correction 1.9.14 est expérimentale et vise ce verrou
uniquement lorsqu'il est effectivement rencontré pour notre demande D6.

## 3. Telecom MIUI confirme le chemin qui coupe le SCO véhicule

Le vrai `CallAudioRouteStateMachine` de MtkTelecom contient setBluetoothOn(String),
setBluetoothOff et les appels au BluetoothRouteManager. Les entrées de route
écouteur entraînent l'arrêt du Bluetooth audio lorsque nécessaire. Cela confirme
le chemin source du changement demandé par CarProjectionHook et recoupe les
déconnexions observées dans le rapport du 20 : environ 0,75 s de SCO véhicule
aux appels 1 et 3, **avant** le pont logiciel.

Ce recoupement reste une origine possible du bref son rapporté, sans marqueur
acoustique synchronisé. Supprimer ce hook relancerait le conflit de deux SCO
constaté en 1.9.4. La 1.9.14 conserve ce comportement ; aucun second SCO n'est créé.

## 4. Focus MIUI et HAL exact : pas de nouveau patch spéculatif

Le MediaFocusControl de services.jar comprend les propriétaires verrouillés
Telecom et une extension MIUI NAVI_LOCK pour une liste de packages de navigation.
La liste reconstruite contient Autonavi, Baidu et Tencent, pas HfpVoipFix ni AA.
Il serait faux d'attribuer automatiquement cette protection à toute piste de
USAGE_ASSISTANCE_NAVIGATION_GUIDANCE. Cette extension n'est pas modifiée.

Le HAL est un ELF ARM 32 bits dont le hash correspond **exactement** au patch
1.9.0 conservé. Fonctions retrouvées : createCaptureHandler à 0x1ef7a4,
syncSharedOutDevice à 0x1f3b4c, routingInputDevice à 0x1f595c, capture BTSCO open
à 0x1a1ac4, playback BTSCO open à 0x1883e8. AudioBTCVSDControl.getInstance utilise
une instance partagée. Le XML distingue primary input et voip_tx et expose les
ports SCO. Cela n'établit ni des PCM indépendants pour deux pairs HFP ni une
nouvelle configuration valide pour le transport voiture.

L'original stock n'est pas dans cette collecte : il n'y a pas de comparaison
binaire complète stock/patch nouvellement effectuée. Le désassemblage ARM ne
reconstitue pas tout le DSP. Le cœur natif AudioPolicyService MIUI et le code du
firmware de l'autoradio ne figurent pas non plus dans l'archive. L'exception
REMOTE_SUBMIX du code AOSP Android 11 reste pertinente, mais ne prouve pas toutes
les différences MIUI. Ces limites interdisent d'annoncer la confidentialité du
micro comme cause, ou de réécrire le HAL D5 à ce stade.

## Références et suite

Les sources AOSP/Xiaomi épinglées et leurs limites sont documentées dans
[RECHERCHE-ARCHITECTURE-2026-10-20.md](RECHERCHE-ARCHITECTURE-2026-10-20.md).
L'audit présent ajoute les binaires installés, sans les remplacer par les sources
d'Android Automotive ou une reconstruction communautaire d'Android Auto.

[LAB-1.9.14.md](LAB-1.9.14.md) décrit les exclusions, le seul changement actif,
les mesures et l'essai matériel restant. Les tests hôtes utilisent des fixtures
indépendantes et synthétiques ; ni l'APK AA propriétaire ni un autoradio ne sont
exécutés dans ces tests.
