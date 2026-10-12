# 1.9.0 — relais D5 autonome minimal

D5 1.8.4 est confirmé par le retour utilisateur dans les deux sens, avec deux appels enregistrés sans erreur. Les démarrages capricieux et la consommation longue durée restent à qualifier.

## Usage

La nouvelle page d’accueil propose Activer/Désactiver le relais, Téléphone, configuration initiale des comptes et diagnostic. Une fois le relais activé et RECORD_AUDIO accordé, un service superviseur persistant attend les appels sans dépendre de CaptureService. BOOT_COMPLETED et MY_PACKAGE_REPLACED relancent le superviseur seulement si l’utilisateur a activé le relais. Il prépare D5 hors appel, après contrôle de la version, des hooks et du nettoyage, puis démarre RxBridgeService via root. Les services ont une notification et le relais peut être désactivé. Le diagnostic reste facultatif ; terminer une capture ne coupe plus le relais autonome. Les modes manuels sont bloqués pendant le relais.

Android 11 empêche normalement le microphone d’un FGS démarré en arrière-plan. ActiveServices.shouldAllowWhileInUsePermissionInFgsLocked fait exception pour ROOT_UID/SYSTEM_UID/SHELL_UID. Le superviseur utilise explicitement am start-foreground-service via root, après permission micro. Ce comportement AOSP reste à vérifier sur MIUI ; aucune permission n’est accordée silencieusement.

Source : https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android11-release/services/core/java/com/android/server/am/ActiveServices.java

La limite d’attente de 10 minutes est supprimée uniquement pour le relais autonome. Les contrôles de fraîcheur, identité, mode et routes audio de 1.8.4 sont conservés. Après une erreur, le superviseur retente uniquement hors appel et après nettoyage. Les échecs root ralentissent les tentatives à 15 secondes. Un processus tué reste un cas MIUI à tester ; START_STICKY et le démarrage boot ne garantissent pas l’absence de restrictions constructeur.

## Correctif natif persistant

Le ZIP Magisk réutilise strictement le HAL 1.7.22 validé (SHA256 3387c3b7e9bc593728cd13c20e911007befb8fa25ea1dc8338645a487ec09ddc), sans nouvelle modification binaire/XML. Il conserve l’ID hfp_native_split_trial et remplace le module d’essai. Il accepte l’original ou le HAL validé déjà monté lors de l’installation. Les contrôles ROM/XML/payload sont refaits au boot ; un échec crée skip_mount et disable. Le service confirme le HAL monté avant le marqueur 1.9.0-persistent. Il ne se désactive plus après chaque boot. Pour revenir au HAL original : désactiver le module dans Magisk et redémarrer. Les binaires et ZIP privés ne sont pas publiés dans ce dépôt, uniquement les scripts.

## Installation et essais

Décompresser le paquet. Installer l’APK (désinstaller l’ancienne si la signature diffère), garder Bluetooth/Gestion des appels/Cadre du sous-système dans LSPosed, installer le ZIP natif dans Magisk, redémarrer. Ouvrir l’application, autoriser le microphone et root puis Activer le relais. Activer/choisir le compte Samsung/HFP dans Téléphone si nécessaire. Sur MIUI, vérifier l’autorisation de démarrage automatique et les restrictions batterie si le service est arrêté par le système.

Tester : appel sortant/entrant sans capture ; écran éteint ; second appel ; reboot sans réarmer Magisk ou démarrer une session ; bouton Désactiver respecté après reboot. Si problème, Diagnostic puis démarrer une capture pendant le relais et exporter après les appels.

Les tests hôtes vérifient les gardes de préparation ; la CI compile l’APK avec avertissements de dépréciation. La permanence Magisk, le microphone après boot, MIUI et l’autonomie batterie restent à valider sur appareil.

## Android Auto

Aucune dépendance USB/Wi-Fi n’est introduite dans le cycle d’appel. L’étape suivante devra adapter les endpoints à ceux de la voiture et tester USB **et** sans fil. La présente version reste haut-parleur/micro du Redmi ; elle ne déclare pas Android Auto compatible. Le transport de projection ne décidera pas de l’architecture du relais.
