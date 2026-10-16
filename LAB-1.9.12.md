# 1.9.12 — research findings, narrowband D6 and discriminating AA trials

## What the 1.9.11 report establishes

The 2026-10-16/17 report contains an initial 8 kHz SCO session followed by four
16 kHz bridge sessions. At 23:58:32.628 the actual HAL trace records
`hfp_set_sampling_rate=8000`, then `hfp_enable=true`. Our module reports
`negotiated_wbs=false`, skips preparation and refuses with `mtk_wbs_not_pending`.
This is a concrete implementation defect: a negotiated narrowband session cannot
reach the software bridge. The same error persists as the last-error metadata in
later sessions; it is not evidence that those later 16 kHz preparations also fail.

On the four subsequent sessions the 1.9.11 focus policy executes. The actual AA
winner has usage=0, content=0, gain=2. RX data continues, nonzero levels are observed,
nearly all samples are written and the playback head advances. At 23:59:48.175,
for example, phase 3 has read 129280 samples and written 129216. The user's result
is still negative. Removing our application's focus pause is therefore insufficient.
No conclusion about the identity of the signal or acoustic car output follows
from these counters. Return microphone RMS is mostly 0 or 1.

The phone's own policy dump identifies two **different** AA mixes:

| Registered output | Matching usages | AA capture format in this report |
| --- | --- | --- |
| `1506431339:ap:0mixp:0` | MEDIA, GAME | 48000 Hz stereo |
| `1506431339:ap:0mixp:1` | ASSISTANT, NAVIGATION_GUIDANCE | 16000 Hz mono |

The bridge used output 57, the second mix. AA has an active REMOTE_SUBMIX reader
for it. An active reader and advancing writer do not prove that the reader receives
nonzero samples, that AA forwards them, or that the car presents that channel.

## Research and boundaries

Current Android documentation defines REMOTE_SUBMIX as audio captured for remote
presentation, not acoustic playback confirmation. The current microphone API
distinguishes MIC from VOICE_COMMUNICATION: the latter can use echo cancellation
or automatic gain control. Android's input-sharing policy also distinguishes source
privacy and priority. These are grounds for a controlled source comparison, not
proof of the cause of this Redmi's low microphone levels. The old report marks
the microphone client as not silenced; changing capture priority globally is not
justified by it.

The current navigation guide and generic focus lifecycle describe ordinary
navigation/media applications. They do not document using navigation projection
as a replacement for this two-phone HFP call. Android Automotive OS focus matrices
are for a different platform and are not proof of Android Auto head-unit behavior.
Third-party protocol reconstructions were examined but mark some channel-role
enums as unverified. No hardcoded protocol channel number or new proprietary
AA class hook is based on those assumptions.

The exact Android 11 AudioRecord Java and JNI implementations were read from the
official AOSP mirror. JNI reads direct buffers using GetDirectBufferAddress and
capacity, without a Java position argument; it writes from that buffer/slice's base.
The Java comment says position in one place, so the sampler follows the actual
Android 11 JNI path and tests it explicitly. This version-specific source is
intentional for the user's Android 11 device; no new deprecated public API is added.

## Changes

1. **D6 8 kHz:** keep the negotiated 8000/16000 rate. For a D6 narrowband session,
   prepare the already-open transport and paired software ports, set BT_SCO=on
   after native routing returns, align bt_wbs=off and restore the previous flag
   and owned ports at teardown. Wideband still uses on. Unknown negotiation
   fails closed. Never force WBS or renegotiate a codec. D5 and D2/D3 narrowband
   behavior remain unchanged. Actual narrowband duplex on this HAL is unverified.
2. **Observe AA's existing readers:** an AA-process-only hook observes public
   AudioRecord.read overload results for REMOTE_SUBMIX, solely while a valid D6
   native transport lease exists. At most 256 scalar samples are inspected per
   read; only RMS/peak, counts, encoding, format and projection address are logged
   at two-second intervals. Device lookup and log output run on a separate daemon
   worker with a bounded four-entry queue; queued observations retain scalar
   metadata and only a weak recorder reference. Nested overloads count once. Buffers, read return
   values, cursors and limits are not changed, copied for storage, or retained.
   No recorder, shell, packet tap, policy mix or audio grant is created. It has
   no physical microphone observation. The weak-key registry is capped at eight.
3. **Four controlled comparisons in one install:** a new `Comparer les 4 essais AA`
   button arms the following sequence, changing one choice between adjacent calls:

   | Trial | Output usage | Redmi microphone source |
   | --- | --- | --- |
   | 1 | Navigation | VOICE_COMMUNICATION (previous reference) |
   | 2 | Media | VOICE_COMMUNICATION |
   | 3 | Media | MIC, default microphone policy |
   | 4 | Navigation | MIC, same default microphone policy |

   Choices are latched per actual HFP call ID and epoch, not SCO reconnect count.
   Starting audio commits the choice; a reconnect of the same call retains it.
   A failed preparation does not consume an unstarted trial. An IOException
   during comparison closes the streams and waits for a fresh no-call/no-SCO/clean
   status before preparing another call; it never retries within the failed call. A later new call
   after the four trials returns to the normal navigation/reference profile.
   The mode is armed only outside a call and does not choose a winner automatically.
   Regular relay activation resets comparison mode. No reboot is needed between
   trials. The UI and metadata identify each profile.
4. The 1.9.11 bounded **transient** coexistence policy also recognizes this app's
   MEDIA comparison track. Permanent, exclusive and unrelated losses still use
   normal handling. If AA requests permanent focus in the media trial, the relay
   can pause; the new data must determine that rather than fabricate a grant.
   D5 still acquires no new playback focus. No change to Magisk runtime 1.9.0.

## What this version cannot establish alone

An AA read with nonzero sampled RMS establishes nonzero data in that observed
reader, not the identity of the content or delivery to the car. RMS sampling
is bounded and approximate. PCM16 array/direct-buffer and float-array levels are
supported; unsupported formats have inspected=0 and must not be called silent.
If AA uses native reading or another path bypassing the public overloads, absence
of observations is inconclusive. The hook may be unavailable on an OEM/AA build;
its startup and failures are logged. There is no proven direct car-microphone path.
No full-duplex or audible-output success is claimed.

## Verification

Host tests run actual production hooks against narrow Android/Xposed fakes:
D6 narrowband alignment/restoration, retained D2/D5/reference paths, bounded
scalar sampling, native-endian byte offsets, direct-buffer cursor preservation,
nested reads, errors/zero results, lease expiry, D5/source exclusion and address
redaction. Trial tests cover one-variable ordering, same-call SCO reconnect,
new-call advancement, failed start and completion. Existing focus, lifecycle,
probe, storage, privacy, routing and native guard suites remain required.
Android compilation, signature and zip alignment must pass before APK delivery.
These are software checks, not validation on the Redmi or the car.

## Sources checked 2026-10-17 (Europe/Paris)

- User report, local copy `report-1911-latest`: hfp-events.log,
  bridge-events.log, audio-policy.txt and audio-flinger.txt. These establish the
  actual mix criteria, negotiated rates, data counters and negative hardware result.
- Current MediaRecorder.AudioSource reference, updated 2026-08-03:
  https://developer.android.com/reference/android/media/MediaRecorder.AudioSource
- Current input-sharing and Android 11 behavior:
  https://developer.android.com/media/platform/sharing-audio-input
- Current focus lifecycle:
  https://developer.android.com/media/optimize/audio-focus
- Current navigation guidance:
  https://developer.android.com/training/cars/apps/navigation
- AOSP Android 11 AudioRecord Java and JNI, retrieved via GitHub API:
  https://github.com/aosp-mirror/platform_frameworks_base/blob/android11-release/media/java/android/media/AudioRecord.java
  https://github.com/aosp-mirror/platform_frameworks_base/blob/android11-release/core/jni/android_media_AudioRecord.cpp
- Examined with explicit uncertainty, not a Google contract:
  https://github.com/mrmees/open-android-auto/blob/main/docs/channels/audio.md

## Installation and useful next test

APK 1.9.12-lab, versionCode 75. If signatures differ, uninstall the old app,
install, enable all five recommended LSPosed targets and reboot Redmi once.
Keep the existing Magisk 1.9.0 runtime. Authorize the microphone.
Start a report capture outside a call, then press `Comparer les 4 essais AA`.
For this comparison, make all four calls from the Samsung to avoid changing
initiation direction at the same time as the audio profile. Keep each call about
30 seconds; wait for relay readiness between calls without rebooting. Use the
same speech/test conditions, speak near the Redmi, and note which trial gives
received sound and whether the remote party hears the Redmi. Stop and export
the report after trial 4. Do not infer the winning profile from counters alone.
