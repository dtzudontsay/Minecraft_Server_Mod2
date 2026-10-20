
package dev.dtzudontsay.knownworld.simulation.integration;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;
import dev.dtzudontsay.knownworld.world.generation.KnownWorldChunkGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * B1: One Minecraft overworld clock day = one campaign day.
 *
 * Uses Minecraft 26.3 getOverworldClockTime(), NOT the
 * older pre-26.3 getDayTime() API.
 *
 * Designed to be registered after NpcSimulation.registerLifecycle():
 * - NpcSimulation loads first.
 * - This bridge enables external clock authority.
 * - NpcSimulation's normal tick does not double-advance days.
 * - The bridge observes world-day crossings.
 *
 * An auxiliary save file records the observed Minecraft clock
 * and any outstanding forward-time catch-up work.
 *
 * Never rewinds historical time on backward /time commands.
 * Does not pretend that a /time change is equivalent to
 * simulated travel, economic activity or political development.
 */
public final class KnownWorldCalendarBridge
        implements ModInitializer {

    private static final long TICKS_PER_DAY = 24000L;

    private static final int MAX_DAY_STEPS_PER_TICK = 4;

    private static final String HEADER =
            "KNOWNWORLD_OVERWORLD_CALENDAR_BRIDGE\t1";

    private static MinecraftServer activeServer;
    private static Path saveFile;

    private static boolean active;
    private static long lastObservedClock;
    private static long pendingDays;
    private static long processedDays;

    private record StoredState(
            long lastObservedClock,
            long pendingDays
    ) { }

    @Override
    public void onInitialize() {

        ServerLifecycleEvents.SERVER_STARTED.register(
                KnownWorldCalendarBridge::start
        );

        ServerTickEvents.END_SERVER_TICK.register(
                KnownWorldCalendarBridge::tick
        );

        ServerLifecycleEvents.BEFORE_SAVE.register(
                (server, flush, force) -> {

                    if (activeServer == server && active) {
                        save();
                    }
                }
        );

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {

            if (activeServer == server && active) {
                save();
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {

            if (activeServer == server) {
                reset();
            }
        });

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        dispatcher.register(
                                Commands.literal("kwcalendar")
                                        .then(
                                                Commands.literal("status")
                                                        .executes(context ->
                                                                showStatus(
                                                                        context.getSource()
                                                                )
                                                        )
                                        )
                        )
        );

        KnownWorld.LOGGER.info(
                "Known World Minecraft-day calendar bridge registered."
        );
    }

    private static void start(MinecraftServer server) {

        reset();
        activeServer = server;

        ServerLevel overworld =
                server.getLevel(Level.OVERWORLD);

        if (overworld == null
                || !(overworld.getChunkSource().getGenerator()
                instanceof KnownWorldChunkGenerator)) {

            KnownWorld.LOGGER.info(
                    "Campaign overworld-clock integration disabled "
                            + "because this save does not use "
                            + "the Known World generator."
            );

            return;
        }

        NpcSimulation simulation =
                NpcSimulation.getNullable();

        if (simulation == null
                || simulation.server() != server) {

            KnownWorld.LOGGER.warn(
                    "Could not activate Known World clock: "
                            + "NPC simulation is unavailable."
            );

            return;
        }

        saveFile = server.getWorldPath(LevelResource.ROOT)
                .resolve("knownworld")
                .resolve("time")
                .resolve("overworld_calendar_bridge_v1.tsv");

        long now = overworld.getOverworldClockTime();

        StoredState state;

        try {

            state = load(now);

        } catch (IOException error) {

            /*
             * A corrupted bridge file is a serious issue:
             * resetting it silently could cause missed or
             * duplicated historical-day processing.
             */
            throw new IllegalStateException(
                    "Could not load Known World calendar bridge. "
                            + "Restore the save from backup or "
                            + "repair its time checkpoint.",
                    error
            );
        }

        lastObservedClock = state.lastObservedClock();
        pendingDays = state.pendingDays();

        if (lastObservedClock > now) {

            /*
             * A world may have been restored from backup or
             * an administrator may have moved time backward.
             * Do not subtract days from campaign history.
             */
            KnownWorld.LOGGER.warn(
                    "Minecraft overworld clock moved backward "
                            + "since its last bridge checkpoint. "
                            + "Re-anchoring without reversing campaign history."
            );

            lastObservedClock = now;
        }

        simulation.campaignCalendar()
                .enableWorldClockAuthority(now);

        active = true;

        KnownWorld.LOGGER.info(
                "Known World B1 active: campaign={} AC day {}, "
                        + "Minecraft time={}, pending days={}, "
                        + "duration=24000 overworld ticks/day.",
                simulation.campaignCalendar().year(),
                simulation.campaignCalendar().dayOfYear() + 1,
                now,
                pendingDays
        );
    }

    private static void tick(MinecraftServer server) {

        if (!active || activeServer != server) {
            return;
        }

        ServerLevel overworld =
                server.getLevel(Level.OVERWORLD);

        NpcSimulation simulation =
                NpcSimulation.getNullable();

        if (overworld == null
                || simulation == null
                || simulation.server() != server) {
            return;
        }

        CampaignCalendar calendar =
                simulation.campaignCalendar();

        long now = overworld.getOverworldClockTime();

        if (now >= lastObservedClock) {

            long previousDay = Math.floorDiv(
                    lastObservedClock,
                    TICKS_PER_DAY
            );

            long currentDay = Math.floorDiv(
                    now,
                    TICKS_PER_DAY
            );

            long daysCrossed = currentDay - previousDay;

            if (daysCrossed > 0) {

                pendingDays = Math.addExact(
                        pendingDays,
                        daysCrossed
                );
            }

        } else {

            /*
             * Time moved backward. Re-anchor here.
             * Do not reverse ages, births or deaths.
             */
            KnownWorld.LOGGER.warn(
                    "Overworld clock moved backward. "
                            + "Campaign date remains unchanged."
            );
        }

        lastObservedClock = now;

        /*
         * At most four days per server tick to avoid one
         * enormous stall if /time add skips years.
         * Pending work remains in the checkpoint file.
         */
        int steps = (int) Math.min(
                pendingDays,
                MAX_DAY_STEPS_PER_TICK
        );

        for (int i = 0; i < steps; i++) {

            calendar.advanceOneDay();

            /*
             * Reuse the EXISTING lifecycle integration.
             * This handles age, pregnancy and mortality
             * through LifeCycleService.
             *
             * Do not call an LLM.
             */
            simulation.lifeCycle()
                    .onNewCampaignDay(
                            simulation.serverTickCounter()
                    );

            pendingDays--;
            processedDays++;
        }

        calendar.syncWorldClockPhase(now);
    }

    private static StoredState load(long initialTime)
            throws IOException {

        if (!Files.exists(saveFile)) {

            /*
             * First B1 activation for this save.
             *
             * Preserve the historical campaign date
             * already loaded by CampaignCalendarPersistence.
             * Do not retroactively replay all vanilla days
             * since the world's creation.
             */
            return new StoredState(initialTime, 0L);
        }

        var lines = Files.readAllLines(
                saveFile,
                StandardCharsets.UTF_8
        );

        if (lines.size() != 2
                || !HEADER.equals(lines.getFirst())) {

            throw new IOException(
                    "Unsupported world clock checkpoint: "
                            + saveFile
            );
        }

        String[] fields = lines.get(1)
                .split("\t", -1);

        if (fields.length != 3
                || !"CLOCK".equals(fields[0])) {

            throw new IOException(
                    "Invalid world clock checkpoint record"
            );
        }

        try {

            long observed = Long.parseLong(fields[1]);
            long pending = Long.parseLong(fields[2]);

            if (pending < 0) {

                throw new IOException(
                        "Negative pending campaign days"
                );
            }

            return new StoredState(observed, pending);

        } catch (NumberFormatException error) {

            throw new IOException(
                    "Invalid world clock checkpoint numbers",
                    error
            );
        }
    }

    private static void save() {

        if (!active || saveFile == null) {
            return;
        }

        try {

            Files.createDirectories(saveFile.getParent());

            String content = HEADER
                    + "\nCLOCK\t"
                    + lastObservedClock
                    + "\t"
                    + pendingDays
                    + "\n";

            Path temp = Files.createTempFile(
                    saveFile.getParent(),
                    "calendar-",
                    ".tmp"
            );

            try {

                Files.writeString(
                        temp,
                        content,
                        StandardCharsets.UTF_8
                );

                try {

                    Files.move(
                            temp,
                            saveFile,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE
                    );

                } catch (AtomicMoveNotSupportedException ignored) {

                    Files.move(
                            temp,
                            saveFile,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }

            } finally {

                Files.deleteIfExists(temp);
            }

        } catch (IOException error) {

            throw new IllegalStateException(
                    "Failed to save Known World time checkpoint",
                    error
            );
        }
    }

    private static int showStatus(
            net.minecraft.commands.CommandSourceStack source
    ) {

        NpcSimulation simulation =
                NpcSimulation.getNullable();

        if (!active || simulation == null) {

            source.sendSuccess(
                    () -> Component.literal(
                            "[Known World / Calendar] "
                                    + "Overworld sync inactive."
                    ),
                    false
            );

            return 0;
        }

        CampaignCalendar calendar =
                simulation.campaignCalendar();

        source.sendSuccess(
                () -> Component.literal(
                        "[Known World / Calendar] "
                                + "active=true"
                                + " | year=" + calendar.year() + " AC"
                                + " | day=" + (calendar.dayOfYear() + 1)
                                + "/365"
                                + " | MC phase="
                                + calendar.tickRemainder() + "/24000"
                                + " | processed this session="
                                + processedDays
                                + " | catch-up pending="
                                + pendingDays
                ),
                false
        );

        return 1;
    }

    private static void reset() {

        activeServer = null;
        saveFile = null;
        active = false;
        lastObservedClock = 0;
        pendingDays = 0;
        processedDays = 0;
    }
}
