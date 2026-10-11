# HfpVoipLab 1.7.19

D4 is a diagnostic sequence, not a confirmed audio fix. The 1.7.18 report showed RX samples forwarded while Android reported the speaker route, but vendor logs closed the speaker hardware path when SCO playback became active. Media volume was already 15/15.

Four automatic phases last 15 seconds each:

1. RX only: same source, usage and gain as D2. No TX capture or playback object exists.
2. Add VOICE_COMMUNICATION microphone capture. Read and discard its samples; no SCO playback object exists.
3. Construct and start SCO playback; forward microphone samples only while the actual MIC-to-SCO route matches.
4. Pause, flush and release the SCO playback track. Keep microphone capture running to isolate removal of TX output.

RX persists across all phases and forwards samples only to a confirmed SCO-to-speaker route, independently of TX. Route conflicts are logged without reapplying device preferences; this avoids changing the experiment during a phase. Only zeroes may prime an unconfirmed playback route. Fresh Bluetooth state and the current SCO session remain mandatory. A local phone call, negative audio read/write result or lost session ends the bridge. The sequence stops the bridge after about 60 seconds; it does not hang up the remote call. No PCM is saved.

PHASE_TRANSITION marks object changes; PHASE_BEGIN/END and PHASE_STATS record phase timestamps, per-phase counts, RMS, routes, playback head, volume and silencing. Stats reset between phases; the RX playback head remains cumulative. Android route confirmation and buffer consumption still do not prove hardware playback or audibility.

Install the signed APK, reboot once, choose D4 and apply outside a call, start capture, arm and call. Keep the call at least 65 seconds and the app visible. Ask the other party to speak during all four phases; your microphone is sent only in phase 3. Note whether RX is audible in phases 1/2/3/4, hang up, stop capture and export. D2/D3 behavior is unchanged.

CI uses the stable signing identity when configured. Otherwise it creates a temporary test identity, verifies APK v2/v3 signatures and alignment, and includes a note that uninstall may be required. Raw diagnostic logs remain private and are not committed.
