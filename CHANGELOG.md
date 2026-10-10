# Changelog

## 1.7.8-lab

- D3 now lets Android select its default built-in microphone instead of rejecting phones that expose multiple microphones of the same type.
- Logs candidate and selected microphone IDs, addresses, and product names for route diagnosis; it still verifies the actual route before sending audio and never saves PCM.
- Bumped package version to `versionCode 43` for in-place updates.

## 1.1.1

- Added a custom blue **phone + Bluetooth** application icon.
- Bumped Android package version to `versionName 1.1.1` / `versionCode 12`.
- Kept the exact same `bt_wbs` HFP Client hook behavior as 1.1.0.
- Kept the same 1.1.x signing certificate, allowing an in-place update from 1.1.0.

## 1.1.0

- Replaced the previous Telecom/VoIP-mode experiment with a MediaTek-specific HFP Client bridge.
- Sends `bt_wbs=on` before `routeHfpAudio(true)` and `bt_wbs=off` before `routeHfpAudio(false)`.
- Removed the old `setAudioModeIsVoip(true)` experiment.

## 1.0.0

- Initial diagnostic build.
- Forced HFP Client connections to `setAudioModeIsVoip(true)`.
- Confirmed Telecom switched to `MODE_IN_COMMUNICATION`, but usable HFP audio was not obtained on the test device.
