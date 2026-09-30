package dev.dtzudontsay.knownworld.simulation.social.dynasty;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.world.reference.ReferenceProvenance;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class DynastyScenarioBootstrapService {

    private static final Gson GSON =
            new Gson();

    private static final String DEFAULT_RESOURCE =
            "data/knownworld/scenarios/agot_298_ac/dynasties.json";

    private DynastyScenarioBootstrapService() {
    }

    public static void ensureDefault(
            NpcSimulation simulation,
            DynastyManager manager
    ) throws IOException {

        DynastyData[] data =
                readJson(
                        DEFAULT_RESOURCE,
                        DynastyData[].class
                );

        /*
         * Pass 1:
         * ensure organizations and dynasty records exist.
         */
        for (
                DynastyData entry :
                data
        ) {

            requireText(
                    entry.id,
                    "dynasty.id"
            );

            requireText(
                    entry.name,
                    entry.id
                            + ".name"
            );

            DynastyType type =
                    enumValue(
                            DynastyType.class,
                            entry.type,
                            entry.id
                                    + ".type"
                    );

            DynastyStatus status =
                    enumValue(
                            DynastyStatus.class,
                            entry.status,
                            entry.id
                                    + ".status"
                    );

            validateReferences(
                    entry
            );

            OrganizationId organizationId =
                    ensureOrganization(
                            simulation,
                            entry.id,
                            entry.name,
                            type
                    );

            manager.ensureAuthored(
                    entry.id,
                    entry.name,
                    type,
                    status,
                    organizationId,
                    entry.homeLocationId,
                    entry.cultureId,
                    entry.religionId,
                    entry.words,
                    entry.heraldry,
                    entry.prestige,
                    entry.wealth,
                    entry.militaryStrength,
                    enumValue(
                            ReferenceProvenance.class,
                            entry.provenance,
                            entry.id
                                    + ".provenance"
                    ),
                    entry.sourceNote
            );
        }

        /*
         * Pass 2:
         * relationships can reference dynasties defined later in the JSON.
         */
        for (
                DynastyData entry :
                data
        ) {

            Dynasty dynasty =
                    manager.findAuthored(
                                    entry.id
                            )
                            .orElseThrow();

            if (hasText(
                    entry.parentDynastyId
            )) {

                Dynasty parent =
                        manager.findAuthored(
                                        entry.parentDynastyId
                                )
                                .orElseThrow(
                                        () ->
                                                new IllegalStateException(
                                                        "Dynasty "
                                                                + entry.id
                                                                + " references unknown parent dynasty "
                                                                + entry.parentDynastyId
                                                )
                                );

                manager.setParent(
                        dynasty.id(),
                        parent.id()
                );
            }

            if (hasText(
                    entry.liegeDynastyId
            )) {

                Dynasty liege =
                        manager.findAuthored(
                                        entry.liegeDynastyId
                                )
                                .orElseThrow(
                                        () ->
                                                new IllegalStateException(
                                                        "Dynasty "
                                                                + entry.id
                                                                + " references unknown liege dynasty "
                                                                + entry.liegeDynastyId
                                                )
                                );

                manager.setLiege(
                        dynasty.id(),
                        liege.id()
                );
            }

            if (hasText(
                    entry.headNpcId
            )) {

                simulation.authoredIds()
                        .findNpc(
                                entry.headNpcId
                        )
                        .ifPresent(
                                npc ->
                                        manager.setHead(
                                                dynasty.id(),
                                                npc
                                        )
                        );
            }

            if (hasText(
                    entry.heirNpcId
            )) {

                simulation.authoredIds()
                        .findNpc(
                                entry.heirNpcId
                        )
                        .ifPresent(
                                npc ->
                                        manager.setHeir(
                                                dynasty.id(),
                                                npc
                                        )
                        );
            }
        }

        KnownWorld.LOGGER.info(
                "Dynasty scenario bootstrap ready with {} dynasties.",
                manager.size()
        );
    }

    private static OrganizationId ensureOrganization(
            NpcSimulation simulation,
            String authoredId,
            String name,
            DynastyType type
    ) {

        var ids =
                simulation.authoredIds();

        OrganizationId existingId =
                ids.findOrganization(
                                authoredId
                        )
                        .orElse(
                                null
                        );

        if (existingId != null) {

            Organization organization =
                    simulation.organizations()
                            .find(
                                    existingId
                            )
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Authored organization mapping "
                                                            + authoredId
                                                            + " points to missing organization "
                                                            + existingId
                                            )
                            );

            if (organization.type()
                    != type.organizationType()) {

                throw new IllegalStateException(
                        "Dynasty "
                                + authoredId
                                + " expects organization type "
                                + type.organizationType()
                                + ", but existing organization is "
                                + organization.type()
                );
            }

            return existingId;
        }

        Organization organization =
                simulation.organizations()
                        .create(
                                name,
                                type.organizationType(),
                                null
                        );

        ids.registerOrganization(
                authoredId,
                organization.id()
        );

        return organization.id();
    }

    private static void validateReferences(
            DynastyData data
    ) {

        WorldReferenceCatalog references =
                WorldReferenceCatalog.get();

        if (hasText(
                data.homeLocationId
        )
                && references.location(
                data.homeLocationId
        ).isEmpty()) {

            throw new IllegalStateException(
                    "Dynasty "
                            + data.id
                            + " references unknown home location "
                            + data.homeLocationId
            );
        }

        if (hasText(
                data.cultureId
        )
                && references.culture(
                data.cultureId
        ).isEmpty()) {

            throw new IllegalStateException(
                    "Dynasty "
                            + data.id
                            + " references unknown culture "
                            + data.cultureId
            );
        }

        if (hasText(
                data.religionId
        )
                && references.religion(
                data.religionId
        ).isEmpty()) {

            throw new IllegalStateException(
                    "Dynasty "
                            + data.id
                            + " references unknown religion "
                            + data.religionId
            );
        }
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type,
            String raw,
            String description
    ) {

        requireText(
                raw,
                description
        );

        try {

            return Enum.valueOf(
                    type,
                    raw.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (
                IllegalArgumentException exception
        ) {

            throw new IllegalArgumentException(
                    "Unknown "
                            + description
                            + ": "
                            + raw,
                    exception
            );
        }
    }

    private static boolean hasText(
            String value
    ) {

        return value != null
                && !value.isBlank();
    }

    private static void requireText(
            String value,
            String description
    ) {

        if (!hasText(
                value
        )) {

            throw new IllegalArgumentException(
                    description
                            + " cannot be empty"
            );
        }
    }

    private static <T> T readJson(
            String resource,
            Class<T> type
    ) throws IOException {

        try (
                InputStream input =
                        DynastyScenarioBootstrapService.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        resource
                                )
        ) {

            if (input == null) {

                throw new IOException(
                        "Dynasty resource not found: "
                                + resource
                );
            }

            try (
                    Reader reader =
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            )
            ) {

                T result =
                        GSON.fromJson(
                                reader,
                                type
                        );

                if (result == null) {

                    throw new IOException(
                            "Dynasty resource produced null: "
                                    + resource
                    );
                }

                return result;
            }

        } catch (
                JsonParseException exception
        ) {

            throw new IOException(
                    "Invalid dynasty JSON: "
                            + resource,
                    exception
            );
        }
    }

    private static final class DynastyData {

        String id;

        String name;

        String type;

        String status;

        String homeLocationId;

        String cultureId;

        String religionId;

        String parentDynastyId;

        String liegeDynastyId;

        String headNpcId;

        String heirNpcId;

        String words;

        String heraldry;

        double prestige;

        double wealth;

        double militaryStrength;

        String provenance;

        String sourceNote;
    }
}