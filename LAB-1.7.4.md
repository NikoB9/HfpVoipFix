# HfpVoipLab 1.7.4 — correction de la clé MediaTek WBS

## Ce que montre l'essai 1.7.3

Le rapport d'essai montre que D2 a attendu la fin du routage HFP natif avant d'appliquer le réglage. Le chemin RX est toutefois resté dans le mode précédent : les interruptions ne progressaient pas et les échantillons étaient nuls. Un code de retour sans erreur ne prouvait pas que le réglage avait été reconnu.

## Cause identifiée lors de l'analyse locale

Une analyse privée des fichiers système a permis d'identifier que le réglage utilisé par la version précédente n'était pas la clé attendue par cette plateforme. Le nom précis de cette clé et les détails internes du composant sont omis de cette note publique.

## Correction en 1.7.4

D2 lit d'abord la valeur de plateforme et refuse l'intervention si elle est inconnue. Après le retour de la route HFP native, il applique le réglage nécessaire ; à la fin du SCO, il restaure la valeur lue. Le mode reste RX uniquement : pas de microphone, de patch Audio, ni de renégociation du codec. Un statut sans erreur ne suffit pas à prouver que le chemin est passé en bande large.

## Vérification

- 60 assertions de sécurité des hooks passées, ainsi que les cas « cycle de vie manquant ».
- APK `com.hfpvoipfix`, versionCode 39 / `1.7.4-lab`, Signature Scheme v3 et alignement vérifiés ; certificat identique aux versions précédentes.
- Un essai ultérieur a été interrompu avant écriture parce que la lecture de la valeur précédente était indisponible. La 1.7.5 documente le repli expérimental. Le test suivant doit vérifier le mode réellement actif, les interruptions et les échantillons non nuls ; un retour sans erreur ne suffit pas.
