# Synthèse publique du diagnostic audio — octobre 2026

Cette note conserve les conclusions utiles au projet sans publier les fichiers système privés qui ont servi à l'analyse. Les noms précis de composants vendor, empreintes, adresses mémoire, journaux bruts et identifiants de rapports ont été retirés. Aucun binaire système ni inventaire provenant du téléphone n'est inclus dans le dépôt.

## Résultats conservés

- La présence d'un appel de paramétrage audio et d'un code de retour sans erreur ne suffit pas à prouver que le chemin audio demandé a réellement été activé.
- Les premiers essais ont ouvert une capture RX pendant l'appel, mais n'ont pas produit d'échantillons audio utiles. Ces observations ne démontrent pas une panne matérielle et ne suffisent pas à identifier une cause unique.
- Une capture Bluetooth a montré qu'une liaison synchrone pouvait être établie alors que le chemin de réception Android restait muet. L'absence de paquets audio dans une trace ne permet pas, à elle seule, de conclure que rien n'a circulé par un chemin audio hors HCI.
- Les essais ont ensuite révélé un décalage entre la bande annoncée par la négociation et le mode observé dans le chemin de réception. Les notes de version publiques décrivent les changements et les résultats sans reproduire les détails internes des bibliothèques système.
- Après un ajustement du chemin RX, l'utilisateur a confirmé une restitution claire. Le microphone TX fait l'objet d'un essai séparé ; sa validation doit rester distincte de celle de la réception.

## Limites et précautions

Ces résultats viennent d'un seul appareil et d'essais expérimentaux. Ils ne prouvent pas que le même comportement existe sur d'autres ROM ou appareils. Ne pas modifier les fichiers système, écrire directement dans le mixeur ou accéder aux flux PCM bruts sur la base de cette synthèse.

Les rapports bruts, captures Bluetooth et fichiers système peuvent contenir des identifiants et des détails propres à l'appareil. Ils doivent rester hors du dépôt public et être examinés avant tout partage. Cette synthèse est destinée à documenter l'évolution du projet, pas à remplacer les données privées de diagnostic.
