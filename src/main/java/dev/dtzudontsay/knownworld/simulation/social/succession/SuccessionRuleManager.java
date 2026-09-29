package dev.dtzudontsay.knownworld.simulation.social.succession;

import dev.dtzudontsay.knownworld.simulation.social.title.TitleId;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class SuccessionRuleManager {

    private final TitleManager titles;

    private final Map<TitleId, SuccessionLaw> laws =
            new LinkedHashMap<>();

    public SuccessionRuleManager(
            TitleManager titles
    ) {
        this.titles =
                Objects.requireNonNull(
                        titles,
                        "titles"
                );
    }

    public synchronized void setLaw(
            TitleId title,
            SuccessionLaw law
    ) {
        validateTitle(
                title
        );

        laws.put(
                title,
                Objects.requireNonNull(
                        law,
                        "law"
                )
        );
    }

    public synchronized SuccessionLaw lawOf(
            TitleId title
    ) {
        validateTitle(
                title
        );

        return laws.getOrDefault(
                title,
                SuccessionLaw.NONE
        );
    }

    public synchronized Map<TitleId, SuccessionLaw> all() {
        return Map.copyOf(
                laws
        );
    }

    private void validateTitle(
            TitleId title
    ) {
        Objects.requireNonNull(
                title,
                "title"
        );

        if (titles.find(
                title
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown title ID: "
                            + title
            );
        }
    }
}