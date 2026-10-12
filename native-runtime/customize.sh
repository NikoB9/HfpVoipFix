# Only install from the running, rooted device. Default Magisk extraction.
[ "$BOOTMODE" = true ] || abort "Installer uniquement depuis l'application Magisk."
[ "$API" = 30 ] || abort "Android 11 requis."
[ "$(getprop ro.build.fingerprint)" = 'Redmi/begonia/begonia:11/RP1A.200720.011/V12.5.6.0.RGGIDXM:user/release-keys' ] || abort "ROM différente : installation refusée."
check_hash() {
  [ -f "$1" ] || abort "Fichier absent : $1"
  actual=$(sha256sum "$1") || abort "Empreinte illisible : $1"
  [ "${actual%% *}" = "$2" ] || abort "Empreinte différente : $1"
}
current=$(sha256sum /vendor/lib/hw/audio.primary.mt6785.so) || abort "HAL illisible"
case "${current%% *}" in
6ca4f18a2652f5ecd0faa7e151f1637e4943da7bfe4256de72116b5c51b1b809|3387c3b7e9bc593728cd13c20e911007befb8fa25ea1dc8338645a487ec09ddc) ;;
*) abort "HAL incompatible";;
esac
check_hash /vendor/etc/audio_policy_configuration.xml '2f5f9e71334418d643b7441ba8948bf21c2f4032b81244a60563b64cab37e5da'
check_hash "$MODPATH/system/vendor/lib/hw/audio.primary.mt6785.so" '3387c3b7e9bc593728cd13c20e911007befb8fa25ea1dc8338645a487ec09ddc'
# Verify the active loaded HAL architecture rather than infer it from the CPU.
found32=0
for maps in /proc/[0-9]*/maps; do
  [ -r "$maps" ] || continue
  if grep -q '/vendor/lib64/hw/audio.primary.mt6785.so' "$maps"; then
    abort "HAL audio 64 bits actif : candidat 32 bits refusé."
  fi
  if grep -q '/vendor/lib/hw/audio.primary.mt6785.so' "$maps"; then found32=1; fi
done
[ "$found32" = 1 ] || abort "HAL audio 32 bits chargé non confirmé."
# Reject another enabled overlay of either target, including staged modules.
for other in /data/adb/modules/* /data/adb/modules_update/*; do
  [ -d "$other" ] || continue
  [ "${other##*/}" = hfp_native_split_trial ] && continue
  [ -f "$other/disable" ] && continue
  [ -f "$other/remove" ] && continue
  for target in system/vendor/lib/hw/audio.primary.mt6785.so system/vendor/etc/audio_policy_configuration.xml vendor/lib/hw/audio.primary.mt6785.so vendor/etc/audio_policy_configuration.xml; do
    [ ! -e "$other/$target" ] || abort "Un autre module remplace déjà un fichier audio : ${other##*/}"
  done
done
# Inherit the contexts of the exact source files, without SELinux rule changes.
inherit_context() {
  context=
  for word in $(ls -Zd "$1"); do
    case "$word" in u:object_r:*:s0) context=$word; break;; esac
  done
  [ -n "$context" ] || abort "Contexte SELinux illisible : $1"
  set_perm "$2" 0 0 0644 "$context"
}
set_perm_recursive "$MODPATH/system/vendor" 0 0 0755 0644 u:object_r:vendor_file:s0
inherit_context /vendor/lib/hw/audio.primary.mt6785.so "$MODPATH/system/vendor/lib/hw/audio.primary.mt6785.so"
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/post-fs-data.sh" 0 0 0755
ui_print "Relais natif persistant 1.9.0. Redemarrer apres installation APK/LSPosed."
ui_print "Pour revenir au son d'origine : desactiver ce module dans Magisk et redemarrer."
