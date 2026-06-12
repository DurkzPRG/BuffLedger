package com.durkz.buffledger.prefs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Persists players who opted out via {@code /buffs off}. Everyone else is tracked on join;
 * the HUD only renders while at least one timed effect is active.
 */
public final class BuffLedgerPrefs {

    private static final Logger LOG = Logger.getLogger(BuffLedgerPrefs.class.getName());

    private final Path optedOutFile;
    private final Set<UUID> optedOut = ConcurrentHashMap.newKeySet();

    public BuffLedgerPrefs(Path dataDirectory) {
        optedOutFile = dataDirectory.resolve("opted_out.txt");
        load();
    }

    public boolean isOptedOut(UUID uuid) {
        return optedOut.contains(uuid);
    }

    public void setOptedOut(UUID uuid, boolean optedOut) {
        if (optedOut) {
            this.optedOut.add(uuid);
        } else {
            this.optedOut.remove(uuid);
        }
        save();
    }

    private void load() {
        if (!Files.isRegularFile(optedOutFile)) {
            return;
        }
        try (Stream<String> lines = Files.lines(optedOutFile, StandardCharsets.UTF_8)) {
            lines.map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .forEach(line -> {
                        try {
                            optedOut.add(UUID.fromString(line));
                        } catch (IllegalArgumentException ignored) {
                            // skip corrupt lines
                        }
                    });
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to read BuffLedger prefs from " + optedOutFile, e);
        }
    }

    private void save() {
        try {
            Files.createDirectories(optedOutFile.getParent());
            String body = optedOut.stream()
                    .map(UUID::toString)
                    .sorted()
                    .reduce((a, b) -> a + System.lineSeparator() + b)
                    .orElse("");
            Files.writeString(optedOutFile, body.isEmpty() ? "" : body + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to write BuffLedger prefs to " + optedOutFile, e);
        }
    }
}
