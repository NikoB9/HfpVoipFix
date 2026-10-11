# HfpVoipLab 1.7.11

## D4 full duplex test

D2 reception and D3 microphone transmission have each worked in separate calls. D4 opens two input/output pairs concurrently: SCO input to the Redmi speaker, plus the Redmi default microphone to SCO output. Both actual Android routes must be confirmed before either stream is forwarded. A route change or loss of call/session stops the bridge. PCM is never stored.

D4 is still experimental. This version keeps explicit per-call arming and a foreground notification. It is not yet integrated into the Android Phone/Telecom call UI. The test should confirm that both parties can hear each other at the same time; separate D2 and D3 successes do not prove simultaneous full duplex works.

## Test sequence

1. Install over 1.7.10 and reboot so LSPosed reloads the module.
2. Start capture, select D4, apply the mode, grant microphone permission if requested, then arm before the call.
3. Place/answer the Samsung call from the Redmi controls. Confirm the Redmi hears the far end while the far end hears the Redmi microphone.
4. End the call and export the report. Look for `ROUTE_CONFIRMED direction=rx`, `ROUTE_CONFIRMED direction=tx`, and ongoing `STATS direction=rx` plus `STATS direction=tx`. The report metrics prove the app passed PCM buffers to AudioTrack; only the listening test establishes intelligible remote audio.
