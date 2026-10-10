# HfpVoipLab 1.7.5 — essai WBS avec état de repli documenté

## Résultat du journal reçu

Un journal d'essai nommé avec une ancienne version indiquait que le module chargé était en réalité la version suivante. La lecture de l'état précédent était indisponible ; aucune ouverture RX n'a donc eu lieu. Ce rapport ne teste pas encore le chemin corrigé. Les noms de fichiers et horodatages de la capture privée sont omis.

## Changement

La 1.7.5 utilise la valeur antérieure lorsqu'elle est lisible. Si la lecture est vide ou échoue, elle applique le mode large bande uniquement après négociation correspondante, puis restaure un état inactif explicite. Ce repli repose sur une mesure antérieure et n'est pas une lecture de l'état courant ; le journal le signale comme une hypothèse de repli.

L'essai reste RX uniquement. Si la demande WBS retourne une erreur ou lève une exception, D2 ferme le pont et tente de restaurer l'état de repli.

## Résultat appareil reçu

Le journal de 1.7.5 confirme que le réglage est appliqué après la négociation et que le pont est routé entre l'entrée SCO et le haut-parleur.

Le rapport d'appel recoupe la même session et confirme l'application du réglage. Il ne contient pas de trace Bluetooth exploitable pour cette session.

Le transport RX reste muet : les lectures ne rapportent pas d'activité et les échantillons restent nuls. À la fin, le mode est restauré et le port d'entrée est retiré. Les statistiques détaillées du rapport privé ne sont pas publiées.

La correction de mode est donc vérifiée, mais elle n'a pas rétabli les données PCM. Ce journal seul ne permet pas de départager l'absence de paquets audio SCO entrants, le transport contrôleur/PCM et une autre rupture dans le chemin IRQ. Il ne contient pas la capture HCI du même appel.

## Vérification de build

- 62 assertions des hooks passent, dont le cas où la lecture de l'état est vide et le repli/restauration.
- APK `com.hfpvoipfix`, versionCode 40 / `1.7.5-lab`, signature v3 et alignement vérifiés, certificat inchangé.
