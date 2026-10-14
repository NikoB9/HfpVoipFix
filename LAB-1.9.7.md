# 1.9.7: lighter diagnostics and scoped Android Auto readiness proxy

The 1.9.6 report records Android Auto `placeCall: isSimReady:false` at
14:02:47.589, an earlier Android Auto ANR, and repeated bridge probe timeouts.
Those events are established; a shared cause for the ANR and timeouts is not.

## Stability

- Limit application diagnostics to two readers, projection/default Android Auto
  and dialer processes, and specific call tags. No verbose UI/MessageQueue logs.
  Poll process changes every six seconds instead of two. Preserve bounded files.
- Put D6 account/network checks and master audio requests on an auxiliary timer,
  outside the local lifecycle lock, separate from status authorization replies.
  Catch unexpected timer failures. The readiness sample expires after five seconds.
- Root commands kill unfinished children on exceptional exits too.

## Android Auto readiness experiment

Add Android Auto (`com.google.android.projection.gearhead`) to LSPosed scope.
Only in that package, only in D6 on Android 11 begonia with the existing native
patch, proxy the default `TelephonyManager.getSimState()` result UNKNOWN/ABSENT
to READY while a fresh root-published lease proves a single connected HFP master,
network service=1 and the unique HFP outgoing account selected. The lease's
timestamp comes from the network sample, not from when it is republished.
No subscription, SIM slot, system telephony, dialer, call intent or emergency
number routing is modified. Locked SIM states are not overridden. D5 bypasses it.
Lease expiry/disconnection/network loss blocks the proxy automatically.

This is a targeted compatibility experiment, not a native Core-Telecom calling
integration. The log shows Android Auto's readiness decision, not which exact
framework method its closed-source version uses. If it checks another API or
explicitly chooses a SIM account, this hook may not remove the refusal.
`AA_SIM_READINESS` logs prove whether this hook was actually queried/applied.
The car audio path and local-microphone limitation remain as in 1.9.5/1.9.6.

Validation: production hook fixtures and pure policy tests cover scope, D5,
locked SIMs, stale/future/missing leases and connected-account/network guards.
Physical autoradio behavior and performance still require a device test.
