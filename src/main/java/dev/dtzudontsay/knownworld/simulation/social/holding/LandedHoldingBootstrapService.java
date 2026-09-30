package dev.dtzudontsay.knownworld.simulation.social.holding;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.Dynasty;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyManager;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleId;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class LandedHoldingBootstrapService {

    private static final Gson GSON =
            new Gson();

    private static final String RESOURCE =
            "data/knownworld/scenarios/agot_298_ac/landed_holdings.json";

    private LandedHoldingBootstrapService() {
    }

    public static Report ensureDefault(
            NpcSimulation simulation,
            DynastyManager dynasties,
            LandedHoldingManager holdings
    ) throws IOException {

        RootData root =
                read();

        List<HoldingData> entries =
                root.holdings == null
                        ? List.of()
                        : root.holdings;

        Set<String> authoredIds =
                new LinkedHashSet<>();

        int created =
                0;

        int reconciled =
                0;

        /*
         * PASS 1
         *
         * Create/reconcile every holding without parent links.
         */
        for (
                HoldingData data :
                entries
        ) {

            requireText(
                    data.id,
                    "holding.id"
            );

            String authoredId =
                    normalize(
                            data.id
                    );

            if (!authoredIds.add(
                    authoredId
            )) {

                throw new IllegalStateException(
                        "Duplicate authored holding "
                                + authoredId
                );
            }

            boolean existed =
                    holdings.findAuthored(
                            authoredId
                    ).isPresent();

            DynastyId deJure =
                    optionalDynasty(
                            dynasties,
                            data.deJureDynastyId
                    );

            DynastyId owner =
                    optionalDynasty(
                            dynasties,
                            data.ownerDynastyId
                    );

            NpcId holder =
                    optionalNpc(
                            simulation,
                            data.holderNpcId
                    );

            OrganizationId government =
                    optionalOrganization(
                            simulation,
                            data.governmentOrganizationId
                    );

            TitleId title =
                    optionalTitle(
                            simulation,
                            data.linkedTitleId
                    );

            holdings.reconcileAuthored(
                    authoredId,
                    requireText(
                            data.name,
                            authoredId + ".name"
                    ),
                    enumValue(
                            HoldingType.class,
                            data.type,
                            authoredId + ".type"
                    ),
                    requireText(
                            data.worldLocationId,
                            authoredId + ".worldLocationId"
                    ),
                    deJure,
                    hasText(
                            data.status
                    )
                            ? enumValue(
                            HoldingStatus.class,
                            data.status,
                            authoredId + ".status"
                    )
                            : HoldingStatus.ACTIVE,
                    owner,
                    holder,
                    government,
                    title,
                    data.capital,
                    valueOrZero(
                            data.taxBase
                    ),
                    valueOrZero(
                            data.militaryValue
                    ),
                    valueOrZero(
                            data.populationWeight
                    )
            );

            if (existed) {

                reconciled++;

            } else {

                created++;
            }
        }

        /*
         * PASS 2
         *
         * Resolve territorial hierarchy only after all authored holdings
         * exist.
         */
        for (
                HoldingData data :
                entries
        ) {

            LandedHolding holding =
                    holdings.requireAuthored(
                            data.id
                    );

            HoldingId parent =
                    hasText(
                            data.parentHoldingId
                    )
                            ? holdings.requireAuthored(
                            data.parentHoldingId
                    ).id()
                            : null;

            holdings.setParent(
                    holding.id(),
                    parent
            );
        }

        int removed =
                holdings.removeStaleAuthored(
                        authoredIds
                );

        KnownWorld.LOGGER.info(
                "Landed holding bootstrap complete: authored={}, created={}, reconciled={}, staleRemoved={}, runtimeTotal={}.",
                authoredIds.size(),
                created,
                reconciled,
                removed,
                holdings.size()
        );

        return new Report(
                authoredIds.size(),
                created,
                reconciled,
                removed,
                holdings.size()
        );
    }

    private static DynastyId optionalDynasty(
            DynastyManager dynasties,
            String authoredId
    ) {

        if (!hasText(
                authoredId
        )) {

            return null;
        }

        return dynasties.findAuthored(
                        authoredId
                )
                .map(
                        Dynasty::id
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown authored dynasty "
                                                + authoredId
                                )
                );
    }

    private static NpcId optionalNpc(
            NpcSimulation simulation,
            String authoredId
    ) {

        return hasText(
                authoredId
        )
                ? simulation.authoredIds()
                .requireNpc(
                        authoredId
                )
                : null;
    }

    private static OrganizationId optionalOrganization(
            NpcSimulation simulation,
            String authoredId
    ) {

        return hasText(
                authoredId
        )
                ? simulation.authoredIds()
                .requireOrganization(
                        authoredId
                )
                : null;
    }

    private static TitleId optionalTitle(
            NpcSimulation simulation,
            String authoredId
    ) {

        return hasText(
                authoredId
        )
                ? simulation.authoredIds()
                .requireTitle(
                        authoredId
                )
                : null;
    }

    private static double valueOrZero(
            Double value
    ) {

        return value == null
                ? 0.0
                : value;
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type,
            String value,
            String description
    ) {

        requireText(
                value,
                description
        );

        try {

            return Enum.valueOf(
                    type,
                    value.trim()
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
                            + value,
                    exception
            );
        }
    }

    private static String normalize(
            String value
    ) {

        return requireText(
                value,
                "id"
        )
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private static String requireText(
            String value,
            String description
    ) {

        if (!hasText(
                value
        )) {

            throw new IllegalArgumentException(
                    description
                            + " cannot be blank"
            );
        }

        return value.trim();
    }

    private static boolean hasText(
            String value
    ) {

        return value != null
                && !value.isBlank();
    }

    private static RootData read()
            throws IOException {

        try (
                InputStream input =
                        LandedHoldingBootstrapService.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        RESOURCE
                                )
        ) {

            if (input == null) {

                throw new IOException(
                        "Landed holding resource not found: "
                                + RESOURCE
                );
            }

            try (
                    Reader reader =
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            )
            ) {

                RootData result =
                        GSON.fromJson(
                                reader,
                                RootData.class
                        );

                if (result == null) {

                    throw new IOException(
                            "Landed holding JSON produced null"
                    );
                }

                return result;
            }

        } catch (
                JsonParseException exception
        ) {

            throw new IOException(
                    "Invalid landed holding JSON "
                            + RESOURCE,
                    exception
            );
        }
    }

    public record Report(
            int authoredCount,
            int createdCount,
            int reconciledCount,
            int staleRemovedCount,
            int runtimeTotal
    ) {
    }

    private static final class RootData {

        List<HoldingData> holdings;
    }

    private static final class HoldingData {

        String id;

        String name;

        String type;

        String status;

        String worldLocationId;

        String parentHoldingId;

        String deJureDynastyId;

        String ownerDynastyId;

        String holderNpcId;

        String governmentOrganizationId;

        String linkedTitleId;

        boolean capital;

        Double taxBase;

        Double militaryValue;

        Double populationWeight;
    }
}