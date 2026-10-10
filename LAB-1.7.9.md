# HfpVoipLab 1.7.9 — route microphone SCO stable

## Rapport 1.7.8

Le rapport du 10 octobre confirme un appel en `soft_tx` et un SCO actif à 16 kHz. Le pont s'est ensuite arrêté avant toute confirmation de route, avec une `NullPointerException` causée par un `AudioDeviceInfo` devenu nul entre deux lectures successives de la route. Aucun événement `MIC_SELECTED`, `ROUTE_CONFIRMED` ou `STATS direction=tx` n'a été écrit ; le rapport ne prouve donc pas que des échantillons micro ont été envoyés.

## Correctif

La vérification lit maintenant une seule fois les périphériques d'entrée et de sortie par cycle. Si l'un des deux est temporairement absent, le pont attend le prochain cycle sans déréférencer une valeur nulle. La confirmation du micro utilise le même instantané que le test de route. Si la route change après confirmation, le pont s'arrête comme avant.

La version 1.7.9 est signée avec la même clé locale de test que la 1.7.8 fournie pour l'essai. Elle peut être installée par-dessus cette version sans désinstaller l'application.
