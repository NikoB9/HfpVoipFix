# HfpVoipLab 1.7.16

The 1.7.15 report showed the app opening its two output tracks, after which MIUI rerouted the track set from the speaker to Bluetooth SCO. The route-stability check timed out before either capture stream started; no D4 microphone or receive audio was forwarded in that attempt.

D4 RX returns to `USAGE_MEDIA`, which had kept the explicit speaker route in 1.7.14. The app now logs the media volume index/max used for speaker playback and tells the user to adjust the media volume. During the test, set Media volume to a clearly audible level (start around 10/15), then check D4 microphone and receive audio independently.

Install as an update, reboot once for LSPosed to load 1.7.16, select/apply D4, start capture, arm, then call. Export the report if sound or microphone is still missing. PCM is not saved.
