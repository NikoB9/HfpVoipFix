# HfpVoipLab 1.5.1 — comparaison D / D1

Objectif exclusif : audio bidirectionnel entre deux appareils reliés par Bluetooth. Android Auto est une phase ultérieure.

## Installation et campagne courte

Mettre à jour Lab 1.5 avec l'APK 1.5.1 (même package et même certificat, versionCode 31). Conserver le scope LSPosed Bluetooth uniquement. Redémarrer une fois pour charger le nouveau code ; l'interface refuse l'application d'un mode si le module chargé répond encore 1.5.0.

Ne pas désinstaller le module ni toucher aux autres modules Magisk/LSPosed.

1. Vérifier root/module dans le Lab. Autoriser les stratégies expérimentales, sélectionner **D**, appliquer et démarrer une capture.
2. Appeler un correspondant depuis l'appareil source, choisir l'autre appareil comme destination audio Bluetooth. Essayer d'écouter et de parler dans son microphone. Raccrocher, noter les deux sens séparément.
3. Passer à **D1**, appliquer et répéter sans arrêter la capture. Faire un appel d'au moins 15 secondes pour observer si le mode communication se maintient.
4. Revenir à **D** pour un dernier appel témoin : vérifier que l'état audio initial est récupéré après D1.
5. Terminer, vérifier et partager le ZIP unique. Pas de nouvelle série A/B/C/E/G/H/I nécessaire.

Si une restauration non confirmée est signalée, arrêter les appels et exporter au lieu de poursuivre. Aucun appel SIM/VoIP concurrent sur l'appareil testé pendant cette expérience. En D/D1, l'absence d'écran Telecom local est attendue ; les gestes d'appel restent sur l'appareil source.

## Différence technique isolée

- **D** : contournement HFP Telecom de 1.5, sans modification du mode audio.
- **D1** (`bypass_comm`) : même contournement, plus une demande `MODE_IN_COMMUNICATION` via l'AudioManager de la machine HFP. Aucun AudioPatch, ajout de port, forçage haut-parleur ou paramètre WBS supplémentaire.

L'activation attend que le contournement ait réellement été appliqué et qu'une route SCO soit suivie. Elle exige un mode initial `MODE_NORMAL` et une seule route SCO suivie. Si les événements arrivent dans l'ordre SCO puis contournement, la demande est faite une fois ce dernier confirmé ; le moment est journalisé.

Le mode est relu après la demande puis surveillé sans réapplication automatique. Un retour à un autre mode est signalé, pas masqué par des écritures répétées. La demande est conservée lors d'une interruption SCO si l'appel est encore actif, puis retirée après la fin de tous les appels et routes suivis. Le hook de déconnexion Bluetooth assure aussi ce nettoyage si la terminaison d'appel manque. D1 est indisponible si ce hook manque.

Le retour au mode normal retire la demande du processus selon l'implémentation AOSP ciblée. Si le mode observé reste en communication, le nettoyage est signalé comme non confirmé et de nouvelles interventions sont bloquées. Les tests sur doubles vérifient aussi le cas d'un autre propriétaire en mode appel ; cela ne constitue pas une garantie de comportement de toutes les ROM dans toutes les courses concurrentes.

## Collecteur et interface

Le masquage ne remplace plus des suites numériques arbitraires. Préservation des dates, heures, PID/TID, fréquences et identifiants techniques. MAC, URI tel/SIP, courriels, champs de numéro/identité explicitement nommés et numéros internationaux contigus restent filtrés. Un numéro sans libellé peut rester visible : vérifier le ZIP avant partage.

Les événements `STRATEGY`, `COMM_BEFORE`, `COMM_REQUEST`, `COMM_MONITOR` et `COMM_RELEASE` distinguent demande, application et nettoyage. « Appliquée » signifie changement logiciel observé, jamais voix validée. Les décisions sont aussi consignées dans les notes de `sessions.json`. Les journaux AudioService et MediaFocusControl sont maintenant retenus, avec instantanés après demande/libération du mode communication.

Capture continue et limites inchangées : premier segment 6 Mio + cinq derniers segments 6 Mio ; avertissement et compte des suppressions du milieu. Export ZIP et observations séparées réception/microphone conservés.

## Fondement et validation

Le rapport utilisateur de Lab 1.5 montre en D un état AudioPolicy NORMAL et l'absence des deux routes téléphoniques locales présentes en C, avec `hfp_enable=true` mais sans voix entendue. Il justifie cette comparaison isolée, sans prouver la cause du silence.

Source API correspondant à la cible : [AOSP Android 11 AudioService](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android11-release/services/core/java/com/android/server/audio/AudioService.java), méthodes `setMode`/`setModeInt`. [Machine HFP Android 11](https://android.googlesource.com/platform/packages/apps/Bluetooth/+/refs/heads/android11-release/src/com/android/bluetooth/hfpclient/HeadsetClientStateMachine.java), `broadcastConnectionState` et `routeHfpAudio`.

Tests hôtes sur le code de production : attente de confirmation du contournement, ordre des événements, maintien pendant interruption SCO, nettoyage dans les deux ordres fin appel/fin SCO, déconnexion sans fin d'appel, refus depuis un autre mode, requête ignorée, erreur de libération, préservation des horodatages et valeurs techniques. Les tests d'export existants restent applicables.

Aucun appareil/émulateur accessible dans cet environnement ; aucun test de voix sur le matériel cible ou d'interface sur Android. Aucun nouveau résultat matériel n'est revendiqué. APK compilée et signée avec la clé privée déjà utilisée en 1.5.0 ; aucun secret n'est inclus dans le dépôt.
