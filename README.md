# HfpVoipFix

Experimental LSPosed module for **Android HFP Client on MediaTek devices**.

The current build, **v1.1.1**, targets a compatibility gap observed on an Android 11 / MT6785 Xiaomi device: Android's HFP Client opens SCO and sends the legacy HFP parameters, while the MediaTek audio HAL exposes a separate `bt_wbs` switch that configures the SCO codec path.

![HfpVoipFix icon](art/ic_launcher.svg)

## Current build

**Version:** 1.1.1  
**Version code:** 12  
**APK:** `HfpVoipFix-1.1.1-mtk.apk`  
**SHA-256:** `a85e5a59c8971e993d90fb7fea36da0f4fef0ba80de3e51c1ff8575cde1fb1db`

[Download the latest APK](https://github.com/NikoB9/HfpVoipFix/releases/latest/download/HfpVoipFix-1.1.1-mtk.apk)

v1.1.1 adds a custom **phone + Bluetooth** application icon and bumps the Android package version. The HFP hook itself is unchanged from v1.1.0.

> **Experimental.** This is still a diagnostic/compatibility build; it is not yet a confirmed complete audio fix.

## What the module does

The module is scoped to `com.android.bluetooth` and hooks:

`com.android.bluetooth.hfpclient.HeadsetClientStateMachine.routeHfpAudio(boolean)`

Immediately before the stock HFP Client routing code runs, it sends:

```text
routeHfpAudio(true)  -> AudioSystem.setParameters("bt_wbs=on")
routeHfpAudio(false) -> AudioSystem.setParameters("bt_wbs=off")
```

It does **not** force Telecom into VoIP mode.

## Why this exists

On the test device, a normal HFP Client call already reaches:

```text
SCO/eSCO opened
hfp_set_sampling_rate=16000
hfp_enable=true
hfp_volume=8
```

but no MediaTek `BT_SCO_RX/TX` start sequence was observed.

Static inspection of `audio.primary.mt6785.so` showed that the HAL contains `bt_wbs`, `SetBTCurrentSamplingRateNumber`, `BT_SCO_SetMode`, the `BT_SCO_RX/TX` control path, the BTSCO/BTCVSD playback/capture classes, and the `VOIP_Call_BT_Playback/Capture` PCM names.

The purpose of the 1.1.x line is to test the missing AOSP-HFP-Client-to-MediaTek `bt_wbs` translation directly.

## Installation

- Root + LSPosed are required.
- HFP Client must already be enabled.
- Install the APK.
- Enable **HFP VoIP Fix** in LSPosed.
- Scope it **only** to Bluetooth / `com.android.bluetooth`.
- Reboot.

### Updating from 1.1.0

v1.1.1 uses the **same signing certificate** as v1.1.0, so it can be installed directly as an update.

The older public v1.0.0 build used a different signing certificate; uninstall v1.0.0 first if it is still installed.

## Verification

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

The first success criterion is that `bt_wbs=on` reaches the MediaTek HAL and triggers `SetBTCurrentSamplingRateNumber(16000)` / `BT_SCO_SetMode(true)`.

## Safety

This module does not modify SMS, Google Messages, the HFP Client enablement Magisk module, Bluetooth pairing, the vendor audio HAL binary, or Android Auto.

On the MT6785 test kernel, reading active `/proc/asound/card*/pcm*/sub*/status` nodes caused a kernel panic inside `mtk_afe_pcm_pointer()`. Do not use those active ALSA status nodes for diagnostics on this device.

## Source

The Java source documents the hook logic. The artwork source is in `art/ic_launcher.svg`; the release APK embeds a rendered PNG resource.

`android.media.AudioSystem` is a hidden platform API, so a conventional Gradle rebuild needs suitable platform/hidden-API stubs in addition to the public Android SDK.

## License

MIT.
