# HfpVoipLab 1.7.6 — activation de l’état BTCVSD MediaTek

## Indice confirmé dans les rapports

Dans le rapport de test précédent, le chemin RX s'ouvrait mais ne fournissait pas de trames exploitables. Les statistiques du pont montraient également une irrégularité temporelle. Les détails horodatés du rapport privé ne sont pas reproduits ici.

## Analyse locale de la plateforme

L'analyse locale a révélé qu'un second état de plateforme devait être activé en plus du réglage de bande passante. Les noms internes, offsets et détails du composant propriétaire sont omis ici. La version 1.7.5 ne modifiait pas cet état, ce qui concordait avec les observations du chemin RX.

## Changement 1.7.6

Pour D2 WBS seulement, après le retour de la route HFP native et avant le démarrage RX, la 1.7.6 active le second état de plateforme puis le réglage de bande. À la fin du SCO, elle restaure ces états dans l'ordre inverse. L'état inactif est un repli explicite lorsque sa lecture n'est pas disponible. Toute erreur de paramètre ferme le mode expérimental et conserve un indicateur de restauration non résolue afin d'interdire un nouvel essai.

Le rapport reçu ensuite confirme l'ouverture RX et des échantillons non nuls. L'utilisateur confirme que la voix est désormais claire. La 1.7.6 a donc corrigé le chemin de réception observé. Le TX n'était pas testé dans cette version.

## Validation de build

- 62 assertions de sécurité des hooks passent; elles ne remplacent pas un essai sur le téléphone.
- APK `com.hfpvoipfix`, versionCode 41 / `1.7.6-lab`, signature v3 et alignement vérifiés.
- Certificat de signature identique aux versions précédentes.
- APK compilée, alignée et vérifiée localement.
