
package dev.dtzudontsay.knownworld.simulation.economy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

/**
 * Persistent, explicit choice of authoritative
 * economic save format.
 *
 * Absence of this marker means legacy compatibility
 * selection, not implicit snapshot activation.
 */
public final class EconomyAuthorityMarker {

    public enum Authority {
        SNAPSHOT
    }

    private static final String HEADER =
            "KNOWNWORLD_ECONOMY_AUTHORITY\t1";

    private static final String FILE =
            "economy_authority.tsv";

    private final Path file;

    public EconomyAuthorityMarker(Path directory) {
        Objects.requireNonNull(directory, "directory");
        this.file = directory.resolve(FILE);
    }

    public Path file() {
        return file;
    }

    public boolean exists() {
        return Files.exists(file);
    }

    public Authority read() throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException(
                    "Economy authority marker is missing"
            );
        }

        List<String> lines = Files.readAllLines(
                file,
                StandardCharsets.UTF_8
        );

        if (lines.size() != 2
                || !HEADER.equals(lines.get(0))
                || !"AUTHORITY\tSNAPSHOT".equals(lines.get(1))) {
            throw new IOException(
                    "Unsupported or corrupt economy authority marker"
            );
        }

        return Authority.SNAPSHOT;
    }

    /**
     * Called only by an explicitly requested and
     * validated activation procedure.
     *
     * Never replaces an existing marker.
     */
    public void activateSnapshot() throws IOException {
        if (Files.exists(file)) {
            throw new IOException(
                    "Economy authority marker already exists"
            );
        }

        Files.createDirectories(file.getParent());

        Path temporary = file.resolveSibling(
                FILE + ".tmp"
        );

        if (Files.exists(temporary)) {
            throw new IOException(
                    "Unresolved authority marker temporary file"
            );
        }

        String content = HEADER
                + "\nAUTHORITY\tSNAPSHOT\n";

        try {
            Files.writeString(
                    temporary,
                    content,
                    StandardCharsets.UTF_8
            );

            try {
                Files.move(
                        temporary,
                        file,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException ex) {
                throw new IOException(
                        "Atomic authority activation unsupported",
                        ex
                );
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
