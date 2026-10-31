# Relais d’appels 1.9.18 — utilisation quotidienne simplifiée

VersionCode 81, Android 11/API 30, paquet `com.hfpvoipfix` inchangé.
Le runtime Magisk 1.9.0-persistent et les cinq cibles LSPosed restent requis.

## Interface

L'application s'appelle désormais **Relais d’appels**. L'écran principal affiche
l'état, Activer/Désactiver, Démarrage automatique et Options. Il ne lance aucune
capture de diagnostic. Les libellés D5/D6, fréquences, profils, phases et contrôles
techniques ne sont plus dans le parcours quotidien.

La sortie par défaut est **Autoradio** (`telecom_aa_control`). Options permet de
choisir **Redmi** (`telecom_duplex`, D5 conservé) ou **Compatibilité**
(`projection_only`). Ces sorties restent des choix explicites : cette version
n'ajoute pas de changement automatique du routage pendant un appel.
Les outils antérieurs sont conservés dans une activité de maintenance facultative.
Le changement de sortie active exige un appel et un nettoyage entièrement terminés.

Les notifications des deux services du relais sont regroupées, avec des textes
simples. La notification de service reste présente : elle n'est pas masquée au
système. La présentation exacte du groupe dépend de MIUI.

## Démarrage

L'utilisateur active une fois le relais après autorisation microphone/root et
configuration LSPosed. Le démarrage automatique est coché par défaut, désactivable.
BOOT_COMPLETED reprend le dernier mode après le premier déverrouillage ;
MY_PACKAGE_REPLACED applique les mêmes conditions. Une installation neuve ne
s'active pas silencieusement. Désactiver le relais empêche aussi sa reprise au boot.

Le récepteur vérifie le consentement enregistré et l'autorisation microphone,
gère un refus système sans crash et ne réarme pas les essais de diagnostic.
Le superviseur existant attend toujours les hooks de version correspondante,
les permissions et un état propre avant de préparer le pont. Le lancement root
du service audio existant reste utilisé pour Android 11.

Le démarrage automatique doit encore être vérifié sur le matériel MIUI ; le
système peut nécessiter l'autorisation de lancement automatique de l'application.
Aucune modification du module natif, de la ROM ou des réglages Bluetooth
persistants n'est ajoutée.

## Diagnostic facultatif

Options → Diagnostic fournit Démarrer, Terminer et Enregistrer dans
Téléchargements. La capture et le relais restent indépendants ; terminer la
capture ne ferme pas le relais autonome. Aucun son n'est enregistré. Les outils
avancés restent disponibles pour les essais, la collecte et les modes de secours.

## Compatibilité AA

Le sélecteur ne bloque plus D6 contrôlable sur l'empreinte d'un seul APK Android
Auto. Les hooks qui utilisent des noms internes AA gardent leur vérification
SHA-256 exacte et restent inactifs sur un autre binaire. Ils ne sont pas adaptés
aveuglément à une nouvelle version. Le filtre Telecom/HFP véhicule conserve son
périmètre matériel et de compte. Le moteur audio et ses protections restent ceux
de 1.9.17 ; seul son habillage quotidien change dans cette version.

Cela ne constitue pas une promesse de portabilité universelle : les protections
et la préparation native ciblent begonia/Android 11/MIUI connu. La compatibilité
avec d'autres autoradios, une autre ROM, le sans-fil et le microphone du véhicule
n'est pas établie. Le retour utilise le microphone du Redmi.

## Vérification

Les fixtures hôte de production testent l'activation explicite, le choix par
défaut, les reprises boot/mise à jour, le refus sans microphone, le choix de ne
pas démarrer automatiquement, les appels actifs, la reconfiguration hors appel,
l'échec de persistance et l'absence de capture imposée. Les tests audio existants
restent requis. Aucun émulateur/adb n'est disponible dans l'environnement ; aucune
validation visuelle ou matérielle n'est revendiquée sur cette base.

Références Android :
- [Stockage et notification après déverrouillage](https://developer.android.com/privacy-and-security/direct-boot).
- [Restrictions des services microphone sous Android 11](https://developer.android.com/about/versions/11/privacy/foreground-services).
