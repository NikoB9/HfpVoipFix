# HfpVoipFix

Experimental LSPosed module for Android 11 / MediaTek HFP Client audio routing.

## v1.3.0 development build

v1.3 keeps the validated v1.2 SCO endpoint + AudioPatch experiment and changes the VoIP timing.

The previous build forced `Connection.setAudioModeIsVoip(true)` from the Bluetooth process, but logs showed Telecom had already entered `MODE_IN_CALL` and MediaTek had already enabled its local phone-call controller before that hook took effect.

v1.3 therefore adds a second hook in `com.android.server.telecom`:

- `com.android.server.telecom.Call.getIsVoipAudioMode()`
- returns `true` immediately when the call's target PhoneAccount or ConnectionService is `com.android.bluetooth/.hfpclient.connserv.HfpClientConnectionService`.

The Bluetooth-side hook still:

- sends `bt_wbs=on/off`;
- declares HFP Client SCO input/output endpoints;
- creates software AudioPatch bridges `SCO_IN -> speaker` and `mic -> SCO_OUT`;
- cleans them up when SCO closes.

## LSPosed scope

The manifest includes the legacy LSPosed `xposedscope` metadata with the recommended packages:

- `com.android.bluetooth`
- `com.android.server.telecom`

LSPosed should surface these as the module's recommended scope. Depending on LSPosed Manager behavior/version, you may still need to confirm "Recommended" when enabling the module.

## Expected startup logs

```text
HfpVoipFix: MTK v1.3 Bluetooth hooks installed
HfpVoipFix: MTK v1.3 Telecom early-VoIP hook installed
```

During an HFP Client call, the key early line is:

```text
HfpVoipFix: Telecom early VoIP=true for HFP Client call
```

The important routing success criterion is that MediaTek goes directly from normal mode to `MODE_IN_COMMUNICATION` without first opening the local `MODE_IN_CALL` telephony RX/TX path.

## Safety

This remains experimental. Keep call volume low during testing. Avoid reading active `/proc/asound/card*/pcm*/sub*/status` nodes on the test device; this previously caused a kernel panic.

## Diagnostic data

Do not commit raw bug reports, Bluetooth captures, system-library archives, device inventories, or per-device fingerprints. They can contain identifiers and proprietary system details. The version notes in this repository contain a sanitized summary; keep raw diagnostic files private and review any export before sharing it.
