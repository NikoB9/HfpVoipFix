# HfpVoipLab 1.7.13

The D4 report from MIUI 12.5.6 showed both D4 captures using the `primary input` mix port. The supplied vendor audio policy config declares that port with `maxActiveCount="1"` and separately declares a `voip_tx` input port with `AUDIO_INPUT_FLAG_VOIP_TX`, also with one active stream. The report showed MediaTek stopping SCO RX when the D4 microphone capture opened.

Version 1.7.13 changes only the D4 microphone `AudioRecord` source from `MIC` to `VOICE_COMMUNICATION`. This is intended to request the vendor VoIP input profile while D4 RX keeps the SCO input. The app logs the requested source and both active recording configurations; it still forwards neither direction unless both routes are confirmed. PCM is never saved.

This is a device test candidate, not a claimed fix. Android policy source selection does not guarantee that this MIUI HAL maps the request to `voip_tx`, nor that the HAL can keep the SCO input alive concurrently. Validate that in the fresh D4 report before declaring duplex working. D2 and D3 code paths are unchanged.

## Device test

1. Install this APK as an update and reboot once for LSPosed to load module 1.7.13.
2. Select D4, apply it while idle, start capture, arm, and wait for “D4 duplex armé”.
3. Call the Samsung from the Redmi. Stop promptly if either direction is audible but distorted or uncontrolled; otherwise allow a few seconds to gather route diagnostics, then hang up.
4. Export the HFP report. Check `hfp-events.log` for `INPUT_CONFIG`, the audio-policy `getInputForAttr` source/flags, and whether `BT_SCO_RX_End()` still occurs immediately after the VoIP capture opens. Confirm audio in both directions separately.

Do not modify mixer controls or system/vendor files for this test.
