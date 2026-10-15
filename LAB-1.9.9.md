# 1.9.9 — D6 route compatibility and bounded pending-audio recovery

The 1.9.8 hardware report contains four calls. Only the first opens the master
SCO and starts the bridge. It acquires projection focus and receives nonzero
samples, but the user does not hear audio from the car. Subsequent calls remain
at master audio CONNECTING=1 without another SCO_BEGIN. The Telecom route hook
repeatedly logs NoSuchFieldException. Its old error did not identify the field;
we cannot say which field the OEM removed or renamed.

## Changes

- Use the route machine's CallAudioManager, whose tracked-call and foreground
  APIs are already used by our mode hook. Request ROUTE_EARPIECE through
  CallAudioManager.setAudioRoute; stop reading mCallsManager and
  USER_SWITCH_EARPIECE. Telecom supplies its own internal message mapping.
  Log the exact failing stage if this alternative is unavailable. Keep existing
  D6-only, single foreground HFP, SIM and emergency exclusions. D5 returns before
  any route lookup. A successful request is not logged as confirmed routing.
- Count actual master connect requests, not CONNECTING polls, against the
  existing three-request cap. If an active D6 master call has no owned SCO and
  stays CONNECTING for twelve seconds, allow one pending cancellation per call.
  The standard service disconnectAudio refuses isAudioOn=false, so post a
  revalidated request to the existing HFP Client state-machine thread and queue
  Android 11 DISCONNECT_AUDIO=4 at its front. Preserve the profile and all calls;
  do not modify mAudioState or fabricate an event. Wait for real state 0 and the
  cooldown before another connect. CONNECTED=2 without owned ports is observed
  only; open/owned SCO, ended/changed calls and D5 are excluded.
- Accept projected RX only when its actual output is a remote submix with the
  registered projection address pattern, not any remote submix or local speaker.
  Record actual output type/address when the route changes. No new audio channel.
- Retain the 1.9.8 projection focus experiment, the 1.9.7 AA readiness proxy and
  D5 audio processing/routing. Version handshakes advance to 1.9.9 (code 72).
  Keep native Magisk runtime 1.9.0 and the existing four LSPosed scopes.

## Validation and limits

Production hook fixtures deliberately omit both old OEM-dependent fields. They
exercise D6 route requests, D5/SIM/emergency/multiple-call/foreground guards and
API failures. Production master recovery fixtures exercise a queued pending
cancellation, missing native completion, connected state, owned ports, ended
calls, D5 and a late callback after call removal. Pure policy tests cover timers,
request caps and new calls. Endpoint tests reject local and generic submix outputs.
The CI runs the full host suite and compiles/signs/verifies the APK.

No hardware confirmation is claimed. The exact origin of the first audible sound
is unconfirmed; the user's report excludes the autoradio. These changes do not
implement the car microphone or establish that its navigation projection channel
can reproduce call audio.

Implementation references: Android 11 CallAudioManager.setAudioRoute,
HeadsetClientService.connectAudio/disconnectAudio and HeadsetClientStateMachine
CONNECT_AUDIO/DISCONNECT_AUDIO handlers; source copies inspected in the task
workspace. BluetoothHeadsetClient defines states DISCONNECTED=0, CONNECTING=1,
CONNECTED=2 in AOSP Android 11.
