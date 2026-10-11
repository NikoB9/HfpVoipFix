# HfpVoipLab 1.7.15

D4 RX was delivered to the speaker route, but MIUI applied a -48 dB gain to the active receive track while `STREAM_MUSIC` was at volume index 3/15. D4 receive playback now uses `USAGE_VOICE_COMMUNICATION`, which should use the call-volume group while keeping the explicit built-in speaker destination. D4 TX and D2/D3 behavior are unchanged.

Install as an update, reboot once for LSPosed to load 1.7.15, then test D4. Raise the in-call volume during the test. Confirm the speaker receive audio and microphone independently with the other caller. Export a report if RX remains silent; the app does not save PCM.

This is a targeted fix based on the captured MIUI volume state and needs on-device confirmation.
