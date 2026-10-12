#!/system/bin/sh
MODDIR=${0%/*}
valid=true
[ "$(getprop ro.build.fingerprint)" = 'Redmi/begonia/begonia:11/RP1A.200720.011/V12.5.6.0.RGGIDXM:user/release-keys' ] || valid=false
sha=$(sha256sum /vendor/lib/hw/audio.primary.mt6785.so);case "${sha%% *}" in
6ca4f18a2652f5ecd0faa7e151f1637e4943da7bfe4256de72116b5c51b1b809|3387c3b7e9bc593728cd13c20e911007befb8fa25ea1dc8338645a487ec09ddc) ;; *) valid=false;; esac
sha=$(sha256sum /vendor/etc/audio_policy_configuration.xml);[ "${sha%% *}" = 2f5f9e71334418d643b7441ba8948bf21c2f4032b81244a60563b64cab37e5da ] || valid=false
sha=$(sha256sum "$MODDIR/system/vendor/lib/hw/audio.primary.mt6785.so");[ "${sha%% *}" = 3387c3b7e9bc593728cd13c20e911007befb8fa25ea1dc8338645a487ec09ddc ] || valid=false
if [ "$valid" != true ]; then
 touch "$MODDIR/skip_mount" "$MODDIR/disable"
 log -p e -t HfpNativeSplit "Runtime incompatible: overlay skipped, module disabled."
fi
