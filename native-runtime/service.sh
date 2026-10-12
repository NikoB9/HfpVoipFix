#!/system/bin/sh
MODDIR=${0%/*}
[ ! -f "$MODDIR/skip_mount" ] || exit 0
sha=$(sha256sum /vendor/lib/hw/audio.primary.mt6785.so)
if [ "${sha%% *}" = 3387c3b7e9bc593728cd13c20e911007befb8fa25ea1dc8338645a487ec09ddc ]; then
 setprop debug.hfpvoipfix.native_split 1.9.0-persistent
 log -p i -t HfpNativeSplit "Persistent validated overlay active."
else
 touch "$MODDIR/disable"
 setprop debug.hfpvoipfix.native_split runtime-inactive
 log -p e -t HfpNativeSplit "Persistent overlay not confirmed; disabled for next boot."
fi
