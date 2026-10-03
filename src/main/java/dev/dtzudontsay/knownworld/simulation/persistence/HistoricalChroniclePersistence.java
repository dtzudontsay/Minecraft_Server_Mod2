package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.event.HistoricalChronicleManager;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Dedicated persistence for the compressed historical chronicle.
 *
 * This intentionally lives beside, rather than inside, npcs.tsv. The detailed
 * event journal and the long-term compressed chronicle have different scaling
 * and migration requirements and should not be forced into the same storage
 * lifecycle forever.
 *
 * Lifecycle ordering is important:
 *
 * KnownWorld registers NpcSimulation first and this class immediately after
 * it. Fabric therefore calls the NPC simulation SERVER_STARTED listener first,
 * allowing the retained detailed journal to load/reconstruct a fallback
 * chronicle. This persistence layer then replaces that fallback with the exact
 * persisted chronicle when a chronicle file exists.
 */
public final class HistoricalChroniclePersistence {

    private static final String HEADER =
            "KNOWNWORLD_HISTORICAL_CHRONICLE\t1";

    private static final String FILE_NAME =
            "historical_chronicle.tsv";

    private static final String TEMP_FILE_NAME =
            "historical_chronicle.tsv.tmp";

    private final Path directory;

    private final Path file;

    public HistoricalChroniclePersistence(
            Path directory
    ) {

        this.directory =
                directory;

        this.file =
                directory.resolve(
                        FILE_NAME
                );
    }

    /**
     * Registers persistence after NpcSimulation.registerLifecycle().
     */
    public static void registerLifecycle() {

        ServerLifecycleEvents.SERVER_STARTED.register(
                HistoricalChroniclePersistence::loadForServer
        );

        ServerLifecycleEvents.BEFORE_SAVE.register(
                (
                        server,
                        flush,
                        force
                ) ->
                        saveForServer(
                                server
                        )
        );

        ServerLifecycleEvents.SERVER_STOPPING.register(
                HistoricalChroniclePersistence::saveForServer
        );
    }

    public void save(
            HistoricalChronicleManager chronicle
    ) throws IOException {

        HistoricalChronicleManager.PersistedSnapshot snapshot =
                chronicle.snapshotForPersistence();

        HistoricalChronicleManager.validateSnapshot(
                snapshot
        );

        Files.createDirectories(
                directory
        );

        Path temporary =
                directory.resolve(
                        TEMP_FILE_NAME
                );

        try (
                BufferedWriter writer =
                        Files.newBufferedWriter(
                                temporary,
                                StandardCharsets.UTF_8
                        )
        ) {

            writer.write(
                    HEADER
            );

            writer.newLine();

            HistoricalChronicleManager.ChronicleStats global =
                    snapshot.global();

            writer.write(
                    String.join(
                            "\t",
                            "GLOBAL",
                            Long.toString(
                                    global.totalEvents()
                            ),
                            Long.toString(
                                    global.socialEvents()
                            ),
                            Long.toString(
                                    global.structuralEvents()
                            ),
                            Long.toString(
                                    global.eventsWithFacts()
                            ),
                            Double.toString(
                                    global.totalImportance()
                            ),
                            Double.toString(
                                    global.maximumImportance()
                            ),
                            Long.toString(
                                    global.firstRecordedTick()
                            ),
                            Long.toString(
                                    global.lastRecordedTick()
                            )
                    )
            );

            writer.newLine();

            for (
                    HistoricalChronicleManager.TypeStats typeStats :
                    snapshot.types()
            ) {

                writer.write(
                        String.join(
                                "\t",
                                "TYPE",
                                typeStats.type()
                                        .name(),
                                Long.toString(
                                        typeStats.count()
                                ),
                                Long.toString(
                                        typeStats.factCount()
                                ),
                                Double.toString(
                                        typeStats.totalImportance()
                                ),
                                Double.toString(
                                        typeStats.maximumImportance()
                                ),
                                Long.toString(
                                        typeStats.firstTick()
                                ),
                                Long.toString(
                                        typeStats.lastTick()
                                )
                        )
                );

                writer.newLine();
            }
        }

        try {

            Files.move(
                    temporary,
                    file,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );

        } catch (IOException exception) {

            Files.move(
                    temporary,
                    file,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    /**
     * @return true when an exact persisted chronicle was loaded, false when no
     * chronicle file exists and the caller should retain the journal-derived
     * migration fallback already present in memory.
     */
    public boolean loadInto(
            HistoricalChronicleManager chronicle
    ) throws IOException {

        if (!Files.exists(
                file
        )) {

            return false;
        }

        try (
                BufferedReader reader =
                        Files.newBufferedReader(
                                file,
                                StandardCharsets.UTF_8
                        )
        ) {

            String header =
                    reader.readLine();

            if (!HEADER.equals(
                    header
            )) {

                throw new IOException(
                        "Unsupported historical chronicle format: "
                                + header
                );
            }

            HistoricalChronicleManager.ChronicleStats global =
                    null;

            Map<WorldEventType, HistoricalChronicleManager.TypeStats> types =
                    new EnumMap<>(
                            WorldEventType.class
                    );

            String line;

            int lineNumber =
                    1;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                lineNumber++;

                if (line.isBlank()) {

                    continue;
                }

                try {

                    if (line.startsWith(
                            "GLOBAL\t"
                    )) {

                        if (global != null) {

                            throw new IllegalArgumentException(
                                    "Duplicate GLOBAL record"
                            );
                        }

                        global =
                                decodeGlobal(
                                        line
                                );

                        continue;
                    }

                    if (line.startsWith(
                            "TYPE\t"
                    )) {

                        HistoricalChronicleManager.TypeStats typeStats =
                                decodeType(
                                        line
                                );

                        HistoricalChronicleManager.TypeStats previous =
                                types.put(
                                        typeStats.type(),
                                        typeStats
                                );

                        if (previous != null) {

                            throw new IllegalArgumentException(
                                    "Duplicate TYPE record for "
                                            + typeStats.type()
                            );
                        }

                        continue;
                    }

                    throw new IllegalArgumentException(
                            "Unexpected chronicle record type"
                    );

                } catch (RuntimeException exception) {

                    throw new IOException(
                            "Invalid historical chronicle data at line "
                                    + lineNumber
                                    + ": "
                                    + line,
                            exception
                    );
                }
            }

            if (global == null) {

                throw new IOException(
                        "Historical chronicle is missing GLOBAL record"
                );
            }

            List<HistoricalChronicleManager.TypeStats> completeTypes =
                    new ArrayList<>();

            for (
                    WorldEventType type :
                    WorldEventType.values()
            ) {

                HistoricalChronicleManager.TypeStats stored =
                        types.get(
                                type
                        );

                if (stored != null) {

                    completeTypes.add(
                            stored
                    );

                } else {

                    /*
                     * Forward-compatible default for event types added after
                     * this save was originally written.
                     */
                    completeTypes.add(
                            emptyTypeStats(
                                    type
                            )
                    );
                }
            }

            HistoricalChronicleManager.PersistedSnapshot snapshot =
                    new HistoricalChronicleManager.PersistedSnapshot(
                            global,
                            completeTypes
                    );

            try {

                HistoricalChronicleManager.validateSnapshot(
                        snapshot
                );

                chronicle.restore(
                        snapshot
                );

            } catch (RuntimeException exception) {

                throw new IOException(
                        "Historical chronicle failed integrity validation",
                        exception
                );
            }
        }

        return true;
    }

    private static HistoricalChronicleManager.ChronicleStats decodeGlobal(
            String line
    ) {

        String[] p =
                line.split(
                        "\t",
                        -1
                );

        if (p.length != 9) {

            throw new IllegalArgumentException(
                    "Expected 9 GLOBAL columns, got "
                            + p.length
            );
        }

        long totalEvents =
                Long.parseLong(
                        p[1]
                );

        double totalImportance =
                Double.parseDouble(
                        p[5]
                );

        double averageImportance =
                totalEvents == 0L
                        ? 0.0
                        : totalImportance
                        / totalEvents;

        return new HistoricalChronicleManager.ChronicleStats(
                totalEvents,
                Long.parseLong(
                        p[2]
                ),
                Long.parseLong(
                        p[3]
                ),
                Long.parseLong(
                        p[4]
                ),
                totalImportance,
                averageImportance,
                Double.parseDouble(
                        p[6]
                ),
                Long.parseLong(
                        p[7]
                ),
                Long.parseLong(
                        p[8]
                )
        );
    }

    private static HistoricalChronicleManager.TypeStats decodeType(
            String line
    ) {

        String[] p =
                line.split(
                        "\t",
                        -1
                );

        if (p.length != 8) {

            throw new IllegalArgumentException(
                    "Expected 8 TYPE columns, got "
                            + p.length
            );
        }

        WorldEventType type =
                WorldEventType.valueOf(
                        p[1]
                );

        long count =
                Long.parseLong(
                        p[2]
                );

        double totalImportance =
                Double.parseDouble(
                        p[4]
                );

        double averageImportance =
                count == 0L
                        ? 0.0
                        : totalImportance
                        / count;

        return new HistoricalChronicleManager.TypeStats(
                type,
                count,
                Long.parseLong(
                        p[3]
                ),
                totalImportance,
                averageImportance,
                Double.parseDouble(
                        p[5]
                ),
                Long.parseLong(
                        p[6]
                ),
                Long.parseLong(
                        p[7]
                )
        );
    }

    private static HistoricalChronicleManager.TypeStats emptyTypeStats(
            WorldEventType type
    ) {

        return new HistoricalChronicleManager.TypeStats(
                type,
                0L,
                0L,
                0.0,
                0.0,
                0.0,
                -1L,
                -1L
        );
    }

    private static void loadForServer(
            MinecraftServer server
    ) {

        NpcSimulation simulation =
                NpcSimulation.getNullable();

        if (simulation == null
                || simulation.server()
                != server) {

            return;
        }

        HistoricalChroniclePersistence persistence =
                new HistoricalChroniclePersistence(
                        saveDirectory(
                                server
                        )
                );

        try {

            boolean loaded =
                    persistence.loadInto(
                            simulation.events()
                                    .chronicle()
                    );

            if (loaded) {

                HistoricalChronicleManager.ChronicleStats stats =
                        simulation.events()
                                .chronicle()
                                .stats();

                KnownWorld.LOGGER.info(
                        "Loaded persisted historical chronicle with {} total events ({} social, {} structural).",
                        stats.totalEvents(),
                        stats.socialEvents(),
                        stats.structuralEvents()
                );

            } else {

                HistoricalChronicleManager.ChronicleStats stats =
                        simulation.events()
                                .chronicle()
                                .stats();

                KnownWorld.LOGGER.info(
                        "No persisted historical chronicle found. Using retained-event migration baseline with {} events; it will become canonical on the next save.",
                        stats.totalEvents()
                );
            }

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to load historical chronicle.",
                    exception
            );
        }
    }

    private static void saveForServer(
            MinecraftServer server
    ) {

        NpcSimulation simulation =
                NpcSimulation.getNullable();

        if (simulation == null
                || simulation.server()
                != server) {

            return;
        }

        HistoricalChroniclePersistence persistence =
                new HistoricalChroniclePersistence(
                        saveDirectory(
                                server
                        )
                );

        try {

            persistence.save(
                    simulation.events()
                            .chronicle()
            );

        } catch (IOException exception) {

            KnownWorld.LOGGER.error(
                    "Failed to save historical chronicle.",
                    exception
            );
        }
    }

    private static Path saveDirectory(
            MinecraftServer server
    ) {

        return server.getWorldPath(
                        LevelResource.ROOT
                )
                .resolve(
                        "knownworld"
                )
                .resolve(
                        "npc"
                );
    }
}
