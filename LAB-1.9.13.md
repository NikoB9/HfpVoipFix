# 1.9.13 — one-call AA isolation, not a claimed acoustic fix

## Evidence from the newly supplied report

Read on 2026-10-20, not inferred from older reports. Input archive:
`HFP-Rapport(20261020-202327).zip`, SHA-256
`d8de24885c658001fd34cb27f7623b03b2faddacb5faa6fc4c54d57fc4c02976`.
Private raw logs are not committed. The table uses the report's displayed local
clock (UTC+02); bridge epoch timestamps corroborate these times.

| HFP call/session | Initiation evidence | RX only | Microphone starts | TX starts | AA navigation reads after both started |
| --- | --- | --- | --- | --- | --- |
| 1 / 25646:1 | Telecom CREATED by gearhead; GH.CallManager placeCall | 22:07:12.388 | 22:07:13.475 | 22:07:14.526 | At 22:07:16.210: RMS 2107; at 22:07:22.349: RMS 1843; later nonzero again |
| 2 / 25646:2 | Outgoing call discovered as unknown by HFP; originating UI not established | 22:07:37.276 | 22:07:38.367 | 22:07:39.415 | At 22:07:41.232: RMS 2101; at 22:07:47.371: RMS 1398; later nonzero again |
| 3 / 25646:3 | Telecom CREATED by gearhead; GH.CallManager placeCall | 22:08:03.800 | 22:08:04.877 | 22:08:05.955 | At 22:08:07.724: RMS 2054; at 22:08:13.819: RMS 1890 |

All three use 16000 Hz and negotiated WBS=true, paired master SCO ports, native
preparation and MODE_NORMAL. The actual codec bits over the air are not captured.
`D6_PROFILE` is **comparison=false, trial=1, usage=12, mic_source=7** on every
call. The four-profile comparison was not executed. Calls 1 and 3 are verified
as AA-initiated; call 2's originating user interface cannot be established.

All three open RX to the navigation projection output id=60,
`1506431339:ap:0mixp:1`; the Redmi bottom microphone is id=15. Master SCO
input/output pairs are 70/74, 90/92 and 110/114. There is no car microphone bridge.

The AA observer did load (`hook_ready=true`, seven overloads), including the
projection process that emits the measurements. Navigation reader id=49 reports
26 sampled windows: 21 have RMS > 0 (ranges by call 0–2372, 0–2928, 1–2212).
The media reader id=45 also runs and the sampled windows are zero; this is not
an executed media-output trial. Sampling does not establish content identity.

Focus coexistence executes for AA usage=0/content=0/gain=2, at 22:07:12.958,
22:07:37.849 and 22:08:04.343. These events happen about 0.57–0.68 seconds after
RX begins, before the microphone starts. The bridge does not pause its RX output
at those events. The AA navigation reader still contains nonzero samples after
the microphone and TX start. RX track heads advance and writes continue.
Therefore a repeatable complete silencing of that observed reader on microphone
start, or a repeat of the old app-side focus pause, is not supported by this ZIP.
A head-unit/projection-side suppression after capture remains possible, not proven.
There is no synchronized acoustic marker for the reported one second of sound:
its exact beginning/end cannot be reconstructed from software logs alone.

The first two bridges finish at 22:07:32.964 and 22:07:58.002; the third at
22:08:18.610. Totals RX read/written/dropped: 318720/317640/1080,
321280/320776/504 and 224960/213960/11000. The third includes route loss during
teardown; it does not show that the early audible cutoff coincided with a route
loss. Microphone TX levels are generally very low, with occasional nonzero
windows. The end dump lists AEC/NS/AGC on VOICE_COMMUNICATION. That is an observed
configuration, not proof those effects cause the weak TX or car silence.

## Why this change

One-second RX/microphone windows are too short to relate an acoustic event to
microphone capture, TX playback or AA's focus response. Those changes are close
together, while AA level reports aggregate about two seconds. The old logs also
lack the observed AA recorder's own silencing state. A new HAL, focus or privacy
policy intervention cannot be justified from the data. This release fixes these
measurement/protocol deficiencies and the microphone source label (previously
hardcoded VOICE_COMMUNICATION even for MIC comparison profiles). It does **not**
claim to fix audible car output.

## Optional single-call experiment

New button: **Diagnostiquer la coupure AA · 1 appel**. It selects D6 and excludes
the four-call comparison. Each stage lasts eight seconds, then all audio objects
are released and the service waits for hangup. It does not terminate the phone
call, retry HAL flags, renegotiate codecs or start a second SCO transport.

| Stage | Projection output | Redmi capture | Master TX output |
| --- | --- | --- | --- |
| 1 | Navigation | Absent | Absent |
| 2 | Navigation | VOICE_COMMUNICATION | Absent |
| 3 | Navigation | Same recorder | SCO |
| 4 | Navigation | Same recorder | Removed |
| 5 | Navigation | Removed | Absent |
| 6 | Media | Absent | Absent |
| 7 | Media | VOICE_COMMUNICATION | Absent |
| 8 | Media | Same recorder | SCO |

The SCO RX recorder remains the same throughout. Each adjacent stage changes
one endpoint choice. The navigation→media transition **necessarily also restarts
the playback track and focus request**; an improvement then cannot by itself
isolate usage from the restart. Other transitions do not rebuild RX playback or
focus. MIC vs VOICE_COMMUNICATION is not changed in this experiment.

`PHASE_BEGIN` includes output usage, elapsed time, active resources and wall time.
Notifications display stage/label/remaining time. Silence, missing PCM and route
conflicts are observed through the bounded sequence instead of aborting before
rollback; actual forwarding still requires the expected routes, current HFP
identity and fresh status. An API error or ungranted focus closes audio. A new
SCO identity ends the sequence. A started diagnostic is latched by HFP epoch/call
ID; the same call cannot restart it after SCO reconnection. Another call can run
a fresh diagnostic while this option remains selected. Regular activation clears
this option. D5 and regular D6 retain the original 1s/1s/continuous schedule.

The AA observer adds read-window start/end elapsed timestamps and queries its own
`getActiveRecordingConfiguration().isClientSilenced()` on the logging worker.
Null configuration or OEM error is **reader_silenced=unknown**, not false.
`metadata_at_elapsed_ms` identifies when that state was queried; it is not a
claim about the entire preceding RMS window. Observation remains AA-only,
D6-lease gated, bounded and scalar-only. No buffers/audio recording are saved.

## Sources rechecked 2026-10-20

- New user-supplied archive above: hfp-events.log, bridge-events.log,
  aa-events-0.log, telecom-state.txt, audio-manager.txt, sessions.json.
- Android 11 AudioPolicyService.cpp: isVirtualSource includes REMOTE_SUBMIX;
  virtual sources get allowCapture=true. This is the relevant baseline for the
  Redmi's Android 11, not proof of an identical MIUI implementation:
  https://android.googlesource.com/platform/frameworks/av/+/refs/heads/android11-release/services/audiopolicy/service/AudioPolicyService.cpp
- Current input-sharing documentation, secondary to the version-specific code:
  https://developer.android.com/media/platform/sharing-audio-input
- AudioRecord.getActiveRecordingConfiguration, API 29 (available on Android 11):
  https://developer.android.com/reference/android/media/AudioRecord#getActiveRecordingConfiguration()
- Navigation projection documentation is not a contract for HFP call relaying:
  https://developer.android.com/training/cars/apps/navigation

## Software verification

The workflow's host suite passes locally using JDK 17 compiler modules. The new
production-service test runs RxBridgeService with deterministic Android fakes:
all eight stages and actual resource lifetimes, one SCO RX recorder, same-call
restart refusal, media focus denial cleanup, wrong-route PCM exclusion, explicit
stop and changed session cleanup, and unchanged D5/regular D6 scheduling.
Observer tests cover true/false/unknown AA reader state and elapsed-window timing.
These are software fixtures, not a Redmi, emulator, HAL or car test. Android
compilation, signing, manifest and alignment are verified before delivery.

## Installation and next useful test

1. Install signed 1.9.13-lab / versionCode 76. If Android rejects the signature,
   uninstall the previous APK, install and restore all five LSPosed scopes.
   Reboot Redmi once. Keep the Magisk 1.9.0-persistent runtime.
2. Start report capture outside a call, then press the new one-call diagnostic
   button. Wait for readiness. Keep the car parked.
3. Initiate one call from the autoradio, preserving the user's reported scenario.
   Use a remote person who speaks/counts continuously, not a short announcement
   followed by silence. Keep the call at least 75 seconds. Note the displayed
   stages during audible sound/cutoff/return; speak near Redmi in stages 3 and 8
   and ask whether the remote person hears it. Car microphone is not supported.
4. Hang up, stop capture, export the report. No four-call comparison or extra
   restart is needed for this experiment.
