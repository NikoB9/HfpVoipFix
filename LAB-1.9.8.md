# 1.9.8 — D6 projected playback focus experiment

The 1.9.7 report contains nonzero master RX samples routed to the registered
navigation remote submix, with no app focus request. Its AudioManager focus
history instead records Telecom's locked call focus (flags=0x4), plus ordinary
Bluetooth client focus. Successful submix writes did not produce audible car audio.

Android 11 MediaFocusControl rejects ordinary focus acquisition under a locked
call owner. A new AudioTrack alone does not request focus. This release tests
that missing control path; it does not establish that this car can play the
projection channel during a call.

- Only in the existing Telecom VoIP/Other focus-enter context, for one foreground
  non-emergency HFP call in D6, suppress the locked voice-call focus request.
  Abandon the prior Telecom ringing focus at that transition, if present.
  Keep the existing NORMAL-mode translation and second-SCO suppression.
- The D6 bridge requests transient projection focus with the exact navigation
  attributes used by its output track. Refusal stops that bridge with an explicit
  diagnostic, without claiming playback or forcing focus.
- Pause and flush projected playback on any loss, including ducking; drain but
  discard received samples while focus is absent. Resume on gain, release the
  request on every exit. Do not pause the return microphone because of a projected
  playback loss. No focus retries.
- D5 has no new focus request and keeps Telecom's original focus behavior.
  Its audio DSP/routing is unchanged. Only version handshake strings advance.
- Keep 1.9.7 AA SIM readiness lease and diagnostics unchanged except version.

The mic is still the Redmi mic. This does not implement the vehicle microphone
or prove projection focus will override a head unit's independent call transport.
No new Magisk runtime is required; keep 1.9.0 and the same four LSPosed scopes.

Validation: production focus lifecycle tests and actual Telecom hook fixture,
including D5, SIM, emergency, ringing, multiple calls, non-foreground, foreign
stream and calls outside Telecom's scoped state entry. Hardware test required.

Sources checked 2026-10-15:
- https://developer.android.com/media/optimize/audio-focus
- https://android.googlesource.com/platform/frameworks/base/+/android11-release/services/core/java/com/android/server/audio/MediaFocusControl.java
- Android 11 CallAudioModeStateMachine.VoipCallFocusState.enter and
  HeadsetClientStateMachine.requestAudioFocus (AOSP source copies examined).
