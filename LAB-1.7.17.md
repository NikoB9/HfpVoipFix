# HfpVoipLab 1.7.17

The 1.7.16 report showed the RX output initially targeting the speaker, then MIUI changing the output patch to Bluetooth SCO while the app was waiting for the two outputs to settle. The bridge rejected RX and wrote no received audio.

D4 now starts its RX and microphone captures before starting the speaker and SCO output tracks, restoring the startup order used by the earlier D4 run that confirmed both routes and forwarded RX samples. The bridge still forwards audio only while each actual route is confirmed and continuously stops if a route changes. The media stream remains at the device's Media volume, and the bridge logs its current index/max. This build has not yet been verified on the Redmi; audible RX and usable microphone both require a real call test. PCM is not saved.

Install as an update, reboot once for LSPosed to load 1.7.17, select/apply D4, start capture, arm, then call. Confirm received sound first. Test the microphone separately by asking the other party to confirm they can hear you. Export the report if either direction fails.
