package com.hfpdiag;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

final class Root {
    private Root() {}

    static boolean available() {
        try { return "0".equals(run("id -u", 100, 5).trim()); }
        catch (Exception e) { return false; }
    }

    // Read-only diagnostics. No ALSA /proc/*/status reads, no audio routing changes.
    static String run(String command, int limit, int seconds) throws Exception {
        Process p = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
        StringBuilder result = new StringBuilder();
        Thread t = new Thread(() -> {
            try (BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = b.readLine()) != null) {
                    if (result.length() < limit) result.append(line, 0, Math.min(line.length(), limit - result.length())).append('\n');
                }
            } catch (IOException ignored) { }
        }, "hfp-command-reader");
        t.setDaemon(true);
        t.start();
        if (!p.waitFor(seconds, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IOException("Timeout: " + command);
        }
        t.join(1200);
        if (p.exitValue() != 0) throw new IOException("Command failed: " + command + " (exit " + p.exitValue() + ")");
        return result.toString();
    }

    static String snapshot(boolean inCall) {
        String[] commands = inCall ?
            new String[] { "dumpsys audio", "dumpsys media.audio_policy", "dumpsys media.audio_flinger" } :
            new String[] { "dumpsys audio", "dumpsys media.audio_policy", "dumpsys media.audio_flinger",
                "dumpsys bluetooth_manager", "dumpsys telecom", "dumpsys package com.hfpvoipfix",
                "getprop ro.build.fingerprint", "getprop ro.build.version.release",
                "getprop ro.product.device", "pidof com.android.bluetooth" };
        StringBuilder result = new StringBuilder();
        for (String cmd : commands) {
            result.append("\n====== ").append(cmd).append(" ======\n");
            try { result.append(run(cmd, 240000, 12)); }
            catch (Exception e) { result.append("Unavailable: ").append(e.getMessage()); }
        }
        return result.toString();
    }

    static String vendorConfig() {
        String[] paths = { "/vendor/etc/audio_policy_configuration.xml",
            "/vendor/etc/audio_device.xml", "/odm/etc/audio_policy_configuration.xml",
            "/vendor/etc/audio_policy_configuration_bluetooth_legacy_hal.xml" };
        StringBuilder result = new StringBuilder();
        for (String path : paths) {
            result.append("\n====== ").append(path).append(" ======\n");
            try { result.append(run("cat " + path, 240000, 10)); }
            catch (Exception ignored) { result.append("Unavailable\n"); }
        }
        return result.toString();
    }
}
