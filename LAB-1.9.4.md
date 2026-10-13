# 1.9.4 — capture confirmée et canal audio véhicule D6

Le rapport 1.9.3 a un journal indépendant du pont cohérent (SCO maître reçu, réception non nulle, sortie REMOTE_SUBMIX), mais la capture logcat échoue et les instantanés optionnels conservent des données antérieures. La sortie de projection prouve une écriture locale, pas la restitution par l’autoradio.

## Capture

Le marqueur est réémis jusqu’à confirmation par le lecteur, pendant au plus 12 s. Le premier marqueur pouvait précéder le lancement effectif su/logcat ; cette course est corrigée, sans prétendre prouver que c’était l’unique cause sur MIUI. La session supprime toutes ses données précédentes, y compris les instantanés Telecom/projection/chargement, en préservant le ZIP précédent et le firmware. capture-state.txt indique explicitement si logcat a été confirmé. Le nombre de lignes examinées et l’erreur sont restaurés lors d’une recréation du service. Un arrêt consécutif au timeout n’ajoute plus une fausse erreur de lecteur interrompu.

## D6 uniquement

Le monitor de Bluetooth examine HeadsetService (rôle passerelle vers le véhicule). Pendant un unique appel HFP maître actif/confirmé, il journalise le nombre de périphériques mains libres connectés, l’existence d’un appareil actif distinct du maître et son état audio. Après ouverture du SCO maître, il demande connectAudio(device) si une unique cible distincte est active, audio=10 (déconnecté) et AudioManager en MODE_NORMAL. Le délai initial est de 2 s, la pause 5 s, trois demandes maximum par appel. Les états 11/12 (connexion en cours/connecté), plusieurs appareils, un appel local en mode 2 ou une identité commune sont laissés intacts. isScoAcceptable, les permissions/choix du périphérique actif et les refus natifs ne sont jamais contournés. La réponse true ne prouve pas l’ouverture.

Le pont D6 attend jusqu’à 5 s la paire de ports du véhicule avant de sélectionner le repli de projection. S’ils apparaissent, il essaie leur routage existant de 1.9.3 ; sinon il conserve le repli projection/micro local. Aucun changement de format, de mode global, de binaire vendor ou XML. La coexistence de deux SCO et le son/micro voiture restent non validés.

## Réseau et diagnostics

CAR_NETWORK_STATE lit le service réseau rapporté par l’AG Samsung (EXTRA_NETWORK_STATUS) et le getCindService local exposé au véhicule, au plus toutes les 3 s. Il ne modifie aucun indicateur. Ces lectures peuvent être indisponibles selon les méthodes de la ROM ; cet échec est explicite et n’arrête pas le relais. Les logs HeadsetService/HeadsetStateMachine et un instantané Bluetooth sont inclus dans le rapport privé pour étudier les demandes, refus et déconnexions. L’erreur de numérotation de l’autoradio n’est pas déclarée corrigée.

D5 conserve son chemin 1.9.0 ; la nouvelle demande véhicule et les lectures réseau sont réservées à D6. USB/Wi-Fi ne sont pas utilisés pour décider des routes. Magisk runtime 1.9.0 inchangé.

Sources versionnées pour le téléphone Android 11 :
- https://android.googlesource.com/platform/packages/apps/Bluetooth/+/refs/heads/android11-release/src/com/android/bluetooth/hfp/HeadsetService.java (connectAudio et isScoAcceptable)
- https://android.googlesource.com/platform/packages/apps/Bluetooth/+/refs/heads/android11-release/src/com/android/bluetooth/hfp/HeadsetStateMachine.java (processAtCind)

Validation : fixture exécutant le véritable monitor avec fakes Android/Xposed (isolation D5, délai, états, mode étranger, identités, ambiguïté, plafond) ; suppression sélective des données de capture et préservation firmware/ancien ZIP ; CI Android 11/signature. Pas de test matériel disponible ici.
