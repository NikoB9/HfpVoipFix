# Changelog

## 1.9.5

- Use a projected speech-channel experiment in D6 instead of bridging two SCO devices.
- Scope Telecom vehicle-SCO suppression to one foreground non-emergency HFP D6 call.
- Keep D5 audio, confirmed capture and native runtime 1.9.0; versionCode 68.

## 1.9.4

- Confirm capture startup with repeated markers; isolate all session diagnostics and preserve failure metadata.
- Add bounded vehicle HFP audio requests and state monitoring in D6 only.
- Read master/local network indicators without changing them; include Bluetooth diagnostics.
- Keep D5 audio and native runtime 1.9.0 unchanged; Android versionCode 67.

## 1.9.3

- Keep the 1.9.0 local D5 audio path separate from experimental D6 car routing.
- Add local/Android Auto relay buttons and a D5/D6 selector.
- Resolve master SCO ports by identity and a distinct vehicle pair; detect dynamic projection mixes without the main-process PID.
- Keep native runtime 1.9.0 unchanged; Android package versionCode 66.

## 1.9.1

- Route D5 reception through Android Auto's active media projection mix while preserving the validated speaker route outside projection.
- Select the single native HFP Client account as Telecom's outgoing default while D5 is active.
- Add Telecom and projection device snapshots to diagnostic reports.
- Bump the Android package to `versionName 1.9.1-lab` / `versionCode 64`; the persistent native runtime remains 1.9.0.

## 1.7.18-lab

- D4 now logs both actual routes when MIUI changes them, reasserts the preferred speaker/SCO output routes briefly, and waits up to three continuous seconds for both paths to recover.
- Audio remains gated unless both RX and microphone paths are currently routed to their requested devices. A persistent route conflict stops the bridge without forwarding audio on the wrong route.
- Bumped package version to `versionCode 53`.

## 1.7.17-lab

- D4 starts both input captures before activating the two output tracks. The 1.7.16 report showed MIUI redirecting the RX output to SCO during output-only stabilization, before captures opened.
- Kept `USAGE_MEDIA`, the media-volume telemetry, explicit device preferences, and continuous route checks. This is a routing experiment; speaker audio still needs on-device confirmation.
- Bumped package version to `versionCode 52`.

## 1.7.16-lab

- Reverted D4 RX playback to `USAGE_MEDIA`: the 1.7.15 report showed MIUI rerouting the RX and TX communication tracks, so output routes never stabilized and capture never began.
- Added D4 media-volume telemetry and an in-app instruction to adjust the media volume for speaker playback.
- Bumped package version to `versionCode 51`.

## 1.7.15-lab

- D4 receive playback now uses Android's voice-communication volume group while retaining the explicit speaker route. The report showed the media stream at volume index 3/15 and the active RX track attenuated by 48 dB, matching the silent speaker report.
- Bumped package version to `versionCode 50`.

## 1.7.14-lab

- D4 test now stabilizes SCO and speaker outputs before starting either input capture.
- Explicitly requests the built-in bottom microphone for the VOICE_COMMUNICATION input.
- Logs startup order and microphone candidate; PCM remains unsaved.

## 1.7.13-lab

- D4 now requests `VOICE_COMMUNICATION` for the Redmi microphone input. The provided MIUI audio policy exposes a separate `voip_tx` input profile, while the D4 RX and the previous D4 mic source both used `primary input` (single active stream). This is a targeted experiment; it is not yet confirmed to select `voip_tx` or to carry audio on the device.
- D4 logs the requested source in `OPEN` and the active recording source/silencing state in `INPUT_CONFIG`; Android/MediaTek input selection and actual two-way audio still need device validation.
- Bumped package version to `versionCode 48`.

## 1.7.12-lab

- D2/D3/D4 arming now requires a fresh Bluetooth status confirming the matching requested mode, idle call state, current module version, and required hooks. A stale 1.7.10 module or an unapplied D4 mode is rejected before the bridge service starts, with a visible instruction.
- Bumped package version to `versionCode 47`.

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
