# 1.9.15 — export Téléchargements, diagnostics expliqués et confirmation d'appel D6

VersionCode 78. Main vérifiée avant modification :
`999b4c2256afc7f647d174798e89d82eafc69b9a`.

## Nouveau rapport réellement lu

Archive privée `HFP-Rapport.zip`, SHA-256
`440fe7696f4b1544599d50cd107f1f7d103840abf24b437aecbff9fd20873013`.
Les journaux bruts et binaires propriétaires ne sont pas publiés. Le rapport
identifie APK/module 1.9.14, begonia Android 11, runtime 1.9.0-persistent et le
HAL attendu. Les heures ci-dessous sont celles affichées dans le rapport.

Deux appels HFP sont identifiés. Les deux sont précédés d'un `GH.CallManager
placeCall` dans AA ; cela établit le passage par AA, sans reconstituer le geste
physique sur l'autoradio. Le premier appel a deux sessions SCO, le second une.

| Appel / SCO | Codec rapporté | Début RX | Ajout micro Redmi | Ajout TX SCO | Résultat logiciel |
| --- | --- | --- | --- | --- | --- |
| 26238-1 / seq 1 | WBS, 16 kHz | 21:56:04.719 | 21:56:06.061 | 21:56:07.182 | Pont fermé après perte/changement SCO vers 21:56:12.2 |
| Même appel / seq 2 | WBS, 16 kHz | 21:56:13.520 | 21:56:14.575 | 21:56:15.618 | Pont actif jusqu'à fin d'appel / transition suivante |
| 26238-2 / seq 3 | narrowband, 8 kHz | Aucun | Aucun | Aucun | Ports préparés, mais `telecom_ok=false` : pont non démarré |

Les profils réellement démarrés sont tous `comparison=false trial=1 usage=12
mic_source=7`. Ni la comparaison de quatre profils ni le diagnostic à huit
étapes n'a été exécuté. La reconnexion du SCO du premier appel n'est pas un
nouvel appel comparatif.

### Le verrou ciblé en 1.9.14 a bien été rencontré

Dans la deuxième session SCO du premier appel :

- 21:56:13.450 : `blocked_relay=true`, branche iso et APK exacte ;
- 21:56:13.451–.453 : invocation réelle de l'émetteur MAY_DUCK et retour ;
- 21:56:13.464 : état AA traité = 7, guidage seulement, flags false/false ;
- 21:56:14.106 : arrêt d'un endpoint audio observé ;
- 21:56:14.113 : état AA traité = 5, perte transitoire, flags true/false.

L'écart entre 7 et 5 est **649 ms**. Le second drapeau false signifie que le
traitement n'est pas marqué gearheadForcedResponse dans cette APK ; le premier
est unsolicitedResponse. Ce sont les événements traités par AA, pas une capture
réseau indépendante ni une preuve acoustique.

La perte de focus et l'arrêt observés surviennent **avant** l'ajout du microphone
à 14.575 et du TX à 15.618. Leur ouverture n'est donc pas nécessaire à cette
séquence d'arrêt AA. La première session comporte aussi un arrêt d'endpoint à
05.410, avant l'ajout du micro. Cela ne prouve pas que chaque arrêt concerne la
voix ni que l'utilisateur a entendu ces intervalles précis.

Le rapport contient sept fenêtres du lecteur AA navigation id 46 : cinq ont un
RMS échantillonné non nul ; le lecteur média id 42 a sept fenêtres à zéro. Le
seul compteur de soumission/ACK publié concerne `stream=1` : 12 appels, 11
messages ACK reçus. `trh.aa(1)` de l'APK auditée nomme ce flux **SYSTEM**. Il serait
faux d'en faire la preuve d'un envoi de notre voix sur TTS/guidage. L'observateur
d'arrêt 1.9.14 ne précisait pas le flux : cette association reste inconnue.

Les pistes locales continuent à écrire lors des premières pertes AA. Les pauses
locales explicites sont proches de la perte de transport ou du raccrochage.
Les niveaux TX restent très faibles (RMS souvent 0 ou 1), bien que le micro
Redmi soit routé et que ses captures ne soient pas déclarées silencieuses.
`reader_silenced=unknown` reste inconclusif pour le lecteur AA lui-même.

**Bilan : le correctif d'envoi 1.9.14 s'applique, mais ne suffit pas.** La 1.9.15
n'ajoute ni nouvelle demande répétée ni faux accord, et ne prétend pas résoudre
la restitution du premier appel. Elle ajoute le numéro de flux aux arrêts et
observe les allocations de composant aval `ise.a`, sans changer leur résultat,
pour distinguer la capture navigation d'un autre flux audio AA.

### Défaut déterministe du deuxième appel

| Heure | Observation du rapport |
| --- | --- |
| 21:56:21.072 | Fin HFP du premier appel |
| 21:56:21.542 | La nouvelle connexion Telecom HFP est confirmée |
| 21:56:22.201 | Fermeture SCO du premier appel |
| 21:56:22.206 | Première notification HFP du deuxième appel |
| 21:56:24.217–.404 | SCO/ports/paramètres natifs préparés à 8 kHz |
| À partir de 21:56:24.3 | `telecom_ok=false` demeure ; aucun pont logiciel de l'appel 2 |

Le code 1.9.14 conserve une confirmation Telecom globale, et
`finishCommunicationIfIdle` l'efface lorsque appels et SCO deviennent vides.
L'ordre ci-dessus laisse un intervalle de cinq millisecondes entre la fin de
l'ancien SCO et la première notification du nouvel appel. La nouvelle connexion,
créée avant ce nettoyage, perd sa confirmation. Ce défaut explique le non-départ
du pont 2, sans expliquer à lui seul la coupure du pont 1.

Correction D6 : conserver une preuve issue de la **connexion HFP réelle**, par
son `getUUID()` (méthode vérifiée dans MtkBluetooth fourni), puis la corréler au
UUID de la notification HFP. La fin du SCO précédent peut remettre le booléen
courant à false ; elle n'efface pas la preuve d'une autre connexion en attente.
Un nouvel appel ne peut pas hériter d'un UUID différent. Fin d'appel et
déconnexion de profil révoquent les preuves concernées. Connexions déconnectées,
références faibles libérées et preuves non associées après 30 secondes sont
écartées ; limite huit entrées. Une connexion associée à un appel actif n'expire
pas au milieu d'un appel long. D5 conserve sa règle historique.

## Utilisation et export

- Bouton **Enregistrer le rapport dans Téléchargements** : copie du ZIP prêt sous
  un nom daté, sans feuille de partage. Le nom effectif est affiché ; ouvrir la
  conversation voulue puis joindre ce fichier. Même changement pour le firmware.
- Écriture via MediaStore.Downloads, API disponible sur ce téléphone Android 11.
  Pas de permission générale de stockage, pas d'API de chemin partagé dépréciée.
  Le fichier n'est publié qu'après fermeture réussie ; toute copie partielle est
  supprimée en cas d'échec, et le ZIP privé original est conservé. Le fournisseur
  peut adapter un nom en collision : son nom fait autorité.
- Les diagnostics AA sont regroupés dans une section repliée **facultative**.
  Chaque bouton explique son objectif, sa durée et les changements avant
  activation. Le retour au relais AA habituel est indiqué.
- L'essai à un appel sépare réception, micro, TX, leurs retraits, puis média ;
  huit étapes de huit secondes, appel de 70 secondes. Il mesure une relation de
  cause, sans réparer automatiquement le son. Le passage média redémarre aussi
  piste et focus : cette limite est explicitée.
- La comparaison utilise quatre appels de 30 secondes et les profils historiques.
  Il n'est pas demandé de lancer ces diagnostics pour utiliser le relais.

La collecte ciblée accepte désormais aussi `gearhead:car`, processus déclaré
dans l'APK AA fournie. Elle privilégie :car puis :projection, toujours deux
lecteurs au maximum et le même plafond de 3 Mio. Le rapport actuel ne suivait
que :projection dans aa-events, alors que les hooks audio du PID 11514 sont
visibles dans hfp-events. La liste des processus réellement actifs lors de ce
rapport n'est pas disponible : le PID 11514 ne peut pas être attribué avec
certitude à :car à partir de cette seule archive. L'ajout ferme un défaut de
couverture vérifié dans le manifeste, pas une preuve que tous les logs manquants
réapparaîtront.

## Vérifications et limites

Le test de hook de production reproduit l'ordre connexion B → nettoyage SCO A →
notification HFP B. Il vérifie l'UUID réel, les exclusions d'autres appels, les
preuves expirées/déconnectées, et la conservation de D5. Les tests d'export
exécutent DownloadExport avec un fournisseur synthétique : octets identiques,
publication après fermeture, renommage, échecs création/écriture/fermeture/
publication, nettoyage partiel et conservation du rapport original. Les tests
AA vérifient que l'absence de composant aval n'est pas transformée en succès.
La sélection :car/:projection et sa limite de deux lecteurs sont testées.

D5, module Magisk 1.9.0-persistent, cinq cibles LSPosed, codec négocié et absence
de deuxième SCO sont conservés. Le micro reste celui du Redmi. Les fixtures et
la compilation ne valident ni Téléchargements sur MIUI réel ni la restitution
par l'autoradio.

Cette version corrige l'export et le non-départ possible d'un appel suivant.
Elle ne promet pas la disparition de la coupure du premier appel. Après
installation et un redémarrage, garder le relais AA normal ; deux appels
consécutifs suffisent pour vérifier que le deuxième démarre désormais son pont.
Les quatre profils et les huit étapes ne sont pas demandés à ce stade.

Référence Android : [MediaStore.Downloads et fichiers possédés par l'application](https://developer.android.com/training/data-storage/shared/media).
Les sources et limites de l'audit AA/MIUI restent dans
[AUDIT-STATIQUE-2026-10-23.md](AUDIT-STATIQUE-2026-10-23.md).
