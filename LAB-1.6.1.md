# HfpVoipLab 1.6.1 — fiabiliser l'armement D2

Le rapport 1.6.0 montre une demande d'armement, une erreur de handshake puis la fermeture du pont avant le début de l'appel. Aucun événement d'ouverture, de route confirmée ou de statistiques n'est présent. Ce rapport ne teste donc pas la capacité du pont à transporter la voix. La cause précise de l'erreur ne peut pas être confirmée ; les horodatages et identifiants de session ont été retirés.

Correctif : canal root persistant pendant l'armement et l'essai, contrôle uid une seule fois, queue de sortie bornée, dernières 120 lignes logcat, jeton renouvelé à chaque interrogation et correspondance exacte. Repli de la sonde UI également à 120 lignes plutôt que 4 000. Erreurs root, propriété, fermeture, saturation et délai détaillées ; latence et reprises journalisées.

Avant ouverture audio uniquement, une erreur de suivi peut être retentée ; attendre « D2 armé » avant d'appeler. Attente maximale 10 minutes. Après ouverture : garde de fraîcheur 4 secondes et arrêt sur perte de suivi conservés. Aucun redémarrage automatique du pont pendant un appel, aucune réutilisation d'un état ancien. Les hooks 1.6.0, RxGate et les chemins AudioRecord/AudioTrack ne sont pas modifiés.

Installation par-dessus 1.6.0, même certificat. Aucun reboot requis si le module chargé annonce 1.6.0. Hors appel : vérifier/appliquer D2, démarrer capture, armer et attendre « D2 armé », appeler un numéro de test depuis l'appareil source en Bluetooth, raccrocher, noter réception, microphone non évalué, terminer et vérifier le rapport avant tout partage.

Validation : échanges à jetons distincts sur un processus root simulé, refus de modification des propriétés, fermeture du processus, rejet des préfixes de jetons et tests RxGate. Compilation, signature, manifeste, DEX et assets vérifiés. Tests sur hôte ; ces changements traitent la fragilité du suivi et ne prouvent pas encore le fonctionnement du SCO sur l'appareil.
