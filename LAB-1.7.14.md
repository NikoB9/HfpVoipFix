# HfpVoipLab 1.7.14

This is an experimental follow-up to the failed 1.7.13 D4 test on Redmi Note 8 Pro / MIUI 12.5.

The report showed MediaTek reopening input streams when SCO output became active. D4 now starts both output tracks first, waits up to 1.5 seconds for the speaker and SCO routes to settle, then starts its two input captures. The `VOICE_COMMUNICATION` capture explicitly requests the built-in bottom microphone when available. D4 remains gated on live route confirmation and never saves PCM.

Test flow: install APK update, reboot once for LSPosed to load 1.7.14, start capture, select and apply D4, arm, place the call from the app, and note whether speaker audio and microphone both work. Export the report immediately afterward, even if the bridge stops early.
