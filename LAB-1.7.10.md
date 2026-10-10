# HfpVoipLab 1.7.10

## Correction D3 micro

Le rapport HFP de la version 1.7.9 montre que le micro intégré était détecté (`id=15`), que la route micro → SCO était confirmée, puis que le service s’arrêtait immédiatement sur une `NullPointerException`. Le chemin D3 laisse Android choisir le micro par défaut, donc l’objet `input` est volontairement nul. Une vérification secondaire utilisait pourtant `input.getId()` au lieu de l’identifiant `inputId` établi lorsque la route a été confirmée.

La 1.7.10 vérifie la route avec cet identifiant confirmé. L’APK garde le même certificat de signature que la 1.7.9 et peut être installée par-dessus.

## Essai

Après installation et redémarrage pour recharger le module LSPosed : démarrer la capture, choisir D3, armer avant l’appel, puis parler et vérifier auprès du correspondant. Le rapport doit maintenant contenir des lignes `STATS direction=tx`. Si la route micro → SCO passe mais que le correspondant n’entend toujours rien, exporter ce nouveau rapport : il permettra de distinguer des échantillons silencieux d’un défaut de transmission Bluetooth.
