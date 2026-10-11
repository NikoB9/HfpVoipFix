> **Native companion withdrawn after device test (2026-10-11).** Do not reinstall or re-enable the 1.7.21 native trial. Disable it and reboot to restore original vendor files; the APK can remain installed. Check standalone D2 on original files before further native experiments.

## Device result

The export confirms the trial marker and exact hashes of both generated overlays. The four phase starts are preserved; the call ends about five seconds into phase 4, before a complete 60-second sequence.

All 46 recorded phase-stat windows have RX RMS and peak equal to zero, including phase 1 before microphone capture. The expected SCO-to-speaker route remains reported throughout, and media volume is at maximum. This rules out a zero media-volume setting as the explanation for this test; route metadata does not prove useful Bluetooth audio.

The microphone capture contains nonzero samples from phase 2 onward. In phase 3 the bridge submits about 242,000 microphone samples to the SCO AudioTrack. Neither successful writes nor microphone RMS proves the correspondent received audio. The user's report is that neither direction seemed to work.

The previous cross-device RX reroute is absent in this test. Shared-output synchronization refuses SCO as intended, and the speaker path remains open when TX begins. Nevertheless, the Bluetooth receive handler repeatedly reports invalid mSBC headers, packet loss concealment and empty available buffers. These events also occurred briefly in the earlier working-RX baseline, so their existence alone is not a diagnosis; the sustained zero RX signal is the decisive difference.

The exact cause is unconfirmed. Several native and policy changes were introduced together, and no individual change has been isolated on hardware. Host ARM tests validated branching and register preservation; they did not validate hardware initialization or transport. Do not describe the trial as a functional duplex fix. Restore original files first, confirm standalone D2 recovery, then investigate the startup/transport dependency with individually scoped experiments. No codec switch or new native patch is justified solely by these logs.

---

# HfpVoipLab 1.7.21 — native isolation trial companion

The app retains the four-phase D4 sequence and the validated standalone D2/D3 configurations from 1.7.20. Version gating is updated consistently. Capture system-info now records the native trial marker and SHA-256 of the loaded vendor library path and audio policy configuration, so an export distinguishes a native overlay test from an unmodified app-only test. File hashes identify the on-disk overlay; they do not prove a library was loaded into a process.

Analysis of the exact user-supplied firmware confirmed global capture rerouting and shared-output synchronization. An experimental, exact-ROM Magisk companion was prepared privately from the supplied firmware: a separate SCO input profile, narrow SCO/MIC cross-device capture exceptions and SCO output isolation. It preserves other device-pair code paths. Raw vendor binaries, addresses, patches and firmware fingerprints remain outside this public repository.

Host verification of the companion: source inventory SHA-256 verification; 147 ARM emulator/ELF checks for branch destinations, device pairs, registers, stack, condition flags and original paths; installer hash/ROM/active-architecture rejection and one-boot disable-marker checks. These do not establish Android linker compatibility, HAL synchronization correctness or hardware duplex success.

The companion uses Magisk systemless vendor overlays. The installer refuses mismatched source hashes/ROM, a loaded 64-bit HAL or competing overlays. Its late_start script marks it disabled for the next boot; this does not unmount the current trial. A subsequent reboot restores original files once that marker exists. If late_start did not execute, disable it manually before rebooting. The overlay changes SCO behavior in the audio service for the whole trial boot, and may disrupt ordinary audio/Bluetooth calls. There is no permanent vendor partition write, direct mixer control, PCM device access or SELinux rule addition.

Install the APK and the companion outside calls, enable LSPosed for the new installation if needed, then reboot once after both installations. Use D4: apply, start capture, wait for ready, call for 65 seconds. Note received sound in each 15-second phase; microphone is transmitted only in phase 3. End call, finish capture and export HFP-Rapport.zip. Reboot afterward to end the native trial. A different temporary APK signature can require uninstalling the previous app first. APK-only 1.7.21 does not fix the two native routing conflicts.

Magisk packaging and overlay reference: https://topjohnwu.github.io/Magisk/guides.html (checked 2026-10-11).
