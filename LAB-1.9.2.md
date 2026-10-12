# 1.9.2 — ouverture du canal audio du maître

Le rapport 1.9.1 montre un appel HFP actif mais sco=0, sco_seq=0, rate=0 et soft_ports=false pendant toute la capture. Le routage de projection ne peut donc pas être évalué sur cet essai. Telecom utilise déjà le compte HFP sortant et sélectionne une route Bluetooth côté esclave ; les traces ne prouvent pas la cause de l’absence du canal maître.

La nouvelle supervision Bluetooth demande HeadsetClientService.connectAudio pour un unique appel HFP actif et confirmé par Telecom, seulement si aucun SCO maître n’est ouvert. Elle attend deux secondes puis limite les demandes à trois par appel, espacées de cinq secondes. Elle vérifie l’unique appareil HFP Client connecté et son égalité avec l’appareil de la machine d’état de cet appel. Un audio en cours de connexion ou déjà connecté n’est pas interrompu. Une réponse true signifie seulement que la commande a été mise en file ; SCO_BEGIN et le handshake du pont restent les preuves nécessaires.

Aucun changement de transport USB/Wi-Fi, du HAL, du XML ou des connexions Bluetooth autoradio n’est ajouté. Le runtime natif reste 1.9.0-persistent. Le routage média de projection 1.9.1 est conservé, mais son résultat et le micro autoradio restent à valider.

Le diagnostic ne traite plus l’absence du processus principal Android Auto comme une erreur de collecte : il conserve la liste des périphériques. Ce pid ne permet pas à lui seul de conclure que la projection est arrêtée.

Source ciblée Android 11 (version du téléphone, et non recommandation générale pour un nouvel appareil) : https://android.googlesource.com/platform/packages/apps/Bluetooth/+/refs/heads/android11-release/src/com/android/bluetooth/hfpclient/HeadsetClientService.java

Validation : tests hôtes de la temporisation/limite/réinitialisation et compilation Android par CI. Les connexions simultanées restent un test matériel.
