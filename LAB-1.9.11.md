# 1.9.11 — bounded coexistence between the D6 relay and Android Auto

## Observed on 1.9.10, not inferred from host tests

The user's four-call report (2026-10-16, local log timestamps 23:23–23:25)
contains four master SCO sessions. First-phase RX RMS values are 4970, 6372,
5243 and 5464. All four select registered projection output 48. AA requests
GAIN_TRANSIENT (2), flags 1, at 23:24:00.705, 23:24:40.520, 23:25:11.936 and
23:25:25.452. Relay LOSS_TRANSIENT (-2) follows at 23:24:00.713,
23:24:40.524, 23:25:11.939 and 23:25:25.455. RX continues after the relay
pauses and stops writing it. There are occasional regains close to hangup.
The user heard brief car output on calls 2 and 4; route/write counters alone
cannot prove acoustic output on calls 1 and 3, or identify the recorded content.

The native preservation hook executes on every session and restores OFF after
each teardown. This confirms hook execution, not that it alone caused the
nonzero reception: Bluetooth already enabled BT_SCO before the first nonzero
RX window. Microphone levels are very low; this version does not claim TX works.

## Change and explicit experimental assumption

The standard focus lifecycle remains correct for independent audio sources.
Here the relay supplies content to a projection owner which can preempt it.
**Treating these two focus owners as cooperating parts of this D6 session is
an experimental custom policy, not a documented Android Auto call-audio API.**
AA's proprietary reason for the request, and its audio attributes, were not
included in the previous report. We do not claim to have identified its internal
focus handler or distinguish every AA operation from those old logs.

The new system-server hook observes Android 11 FocusRequester.handleFocusLoss.
It suppresses only a transient loss (-2) from a GAIN_TRANSIENT (2) AA winner
to this application's NAVIGATION_GUIDANCE/SPEECH/MAY_DUCK owner, previously
without a loss, in the same MediaFocusControl instance. AA's request still runs
normally, becomes the top of the original stack and returns the original result.
No focus grants are fabricated and the application does not request focus again
after a loss. Its existing listener still pauses for losses outside this gate.

Eligibility also requires the current root-published, token-correlated, five-second
native SCO proof: one active HFP master, matching model/Android/native-runtime,
clean D6 state, plus actual paired port IDs/addresses and MODE_NORMAL validated
by the native worker less than 1.2 seconds ago. Port IDs and epoch/sequence must
match that evidence. No AudioService, HAL, Bluetooth, shell, or port enumeration
runs from the focus hook or while holding the framework focus lock.

Allowed AA usages are DEFAULT (0), MEDIA (1), VOICE_COMMUNICATION (2) and
NAVIGATION_GUIDANCE (12), because the old focus history did not expose the
actual attribute. This is a bounded choice, not evidence that AA uses any
specific one. The new log includes its actual usage/content/gain/loss, live
evidence result and coexistence decision. Assistant, accessibility, notification,
alarm and other usages, permanent/exclusive requests, lock flags, force-duck,
Telecom/other winners and already-lost relay owners use normal handling.

A 500 ms worker takes the original MediaFocusControl lock. If a shared pair is
still present with AA on top, but its transport proof/worker evidence expires,
is removed or changes call, it invokes the original transient loss. Normal
abandon/death, an independent winner or a loss already delivered removes the
pair without invoking a released owner. Registry capacity is four. Installation
checks all required fields/methods and does not enable the hook until its cleanup
worker is scheduled. Unsupported contracts pass through and log failure.

D6 waits up to six seconds for native preparation evidence before acquiring
playback focus; the first 1.9.10 handshake could precede native_sco=true.
Session receive/transmit totals are now separate from phase counters. Written
samples still do not mean audible output. No PCM is stored.

## Retained behavior

D5's local audio path and DSP/device/source settings remain the reference from
1.9.0. Keep Magisk native runtime 1.9.0. Retain AA SIM readiness, the D6 Telecom
route/focus-lock translation, master SCO recovery and HAL preservation from
previous versions. Do not reopen the rejected two-SCO vehicle experiment.
The car microphone is unsupported; return audio still uses the Redmi microphone.
No microphone source/gain change is bundled with this focus experiment.

## Verification

The new host fixture runs the production focus hook, policy and native evidence
worker. It exercises Android 11 stack order (loss propagation before push), normal
AA grants/abandon/regain, lease expiry/removal/new session, local call mode,
missing/changed ports, stale worker evidence, locked/permanent/exclusive requests,
unrelated owners/usages, force-duck and previously lost owners. It checks no HAL
writes occur under the focus hook. Existing tests cover D5, emergency/local SIM/
multiple calls, route API failure, pending SCO recovery, real probe acknowledgments,
privacy/storage, original focus listener and native lease cleanup. Full suite,
Android compilation, signature and zip alignment are required for delivery.

These checks are not hardware or car validation. Audible continuity and microphone
delivery require another on-device report. There is no basis to promise success.

## Sources checked 2026-10-16

- Official current focus lifecycle guidance:
  https://developer.android.com/media/optimize/audio-focus
- Official current AudioAttributes definitions:
  https://developer.android.com/reference/android/media/AudioAttributes
- AOSP official mirror, Android 11 FocusRequester (retrieved through GitHub API):
  https://github.com/aosp-mirror/platform_frameworks_base/blob/android11-release/services/core/java/com/android/server/audio/FocusRequester.java
- AOSP official mirror, Android 11 MediaFocusControl:
  https://github.com/aosp-mirror/platform_frameworks_base/blob/android11-release/services/core/java/com/android/server/audio/MediaFocusControl.java

The Android 11 implementation is intentionally the phone's platform version.
The new change adds no deprecated public API; its private framework contract is
version-specific and checked at installation. Newer Android routing APIs do not
replace this Android 11 native HFP-client transport. APK versionCode 74.

## Installation and test

If signatures differ, uninstall the older application and install the signed
1.9.11 APK. Select all five recommended LSPosed targets: Bluetooth
(com.android.bluetooth), call management (com.android.server.telecom), android,
system, and Android Auto (com.google.android.projection.gearhead). Reboot Redmi
once after changing the APK/scopes. Keep the current native Magisk module.
Activate the AA relay and capture four calls in the same order as before,
about 30 seconds each. Note car/Samsung/Redmi sound and when it cuts out;
speak near the Redmi separately to assess TX. Export the report afterwards.
