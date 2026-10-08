# Changelog

## 1.1.0

- Replaced the previous Telecom/VoIP-mode experiment with a MediaTek-specific HFP Client bridge.
- Sends `bt_wbs=on` before `routeHfpAudio(true)` and `bt_wbs=off` before `routeHfpAudio(false)`.
- Added `versionName=1.1.0` and `versionCode=11`.
- Added an Android Bluetooth application icon.
- Updated the Xposed description to match the MediaTek experiment.
- Replaced the old v1.0.0 distribution APK.

## 1.0.0

- Initial diagnostic build.
- Forced HFP Client connections to `setAudioModeIsVoip(true)`.
- Confirmed Telecom switched to `MODE_IN_COMMUNICATION`, but usable HFP audio was not obtained on the MT6785 test device.
