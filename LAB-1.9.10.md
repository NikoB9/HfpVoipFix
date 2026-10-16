# 1.9.10 — D6 native SCO preservation and documented projection focus

## Evidence and retained history

- D5's local duplex path is the 1.9.0 reference. Keep its DSP, device choices,
  Telecom call focus and normal-mode behavior. D6 remains separate.
- The 1.9.4 two-SCO experiment redirected master TX to the car. Do not revive it.
- 1.9.7 removed Android Auto's local-SIM refusal on the user's hardware. Retain
  that bounded readiness proxy and the lighter diagnostic collection.
- 1.9.8 obtained navigation projection focus on the first call but did not yield
  audible car output. Later calls lacked the master transport; do not label
  submix writes as playback confirmation.
- 1.9.9's route API runs, with two master SCO sessions. Both receive zero-valued
  samples, acquire focus, then lose it about 20 ms later as gearhead requests
  GAIN. Every captured RX write count stays zero.
- Newly cross-checked AudioFlinger evidence from that same 1.9.9 report:
  Bluetooth UID 1002 sets BT_SCO=on at 19:53:59.728 and 19:54:34.112; system
  UID 1000 sets BT_SCO=off at 19:54:04.146 and 19:54:34.222, before the respective
  zero-valued capture windows. AOSP AudioDeviceBroker.setBluetoothScoOn writes
  this HAL flag separately from its policy force-use updates. This supports the
  native-flag hypothesis; it does not by itself prove the cause of all zero data.

## D6 changes

1. Request navigation playback focus with AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
   as the current Android Auto navigation guide specifies, using the same
   USAGE_ASSISTANCE_NAVIGATION_GUIDANCE/SPEECH attributes as the track. Do not
   retry focus or ignore LOSS. Preserve a callback received before requestAudioFocus
   returns, and reject reuse after close. D5 acquires no new playback focus.
2. Publish a five-second root lease only from a token-correlated current 1.9.10
   Bluetooth status: one active HFP call, one native SCO, confirmed Telecom,
   registered software ports, native SCO preparation and clean D6 state. Timestamp
   is the module's monotonic status_at, not the time of later publication. Include
   only epoch, SCO sequence and numeric port IDs. Confirm property publication
   on the existing root pipe; no extra root processes or Telecom dump polling.
3. In the framework/system-server scope, observe AudioDeviceBroker's SCO flag
   changes. The callback only records metadata. A dedicated 500 ms worker checks
   the lease, model/Android/native-runtime, actual matching input/output port IDs
   and addresses, and AudioService MODE_NORMAL. If the broker requested OFF,
   preserve BT_SCO=on for the already open master, including bootstrap after an
   earlier reset. Never change Bluetooth connection state, policy forced routes,
   mute state, codec, or module binaries. Do not call AudioService/HAL from the
   broker hook or hold its locks. A changed broker snapshot cancels the pending
   reconciliation. Reconcile once per actual broker reset, with at most two HAL
   failures per broker revision/session.
4. Restore the broker's OFF request on lease expiry/removal or missing/mismatched
   ports if this guard owns the preservation. A broker ON or non-NORMAL audio mode
   relinquishes ownership without issuing a conflicting HAL write. D5 is skipped
   before service queries when there is no owned cleanup to complete. Native HFP
   teardown retains its existing cleanup. No new SCO is opened by this guard.
5. Log receive amplitude during preparation phases too, plus the actual projected
   output and preservation decisions. Show a suspended-output status after focus
   loss rather than claiming active output. No PCM recordings or volume changes.

## Verification

The host workflow executes the production system observer, lease and controller
against fake broker/service/ports/HAL implementations: no work in the callback,
bootstrap/reset reconciliation, lease renewal/expiry/future timestamps, missing
and wrong-address ports, D5 and non-NORMAL modes, broker state changes during a
poll, HAL failure limits and cleanup. Existing production fixtures cover D6 route
and native-recovery guards, status availability with blocked Telecom, D5/call
lifecycle, AA readiness and all focus losses, including loss during acquisition.
Root-pipe tests cover lease acknowledgement/refusal and shell-input rejection.
The full suite, Android compilation, signature and alignment are required.

These are host/API-contract tests, not hardware validation or proof of usable
simultaneous transports. The proprietary Android Auto focus/transport behavior
and car output still require an on-device test. Its microphone remains unsupported;
the return microphone is the Redmi's. Keep native Magisk runtime 1.9.0 and the
same LSPosed scopes, including framework and Android Auto. APK versionCode 73.

## Primary sources checked 2026-10-16

- Current Android Auto navigation guidance (updated 2026-09-08), audio section:
  https://developer.android.com/training/cars/apps/navigation#provide-audio-guidance
  specifies NAVIGATION_GUIDANCE plus GAIN_TRANSIENT_MAY_DUCK.
- Android 11 AudioDeviceBroker.setBluetoothScoOn, HAL flag and policy distinction:
  https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android11-release/services/core/java/com/android/server/audio/AudioDeviceBroker.java
- Current audio-focus lifecycle guidance:
  https://developer.android.com/media/optimize/audio-focus

The framework source is intentionally Android 11 to match the validated phone;
newer API-level Bluetooth routing replacements cannot simply be applied to this
Android 11 native HFP Client role.
