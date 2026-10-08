# HfpVoipFix

Experimental LSPosed compatibility module for Android HFP Client calls.

The module changes only one behavior inside `com.android.bluetooth`: when
`com.android.bluetooth.hfpclient.connserv.HfpClientConnection` calls
`Connection.setAudioModeIsVoip(false)`, the argument is changed to `true`.

The goal is to make Android Telecom treat an HFP Client call as a communication
session rather than a local cellular call. This can help on phone-oriented
vendor audio HALs that otherwise build a local modem `TELEPHONY_RX/TX` path
instead of routing the remote HFP Client audio path.

> **Experimental / device-specific.** This was created for an Android 11
> MediaTek/Xiaomi HFP Client experiment. It is not a general-purpose call relay
> solution and is not guaranteed to work on other ROMs or Android versions.

## Download

[Download the latest APK](https://github.com/NikoB9/HfpVoipFix/releases/latest/download/HfpVoipFix-1.0.apk)

Current release: **v1.0.0** (`HfpVoipFix-1.0.apk`).

## Prerequisites

- Android 11 / API 30 for the current build.
- Root access.
- Magisk (or an equivalent root environment).
- LSPosed installed and working.
- Android Bluetooth package `com.android.bluetooth`.
- HFP Client profile already available/enabled on the device. This module does
  **not** enable HFP Client by itself.
- A remote phone exposing the Bluetooth HFP Audio Gateway role.

## Installation

1. Install `HfpVoipFix-1.0.apk`.
2. Open LSPosed and enable **HFP VoIP Fix**.
3. In the module scope, enable **system applications** if LSPosed hides them.
4. Select **only** `com.android.bluetooth` / Bluetooth.
5. Do **not** scope the module to Google Messages, Dialer, Phone, System
   Framework, or unrelated apps.
6. Reboot the device.

## Verification

After reboot, verify that the module loaded:

```sh
su
logcat -c
svc bluetooth disable
sleep 2
svc bluetooth enable
sleep 4
logcat -d | grep -i HfpVoipFix
```

Expected output includes:

```text
HfpVoipFix: Hook installed
```

During an HFP Client call, the hook should also log:

```text
HfpVoipFix: HFP Client: setAudioModeIsVoip(false) -> true
```

To inspect the resulting Telecom/audio mode without touching ALSA mixer state:

```sh
logcat -b all -d -v time | grep -iE \
'HfpVoipFix|setMode\(MODE_|hfp_enable|hfp_set_sampling_rate|hfp_volume|createAudioPatch'
```

A mode switch to `MODE_IN_COMMUNICATION` only proves that the hook is active.
It does **not** prove that SCO audio is routed correctly. On the current MT6785
test device, the hook successfully switches Telecom from `MODE_IN_CALL` to
`MODE_IN_COMMUNICATION`, but usable call audio is still not obtained and a
continuous high-pitched tone can be heard from the speaker. The remaining issue
appears to be in vendor HFP/SCO audio routing rather than in the Telecom mode
flag alone.

## Current test status

On the Android 11 / MT6785 test device:

- the LSPosed hook loads correctly inside `com.android.bluetooth`;
- `setAudioModeIsVoip(false)` is changed to `true`;
- Telecom consequently switches to `MODE_IN_COMMUNICATION`;
- the MediaTek audio HAL still receives HFP parameters such as
  `hfp_set_sampling_rate=16000`, `hfp_enable=true`, and `hfp_volume=8`;
- call signalling works, but usable HFP call audio is **not fixed yet**;
- a continuous high-pitched tone has been observed from the speaker.

This repository therefore documents an **experimental diagnostic workaround**,
not a completed HFP audio fix.

## What this module does not do

- It does not enable the HFP Client profile.
- It does not bridge SMS.
- It does not modify Google Messages.
- It does not create a Bluetooth pairing.
- It does not guarantee that a vendor audio HAL can route SCO audio correctly.
- It does not implement an Android Auto audio bridge.

## Rollback

Disable **HFP VoIP Fix** in LSPosed and reboot. No system files are modified by
this module.

## Safety note for MT6785 / MediaTek diagnostics

On the test device, reading active ALSA PCM status nodes under
`/proc/asound/card*/pcm*/sub*/status` triggered a kernel panic in
`mtk_afe_pcm_pointer()`. Avoid using those status nodes while experimenting on
similar vendor kernels unless you have independently verified they are safe.

## Source layout

- `app/src/main/java/com/hfpvoipfix/HfpVoipFix.java` — LSPosed hook.
- `app/src/main/assets/xposed_init` — legacy Xposed entry point.
- `app/src/main/AndroidManifest.xml` — Xposed module metadata.
- `dist/HfpVoipFix-1.0.apk` — current prebuilt APK used by the release workflow.

## License

MIT. See [LICENSE](LICENSE).
