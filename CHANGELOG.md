# Changelog

## 1.7.11-lab

- Added D4 experimental full duplex: simultaneous SCO playback to the Redmi speaker and Redmi microphone transmission to SCO. Both routes must be confirmed before either stream is forwarded; either route change stops the bridge. PCM is not saved.
- D4 requires the same explicit per-call arm and foreground notification as D2/D3 for this validation build.
- Bumped package version to `versionCode 46`.


## 1.7.10-lab

- Fixed a D3 crash after the microphone and SCO route were confirmed: the bridge used `input.getId()` even though D3 intentionally lets Android select the default microphone (`input` is null). It now checks the already confirmed `inputId`.
- Bumped package version to `versionCode 45` for an in-place update.


## 1.7.9-lab

- Fixed a race where MIUI could return a null routed microphone between two route lookups, crashing the D3 bridge before it could confirm the route.
- D3 now evaluates one input/output route snapshot per check and safely waits for a later snapshot when a device is temporarily unavailable.
- Bumped package version to `versionCode 44`.

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
