# 1.9.14 — demande de focus de projection pendant le relais D6

VersionCode 77. Base main vérifiée avant modification :
`9c98bcc6091cf6931d3c2f688955bd941bc0fd56`.

## Problème et changement

L'[audit des binaires installés](AUDIT-STATIQUE-2026-10-23.md) identifie un verrou
dans Android Auto : une demande de focus transitoire peut être bloquée avant son
envoi au véhicule parce qu'AA voit un appel actif et un état LOSS_TRANSIENT côté
véhicule. Les correctifs de focus du Redmi ne modifient pas ce verrou. AA peut
par ailleurs continuer à lire le sous-mix sans transmettre ses blocs audio.

La nouvelle intervention s'exécute **après** le traitement original de `iso.m`.
Elle ne s'applique que si toutes les conditions suivantes restent réunies :

- SHA-256 de base.apk identique à l'AA 17.8.663874 fournie ;
- D6, begonia, Android 11, runtime natif accepté et preuve SCO NativeScoLease
  fraîche de moins de cinq secondes (un appel HFP actif, transport/ports préparés) ;
- demande MAY_DUCK gain=3, attribuée par AA à `com.hfpvoipfix` ; c'est le gain du
  relais navigation par défaut et des profils média comparatifs ;
- AA voit un appel, son état véhicule est LOSS_TRANSIENT, aucun envoi n'attend
  une réponse, son gestionnaire n'est pas arrêté et son transport est lié ;
- mode audio actuel NORMAL ; les modes téléphonie et communication sont exclus.

Elle invoque alors le vrai émetteur `iso.l(MAY_DUCK, 1)`, une seule fois par objet
et session SCO. La demande, sa réponse et son timeout suivent le traitement AA
normal. Une exception consomme cet unique essai. Aucune boucle de forçage, aucun
changement de call state, aucun succès de connexion/capture/focus fabriqué. Une
perte permanente n'est pas contournée et le véhicule reste libre de refuser.

Une AA différente désactive ces hooks obfusqués ; son fonctionnement normal est
conservé. La branche parallèle `qvk` est observée seulement, faute d'attribution
suffisante au relais. **L'archive statique n'établit pas encore que le verrou ou
la branche corrigée a été rencontré dans les derniers appels.** Cette version
corrige une incompatibilité de politique identifiée ; elle n'est pas une preuve
de réparation acoustique sur la voiture.

## Mesures nouvelles

`AA_CAR_FOCUS` journalise la demande Android et les gardes, l'essai supplémentaire,
l'entrée et le retour de l'émetteur, puis les états véhicule traités. Les états
sont ceux de cette APK : 0 invalide, 1 gain, 2 gain transitoire, 3 perte,
4 perte avec duck, 5 perte transitoire, 6 média seulement, 7 guidage seulement.
Une entrée d'émetteur ou son retour ne signifie pas que le véhicule a accordé.
Les états traités peuvent aussi provenir d'événements internes : ce n'est pas
un enregistrement brut des paquets de l'autoradio.

Le lecteur PCM AA publie `car_focus_hook=exact_iso`, `apk_mismatch` ou
`iso_unavailable`, en plus des mesures 1.9.13. Cela permet de constater le
chargement même si les lignes de démarrage ont quitté logcat.

L'observateur de `jdz`/`jfd` compte les appels de soumission de trames, ceux dont
le canal était démarré à l'entrée, les messages ACK 32772 reçus à l'entrée de leur
traitement et les exceptions. Il rapporte des fenêtres de deux secondes par
endpoint et session. Il ne lit ni ne conserve les buffers. ACK reçu n'est pas
ACK accepté ni son joué ; l'absence de fenêtre ne prouve pas l'absence de trafic
sur l'autre implémentation. La collecte par PID ajoute les vrais tags audio AA,
sans élargir le plafond de stockage.

## Références préservées

D5, phases normales RX/micro/TX, codecs négociés 8/16 kHz, patch Magisk 1.9.0,
cinq cibles LSPosed et règles de focus système conservés. Aucune réintroduction
de deux SCO. Le micro est toujours celui du Redmi. Le diagnostic 1.9.13 et les
quatre profils restent disponibles, sans être nécessaires au premier essai.

## Validation et essai court

Tests hôtes du workflow, nouveau test exécutant la classe de hook de production
sur des fixtures synthétiques : refus d'APK différente, D5/autre propriétaire,
preuve absente/expirée/future, demande permanente, état véhicule différent,
réponse en attente, gestionnaire arrêté, transport absent, modes 2/3, exception,
limite d'une tentative et maintien du vrai état véhicule. Les compteurs transport
et l'absence de données audio/noms d'autres packages dans le journal sont vérifiés.
Compilation API 30, signature et alignement sont vérifiés dans le workflow.
Ces contrôles ne sont pas des essais du Redmi ou de l'autoradio.

Installation : signature temporaire si les secrets stables sont absents. Dans
ce cas désinstaller l'ancienne application, installer, accorder les autorisations,
réactiver le module sur les cinq cibles et redémarrer **une fois**. Conserver
Magisk 1.9.0-persistent.

Premier essai : ouvrir une session de capture, sélectionner **Activer le relais
AA**, connecter AA comme d'habitude, puis passer **un appel de 30 secondes depuis
l'autoradio**, comme dans le dernier essai, correspondant parlant régulièrement.
Raccrocher et exporter. Signaler si le son tient ou l'instant approximatif de sa
coupure. Pas de comparaison de quatre appels demandée. Le résultat attendu dans
le rapport est d'abord la séparation blocage/envoi/réponse/soumissions ; aucune
amélioration acoustique n'est annoncée avant ce retour.
