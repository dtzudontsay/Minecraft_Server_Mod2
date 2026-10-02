package dev.dtzudontsay.knownworld.simulation.social.relationship;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.persistence.DynastyRelationshipPersistence;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.Dynasty;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class GlobalRelationshipRuntime {

    private static GlobalRelationshipRuntime instance;

    private final MinecraftServer server;

    private final NpcSimulation simulation;

    private final SocietyStructureRuntime society;

    private final DynastyRelationshipManager dynastyRelationships;

    private final DynastyRelationshipPersistence persistence;

    private final EffectiveRelationshipResolver resolver;

    private RelationshipReviewAudit.Report lastReviewReport;

    private int authoredCharacterRelationshipCount;

    private int appliedCharacterRelationshipCount;

    private int deferredCharacterRelationshipCount;

    private int authoredDynastyRelationshipCount;

    private int revisedDynastyRelationshipCount;

    private GlobalRelationshipRuntime(
            MinecraftServer server,
            NpcSimulation simulation,
            SocietyStructureRuntime society
    ) {

        this.server =
                Objects.requireNonNull(
                        server,
                        "server"
                );

        this.simulation =
                Objects.requireNonNull(
                        simulation,
                        "simulation"
                );

        this.society =
                Objects.requireNonNull(
                        society,
                        "society"
                );

        this.dynastyRelationships =
                new DynastyRelationshipManager(
                        society.dynasties()
                );

        Path saveDirectory =
                server.getWorldPath(
                                LevelResource.ROOT
                        )
                        .resolve(
                                "knownworld"
                        )
                        .resolve(
                                "society"
                        );

        this.persistence =
                new DynastyRelationshipPersistence(
                        saveDirectory
                );

        this.resolver =
                new EffectiveRelationshipResolver(
                        simulation,
                        society,
                        dynastyRelationships
                );
    }

    public static void registerLifecycle() {

        ServerLifecycleEvents.SERVER_STARTED.register(
                server -> {

                    NpcSimulation simulation =
                            NpcSimulation.getNullable();

                    SocietyStructureRuntime society =
                            SocietyStructureRuntime.getNullable();

                    if (simulation == null) {

                        throw new IllegalStateException(
                                "Cannot initialize global relationships before NPC simulation"
                        );
                    }

                    if (society == null) {

                        throw new IllegalStateException(
                                "Cannot initialize global relationships before society structure runtime"
                        );
                    }

                    GlobalRelationshipRuntime runtime =
                            new GlobalRelationshipRuntime(
                                    server,
                                    simulation,
                                    society
                            );

                    instance =
                            runtime;

                    runtime.loadAndApply();

                    KnownWorld.LOGGER.info(
                            "Global relationship runtime started: characterAuthored={}, characterApplied={}, characterDeferred={}, dynastyAuthored={}, dynastyRuntime={}, dynastyRevised={}.",
                            runtime.authoredCharacterRelationshipCount,
                            runtime.appliedCharacterRelationshipCount,
                            runtime.deferredCharacterRelationshipCount,
                            runtime.authoredDynastyRelationshipCount,
                            runtime.dynastyRelationships.size(),
                            runtime.revisedDynastyRelationshipCount
                    );
                }
        );

        ServerLifecycleEvents.BEFORE_SAVE.register(
                (
                        server,
                        flush,
                        force
                ) -> {

                    GlobalRelationshipRuntime runtime =
                            instance;

                    if (runtime != null
                            && runtime.server
                            == server) {

                        runtime.saveQuietly();
                    }
                }
        );

        ServerLifecycleEvents.SERVER_STOPPING.register(
                server -> {

                    GlobalRelationshipRuntime runtime =
                            instance;

                    if (runtime != null
                            && runtime.server
                            == server) {

                        runtime.saveQuietly();
                    }
                }
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> {

                    if (instance != null
                            && instance.server
                            == server) {

                        instance =
                                null;
                    }
                }
        );
    }

    public static GlobalRelationshipRuntime get() {

        if (instance == null) {

            throw new IllegalStateException(
                    "Global relationship runtime is not running"
            );
        }

        return instance;
    }

    public static GlobalRelationshipRuntime getNullable() {
        return instance;
    }

    public DynastyRelationshipManager dynastyRelationships() {
        return dynastyRelationships;
    }

    public EffectiveRelationshipResolver resolver() {
        return resolver;
    }

    public int authoredCharacterRelationshipCount() {
        return authoredCharacterRelationshipCount;
    }

    public int appliedCharacterRelationshipCount() {
        return appliedCharacterRelationshipCount;
    }

    public int deferredCharacterRelationshipCount() {
        return deferredCharacterRelationshipCount;
    }

    public int authoredDynastyRelationshipCount() {
        return authoredDynastyRelationshipCount;
    }

    private void loadAndApply() {

        try {

            persistence.loadInto(
                    dynastyRelationships
            );

            GlobalRelationshipCatalog.Definition definition =
                    GlobalRelationshipCatalog.loadDefault();

            authoredCharacterRelationshipCount =
                    definition.characterRelationships()
                            .size();

            authoredDynastyRelationshipCount =
                    definition.dynastyRelationships()
                            .size();

            applyDynastyRelationships(
                    definition
            );

            applyCharacterRelationships(
                    definition
            );

            RelationshipReviewCatalog.Definition reviewRoster =
                    RelationshipReviewCatalog.loadDefault();

            lastReviewReport =
                    new RelationshipReviewAudit(
                            simulation,
                            society,
                            definition,
                            reviewRoster
                    )
                            .audit();

            KnownWorld.LOGGER.info(
                    "Relationship review audit: total={}, direct={}, fallbackOnly={}, noSupported={}, continuityUncertain={}, specialPending={}, unreviewed={}.",
                    lastReviewReport.total(),
                    lastReviewReport.count(
                            RelationshipReviewAudit.Status.HAS_DIRECT_RELATION
                    ),
                    lastReviewReport.count(
                            RelationshipReviewAudit.Status.FALLBACK_ONLY
                    ),
                    lastReviewReport.count(
                            RelationshipReviewAudit.Status.NO_SUPPORTED_RELATION_FOUND
                    ),
                    lastReviewReport.count(
                            RelationshipReviewAudit.Status.CONTINUITY_UNCERTAIN
                    ),
                    lastReviewReport.count(
                            RelationshipReviewAudit.Status.SPECIAL_ENTITY_PENDING
                    ),
                    lastReviewReport.count(
                            RelationshipReviewAudit.Status.UNREVIEWED
                    )
            );

            simulation.save();

            persistence.save(
                    dynastyRelationships
            );

        } catch (
                IOException exception
        ) {

            throw new IllegalStateException(
                    "Failed to initialize global relationship runtime",
                    exception
            );
        }
    }

    private void applyDynastyRelationships(
            GlobalRelationshipCatalog.Definition definition
    ) throws IOException {

        Set<String> seen =
                new LinkedHashSet<>();

        for (
                GlobalRelationshipCatalog.DynastyRelationshipDefinition authored :
                definition.dynastyRelationships()
        ) {

            String key =
                    authored.subjectDynasty()
                            + "->"
                            + authored.targetDynasty();

            if (!seen.add(
                    key
            )) {

                throw new IOException(
                        "Duplicate authored dynasty relationship "
                                + key
                );
            }

            Dynasty subject =
                    society.dynasties()
                            .findAuthored(
                                    authored.subjectDynasty()
                            )
                            .orElseThrow(
                                    () ->
                                            new IOException(
                                                    "Unknown authored dynasty "
                                                            + authored.subjectDynasty()
                                            )
                            );

            Dynasty target =
                    society.dynasties()
                            .findAuthored(
                                    authored.targetDynasty()
                            )
                            .orElseThrow(
                                    () ->
                                            new IOException(
                                                    "Unknown authored dynasty "
                                                            + authored.targetDynasty()
                                            )
                            );

            if (dynastyRelationships.reconcileAuthored(
                    subject.id(),
                    target.id(),
                    authored.authoringVersion(),
                    authored.affinity(),
                    authored.trust(),
                    authored.respect(),
                    authored.fear(),
                    authored.familiarity()
            )) {

                revisedDynastyRelationshipCount++;
            }
        }
    }

    private void applyCharacterRelationships(
            GlobalRelationshipCatalog.Definition definition
    ) throws IOException {

        Set<String> seen =
                new LinkedHashSet<>();

        for (
                GlobalRelationshipCatalog.CharacterRelationshipDefinition authored :
                definition.characterRelationships()
        ) {

            String key =
                    authored.subject()
                            + "->"
                            + authored.target();

            if (!seen.add(
                    key
            )) {

                throw new IOException(
                        "Duplicate authored global character relationship "
                                + key
                );
            }

            NpcId subject =
                    simulation.authoredIds()
                            .findNpc(
                                    authored.subject()
                            )
                            .orElse(
                                    null
                            );

            NpcId target =
                    simulation.authoredIds()
                            .findNpc(
                                    authored.target()
                            )
                            .orElse(
                                    null
                            );

            if (subject == null
                    || target == null) {

                deferredCharacterRelationshipCount++;

                continue;
            }

            CharacterProfile profile =
                    simulation.profiles()
                            .getOrCreate(
                                    subject
                            );

            String marker =
                    "authoring.global_relationship."
                            + authored.target()
                            + ".v"
                            + authored.authoringVersion();

            if (profile.values()
                    .containsKey(
                            marker
                    )) {

                continue;
            }

            simulation.relationships()
                    .registerLoaded(
                            new NpcRelationship(
                                    subject,
                                    target,
                                    authored.affection(),
                                    authored.trust(),
                                    authored.respect(),
                                    authored.fear(),
                                    authored.familiarity()
                            )
                    );

            profile.setValue(
                    marker,
                    1.0
            );

            appliedCharacterRelationshipCount++;
        }
    }

    public EffectiveRelationshipResolver.EffectiveRelationship resolve(
            String subjectAuthoredId,
            String targetAuthoredId
    ) {

        NpcId subject =
                simulation.authoredIds()
                        .requireNpc(
                                subjectAuthoredId
                        );

        NpcId target =
                simulation.authoredIds()
                        .requireNpc(
                                targetAuthoredId
                        );

        return resolver.resolve(
                subject,
                target
        );
    }

    /**
     * Resolves BOTH explicit and inherited/fallback dynasty relationships.
     *
     * This is intentionally different from requireDynastyRelationship(),
     * which remains available for inspecting only persisted explicit edges.
     */
    public EffectiveDynastyRelationshipResolver.EffectiveDynastyRelationship resolveDynastyRelationship(
            String subjectAuthoredId,
            String targetAuthoredId
    ) {

        DynastyId subject =
                requireDynastyId(
                        subjectAuthoredId
                );

        DynastyId target =
                requireDynastyId(
                        targetAuthoredId
                );

        return resolver.resolveDynasty(
                subject,
                target
        );
    }

    public DynastyRelationship requireDynastyRelationship(
            String subjectAuthoredId,
            String targetAuthoredId
    ) {

        DynastyId subject =
                requireDynastyId(
                        subjectAuthoredId
                );

        DynastyId target =
                requireDynastyId(
                        targetAuthoredId
                );

        return dynastyRelationships.find(
                        subject,
                        target
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "No explicit dynasty relationship "
                                                + subjectAuthoredId
                                                + " -> "
                                                + targetAuthoredId
                                )
                );
    }

    private DynastyId requireDynastyId(
            String authoredId
    ) {

        return society.dynasties()
                .findAuthored(
                        authoredId
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown dynasty "
                                                + authoredId
                                )
                )
                .id();
    }

    public RelationshipReviewAudit.Report relationshipReviewReport() {

        if (lastReviewReport == null) {

            throw new IllegalStateException(
                    "Relationship review audit has not run"
            );
        }

        return lastReviewReport;
    }

    private void saveQuietly() {

        try {

            persistence.save(
                    dynastyRelationships
            );

        } catch (
                IOException exception
        ) {

            KnownWorld.LOGGER.error(
                    "Failed to save dynasty relationships.",
                    exception
            );
        }
    }
}
