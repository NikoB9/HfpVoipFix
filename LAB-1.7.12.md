# HfpVoipLab 1.7.12

The latest D4 report showed the app at 1.7.11 while the active Bluetooth process still reported module 1.7.10 and `requested=observe`. The bridge correctly refused to open audio, but the arm button started the background service before checking that the mode had actually been applied. That made the refusal easy to miss while a regular Phone app call could still proceed.

Version 1.7.12 checks a fresh Bluetooth handshake before starting the bridge. It requires the current hook version, an idle call, the selected D2/D3/D4 mode in both the app property and Bluetooth status, and the required hooks. If these checks fail, the arm action stops and shows a direct instruction.

Install the APK and reboot once so LSPosed reloads the hook in the Bluetooth process. Then start capture, select D4, tap **Appliquer au prochain appel**, confirm the status says the module is 1.7.12, and tap **Armer**. Wait for **D4 duplex armé** before placing the call. Use the app's **Appeler depuis le Redmi** control for the test; do not continue with a normal Phone app call if the arm action reports an error.

This change improves the guard and diagnosis; it does not establish that D4 audio works. The D4 route still needs to be verified on the phone after the arm status is confirmed.
