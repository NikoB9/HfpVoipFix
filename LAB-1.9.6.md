# 1.9.6: Android Auto outgoing-call diagnostics

The first autoradio attempt is already present in the 1.9.4 capture. Existing
filtered logs cannot identify Android Auto's own refusal. This release adds
diagnostic collection, not a confirmed fix for “network unavailable”.

- During a user-started capture, discover exact Android Auto and dialer process
  names (including colon subprocess names) every two seconds. Start one main/system
  logcat reader per PID, at most six, with a 200-line lookback to cover startup.
- Capture all priorities only for those PIDs, and apply the existing privacy filter
  before writing. No device-wide all-tag logging or radio buffer capture.
- Keep the first 1 MiB and two rotating 1 MiB tail slots. Export timestamps,
  reader starts/ends, errors and dropped-tail counters. Release apps may omit logs;
  an empty app log is not evidence that a request was never made.
- Existing Telecom tags now include debug in main/system, to correlate app requests
  with Telecom connection creation. Close readers before constructing the report.
- D5 and D6 audio routing remain as in 1.9.5. No SIM/network availability spoofing,
  native-module change or new calling integration is included.

Host/CI tests validate process scoping, compilation and existing safety gates.
Real-device diagnostic completeness and Android Auto behavior require a new capture.
