# HfpVoipLab 1.7.18

The 1.7.17 report confirms that D4 briefly reached the requested RX speaker and microphone-to-SCO routes. MIUI then changed the output patches while both bridges were active; the service stopped when the microphone route no longer matched. Media volume was 15/15, so changing that setting would not address this report.

D4 now records the actual device IDs when a route changes and briefly reapplies the speaker/SCO output preferences. It waits up to three continuous seconds for both directions to return. Samples are forwarded only while both routes currently match; a continuing conflict stops the bridge. This is a bounded routing experiment, not a confirmed fix. The latest report had no per-direction `STATS`, so the 1.7.17 attempt did not establish that audio reached either endpoint. PCM is not saved.

The previously working standalone D2 and D3 modes remain the reference checks; this build is specifically for another D4 attempt. Install as an update over 1.7.17, reboot once for LSPosed to load 1.7.18, then select/apply D4, start capture, arm, and call. During a route retry, wait for the bridge to either recover or report that the routes remained unstable.
