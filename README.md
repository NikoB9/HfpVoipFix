# HfpVoipFix

Experimental LSPosed module for **Android HFP Client on MediaTek devices**.

The current build, **v1.1.0**, targets a specific compatibility gap observed on an Android 11 / MT6785 Xiaomi device: Android's HFP Client opens SCO and sends the legacy HFP parameters, while the MediaTek audio HAL exposes a separate `bt_wbs` switch that configures the SCO codec path.

## Current build

**Version:** 1.1.0  
**Version code:** 11  
**APK:** `HfpVoipFix-1.1.0-mtk.apk`  
**SHA-256:** `478cf0000fa258e266ac6e3e51942cb84365ca1ff46df57710b836629ff9b6db`

[Download the latest APK](https://github.com/NikoB9/HfpVoipFix/releases/latest/download/HfpVoipFix-1.1.0-mtk.apk)

The package now exposes a real Android version number and uses Android's built-in Bluetooth icon, so it is identified more cleanly in package managers and LSPosed.

> **Experimental.** v1.1.0 has not yet been validated as a complete audio fix. It is a targeted diagnostic/compatibility build.

## What v1.1.0 does

The module is scoped to `com.android.bluetooth` and hooks:

`com.android.bluetooth.hfpclient.HeadsetClientStateMachine.routeHfpAudio(boolean)`

Immediately before the stock HFP Client routing code runs, it sends:

```text
routeHfpAudio(true)  -> AudioSystem.setParameters("bt_wbs=on")
routeHfpAudio(false) -> AudioSystem.setParameters("bt_wbs=off")
```

It does **not** force Telecom into VoIP mode. The previous `setAudioModeIsVoip(true)` experiment was removed because it changed Telecom to `MODE_IN_COMMUNICATION` but did not produce usable call audio on the MT6785 test device.

## Why this exists

On the test device, a normal HFP Client call already reaches:

```text
SCO/eSCO opened
hfp_set_sampling_rate=16000
hfp_enable=true
hfp_volume=8
```

but no MediaTek `BT_SCO_RX/TX` start sequence was observed.

Static inspection of `audio.primary.mt6785.so` showed that the HAL contains:

```text
bt_wbs
SetBTCurrentSamplingRateNumber
BT_SCO_SetMode
BT_SCO_RX_Open / Start
BT_SCO_TX_Open / Start
AudioALSAPlaybackHandlerBTSCO
AudioALSAPlaybackHandlerBTCVSD
AudioALSACaptureDataProviderBTSCO
AudioALSACaptureDataProviderBTCVSD
VOIP_Call_BT_Playback
VOIP_Call_BT_Capture
```

The purpose of v1.1.0 is therefore to test the missing AOSP-HFP-Client-to-MediaTek `bt_wbs` translation directly.

## Installation

- Root + LSPosed are required.
- HFP Client must already be enabled on the device.
- Install the APK.
- Enable **HFP VoIP Fix** in LSPosed.
- Scope it **only** to Bluetooth / `com.android.bluetooth`.
- Reboot.

### Signature note

The public v1.0.0 APK used an older signing key whose private key is no longer available in the current build environment. The v1.1.x line therefore uses a new signing key.

If v1.0.0 is installed, uninstall it before installing v1.1.0. Builds from the v1.1.x line can update each other as long as this signing key is retained.

## Verification

After reboot:

```sh
su
logcat -c
svc bluetooth disable
sleep 2
svc bluetooth enable
sleep 4
logcat -d | grep -i HfpVoipFix
```

Expected:

```text
HfpVoipFix: MTK bt_wbs bridge hook installed
```

During a short HFP Client call:

```sh
logcat -b all -d -v time | grep -iE \
'HfpVoipFix|bt_wbs|SetBTCurrentSamplingRateNumber|BT_SCO_SetMode|BT_SCO_(RX|TX)_(Open|Start|Stop|Close)|AudioALSAPlaybackHandlerBT(SCO|CVSD)|AudioALSACaptureDataProviderBT(SCO|CVSD)|hfp_|bta_av_sco_chg_cback' \
| tail -350
```

The first success criterion is that `bt_wbs=on` reaches the MediaTek HAL and triggers `SetBTCurrentSamplingRateNumber(16000)` / `BT_SCO_SetMode(true)`. Whether that is sufficient to produce usable bidirectional audio remains to be tested.

## Scope and safety

This module:

- does not enable HFP Client itself;
- does not modify SMS or Google Messages;
- does not pair Bluetooth devices;
- does not implement an Android Auto bridge;
- does not modify the MediaTek audio HAL binary.

On the MT6785 test kernel, reading active `/proc/asound/card*/pcm*/sub*/status` nodes caused a kernel panic inside `mtk_afe_pcm_pointer()`. Do not use those active ALSA status nodes as part of diagnostics on this device.

## Source

The Java source documents the exact hook logic used by the prebuilt APK. It references the hidden framework class `android.media.AudioSystem`; a conventional Gradle rebuild therefore requires platform/hidden-API stubs in addition to the public Android SDK.

## License

MIT.
